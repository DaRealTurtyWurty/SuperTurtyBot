package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.commands.music.handler.MusicTrackScheduler;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class VoteSkipCommand extends CoreCommand {
    private static final Map<Long, VoteSkip> VOTE_SKIPS = new HashMap<>();

    public VoteSkipCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Starts a vote-skip for the currently playing track";
    }

    @Override
    public String getName() {
        return "voteskip";
    }

    @Override
    public String getRichName() {
        return "Vote-skip";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (!event.getMember().getVoiceState().inAudioChannel()) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (!event.getGuild().getAudioManager().isConnected()) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }

        if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }

        if (!AudioManager.isPlaying(event.getGuild())) {
            reply(event, "❌ Nothing is playing right now!", false, true);
            return;
        }

        Guild guild = event.getGuild();

        if(VOTE_SKIPS.get(guild.getIdLong()) != null) {
            reply(event, "❌ There is already a vote-skip in progress!", false, true);
            return;
        }

        event.deferReply().queue();

        final AudioTrack track = AudioManager.getCurrentlyPlaying(event.getGuild());
        final long initiatorId = event.getUser().getIdLong();
        AudioChannel audioChannel = event.getMember().getVoiceState().getChannel();
        GuildMessageChannel messageChannel = event.getChannel().asGuildMessageChannel();

        int memberCount = (int) Math.ceil(audioChannel.getMembers().size() * 0.5);
        if(memberCount <= 1) {
            AudioManager.skip(event.getGuild());
            event.getHook().editOriginal("✅ " + event.getUser().getAsMention() + " has skipped the current track!").queue();
            return;
        }

        // send skip embed message
        var embed = new EmbedBuilder();
        embed.setTitle("Vote-skip for `" + track.getInfo().title + "`");
        embed.setDescription("Vote-skip for `" + track.getInfo().title + "` has been initiated by " + event.getUser().getAsMention() + "!");
        embed.addField("Time Remaining", "1 minute", false);
        embed.addField("Votes Needed", "1/" + memberCount, false);
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.RED);
        embed.setFooter("Vote-skip initiated by " + event.getMember().getEffectiveName(), event.getMember().getEffectiveAvatarUrl());

        event.getHook().editOriginalEmbeds(embed.build()).queue(msg -> {
            Button voteButton = Button.primary("vote_skip-%d-%d".formatted(guild.getIdLong(), initiatorId), "Vote");
            msg.editMessageComponents(ActionRow.of(voteButton)).queue();

            AtomicReference<UUID> uuidReference = new AtomicReference<>();

            var voteSkip = new VoteSkip(guild, track, initiatorId, audioChannel, messageChannel, msg.getIdLong(), uuidReference);
            VOTE_SKIPS.put(guild.getIdLong(), voteSkip);
            addWaiter(voteButton, initiatorId);

            MusicTrackScheduler scheduler = AudioManager.getOrCreate(event.getGuild()).getMusicScheduler();
            UUID listenerUUID = scheduler.addTrackEndListener(audioTrack -> {
                if(uuidReference.get() == null || !uuidReference.get().equals(voteSkip.listenerUUID.get()))
                    return;

                voteSkip.onFinish(guild, messageChannel);
            });

            uuidReference.set(listenerUUID);
        });
    }

    private static void addWaiter(final Button voteButton, final long initiatorId) {
        TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .timeout(1, TimeUnit.MINUTES)
                .condition(event -> event.getComponentId().equals(voteButton.getId()) && event.getUser().getIdLong() != initiatorId)
                .success(event -> {
                    Guild guild = event.getGuild();
                    VoteSkip voteSkip = VOTE_SKIPS.get(guild.getIdLong());
                    if(voteSkip == null)
                        return;

                    if(voteSkip.addVote(event.getUser().getIdLong())) {
                        event.deferReply(true).setContent("✅ You have voted to skip the current track!").mentionRepliedUser(false).queue();
                    } else {
                        event.deferReply(true).setContent("❌ You have already voted to skip the current track!").mentionRepliedUser(false).queue();
                    }

                    GuildMessageChannel messageChannel = guild.getChannelById(GuildMessageChannel.class, voteSkip.messageChannelId);
                    if(messageChannel == null)
                        return;

                    if(voteSkip.isVoteSkipped()) {
                        voteSkip.onFinish(guild, messageChannel);
                    } else {
                        addWaiter(voteButton, initiatorId);
                    }
                })
                .failure(() -> {})
                .build();
    }

    public static class VoteSkip {
        private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

        private final long messageChannelId;
        private final long skipMessageId;

        private final AudioTrack track;
        private final int votesNeeded;
        private final long[] voters;

        private final AtomicReference<UUID> listenerUUID;

        public VoteSkip(Guild guild, final AudioTrack track, final long initiatorId, AudioChannel audioChannel,
                        GuildMessageChannel messageChannel, long skipMessageId, AtomicReference<UUID> listenerUUID) {
            this.messageChannelId = messageChannel.getIdLong();
            this.skipMessageId = skipMessageId;

            this.track = track;
            this.votesNeeded = (int) Math.ceil(audioChannel.getMembers().size() * 0.5) - 1;
            this.voters = new long[audioChannel.getMembers().size()];
            this.voters[0] = initiatorId;

            this.listenerUUID = listenerUUID;

            EXECUTOR.schedule(() -> onFinish(guild, messageChannel), 1, TimeUnit.MINUTES);
        }

        public boolean addVote(final long userId) {
            for (int index = 0; index < voters.length; index++) {
                if (voters[index] == 0) {
                    voters[index] = userId;
                    return true;
                }
            }

            return false;
        }

        public boolean isVoteSkipped() {
            return voters.length >= votesNeeded;
        }

        private void onFinish(Guild guild, GuildMessageChannel messageChannel) {
            VOTE_SKIPS.remove(guild.getIdLong());
            messageChannel.deleteMessageById(skipMessageId).queue(ignored -> {
                if (!isVoteSkipped()) {
                    messageChannel.sendMessage("❌ Vote-skip for `" + track.getInfo().title + "` has failed!").queue();
                    return;
                }

                if (Objects.equals(AudioManager.getCurrentlyPlaying(guild), track)) {
                    AudioManager.skip(guild);
                    messageChannel.sendMessage("✅ Vote-skip for `" + track.getInfo().title + "` has passed!").queue();
                } else {
                    messageChannel.sendMessage("❌ Vote-skip for `" + track.getInfo().title + "` has failed!").queue();
                }

                MusicTrackScheduler scheduler = AudioManager.getOrCreate(guild).getMusicScheduler();
                scheduler.removeTrackEndListener(listenerUUID.get());
            }, ignored -> {});
        }
    }
}
