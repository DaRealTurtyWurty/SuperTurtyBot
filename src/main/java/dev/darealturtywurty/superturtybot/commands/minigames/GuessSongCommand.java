package dev.darealturtywurty.superturtybot.commands.minigames;

import com.github.topisenpai.lavasrc.spotify.SpotifyAudioTrack;
import com.mongodb.client.model.Filters;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.darealturtywurty.superturtybot.commands.levelling.LevellingManager;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GuessSongCommand extends CoreCommand {
    private static final String PLAYLIST = "https://open.spotify.com/playlist/0Dsp6i8lvmcTg5aiusjnFH";
    private static final Map<Long, Tuple3<Long, AudioTrack, Integer>> GUESS_THE_SONG_TRACKS = new HashMap<>();

    public GuessSongCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MINIGAMES;
    }

    @Override
    public String getDescription() {
        return "Guess the title of the song that is currently playing!";
    }

    @Override
    public String getName() {
        return "guesssong";
    }

    @Override
    public String getRichName() {
        return "Guess The Song";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (event.getChannelType() != ChannelType.TEXT) {
            reply(event, "❌ You must be in a text channel to use this command!", false, true);
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || voiceState.getChannel() == null) {
            reply(event, "❌ You must be in a voice channel to use this command!", false, true);
            return;
        }

        final AudioChannel channel = voiceState.getChannel();
        if (!event.getGuild().getAudioManager().isConnected() || event.getGuild().getSelfMember()
                .getVoiceState() == null || event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            reply(event, "❌ I must be connected to a voice channel to use this command!", false, true);
            return;
        }

        if (event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
            reply(event, "❌ You must be in the same voice channel as me to modify the queue!", false, true);
            return;
        }

        if (AudioManager.isGuessSongRunning(event.getGuild())) {
            reply(event, "❌ Guess the song is already running!", false, true);
            return;
        }

        if(AudioManager.getCurrentlyPlaying(event.getGuild()) != null) {
            reply(event, "❌ There is already a song playing!", false, true);
            return;
        }

        run(Either.left(event), event.getGuild(), channel);
    }

    private static void run(Either<SlashCommandInteractionEvent, MessageReceivedEvent> event, Guild guild, AudioChannel voiceChannel) {
        var threadRef = new AtomicReference<ThreadChannel>();
        CompletableFuture<Long> messageId = new CompletableFuture<>();

        event.forEach(slashEvent -> {
            slashEvent.deferReply().setContent("🎵 Guess the song has started!").flatMap(InteractionHook::retrieveOriginal).queue(msg -> {
                msg.createThreadChannel("Guess The Song").queue(thread -> {
                    thread.sendMessage("🎵 Loading...")
                            .queue(message -> messageId.complete(message.getIdLong()));
                    threadRef.set(thread);
                    thread.addThreadMember(slashEvent.getUser()).queue();
                });
            });
        }, messageEvent -> {
            messageEvent.getChannel().sendMessage("🎵 Guess the song has started!").queue();
            messageEvent.getChannel().sendMessage("🎵 Loading...")
                    .queue(message -> messageId.complete(message.getIdLong()));
            threadRef.set(messageEvent.getChannel().asThreadChannel());
        });

        messageId.thenAcceptAsync(id -> {
            CompletableFuture<Either<AudioTrack, FriendlyException>> future = AudioManager.playGuessTheSong(guild,
                    voiceChannel, PLAYLIST);

            future.thenAcceptAsync(result -> {
                result.forEach(track -> {
                    GUESS_THE_SONG_TRACKS.put(guild.getIdLong(),
                            Tuples.of(threadRef.get().getIdLong(), result.isLeft() ? result.getLeft() : null, 0));
                    threadRef.get().deleteMessageById(id).queue();
                }, exception -> {
                    event.forEach(slashEvent -> {
                        slashEvent.getHook().editOriginal("❌ Guess the song has failed to start!").queue();
                    }, messageEvent -> {
                        messageEvent.getChannel().editMessageById(id, "❌ Guess the song has failed to start!").queue();
                    });
                });
            });
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        Guild guild = event.getGuild();

        if (GUESS_THE_SONG_TRACKS.containsKey(guild.getIdLong())) {
            Tuple3<Long, AudioTrack, Integer> tuple = GUESS_THE_SONG_TRACKS.get(guild.getIdLong());
            if (tuple.getT1() != event.getChannel().getIdLong()) return;

            Member member = event.getMember();
            Member selfMember = guild.getSelfMember();

            // check that the user is in the same voice channel as the bot
            if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
                return;
            }

            if (selfMember.getVoiceState() == null || selfMember.getVoiceState().getChannel() == null) {
                return;
            }

            if (member.getVoiceState().getChannel().getIdLong() != selfMember.getVoiceState().getChannel()
                    .getIdLong()) {
                return;
            }

            final AudioTrack track = tuple.getT2();
            if (track == null) {
                reply(event, "❌ The song has ended!");
                AudioManager.endGuessTheSong(guild);
                return;
            }

            String message = event.getMessage().getContentRaw();
            if ("hint".equalsIgnoreCase(message)) {
                int hints = tuple.getT3();
                if (hints >= 3) {
                    reply(event, "❌ You have already used all your hints!");
                    return;
                }

                SpotifyAudioTrack spotifyTrack = (SpotifyAudioTrack) track;

                if (hints == 0 && spotifyTrack.getPosition() >= 5000) {
                    reply(event, "🎵 The song is by `" + tuple.getT2().getInfo().author + "`");
                    GUESS_THE_SONG_TRACKS.put(guild.getIdLong(), Tuples.of(tuple.getT1(), tuple.getT2(), hints + 1));
                } else if (hints == 0 && spotifyTrack.getPosition() < 5000) {
                    reply(event, "❌ You can only use the first hint after 5 seconds!");
                }

                if (hints == 1 && spotifyTrack.getPosition() >= spotifyTrack.getDuration() / 3) {
                    reply(event, "🎵 The song's album cover is: " + spotifyTrack.getArtworkURL());
                    GUESS_THE_SONG_TRACKS.put(guild.getIdLong(), Tuples.of(tuple.getT1(), tuple.getT2(), hints + 1));
                } else if (hints == 1 && spotifyTrack.getPosition() < spotifyTrack.getDuration() / 3) {
                    reply(event, "❌ You can only use the second hint after 1/3 of the song has played!");
                }

                if (hints == 2 && spotifyTrack.getPosition() >= spotifyTrack.getDuration() / 2) {
                    String initials = spotifyTrack.getInfo().title.replaceAll("[^A-Z]", "");
                    // put a . between each letter
                    initials = initials.replaceAll("(.)(?!$)", "$1.");
                    reply(event, "🎵 The song name's initials are: `" + initials + "`");
                    GUESS_THE_SONG_TRACKS.put(guild.getIdLong(), Tuples.of(tuple.getT1(), tuple.getT2(), hints + 1));
                } else if (hints == 2 && spotifyTrack.getPosition() < spotifyTrack.getDuration() / 2) {
                    reply(event, "❌ You can only use the third hint after 1/2 of the song has played!");
                }

                return;
            }

            if ("give up".equalsIgnoreCase(message)) {
                reply(event, "🎵 The song was `" + tuple.getT2().getInfo().title + "` by `" + tuple.getT2()
                        .getInfo().author + "`");
              
                ThreadChannel thread = event.getGuild().getThreadChannelById(tuple.getT1());
                if (thread != null) {
                    thread.getManager().setArchived(true).queue();
                }
              
                GUESS_THE_SONG_TRACKS.remove(guild.getIdLong());
                AudioManager.endGuessTheSong(guild);
                return;
            }

            if ("skip".equalsIgnoreCase(message)) {
                reply(event, "🎵 The song was `" + tuple.getT2().getInfo().title + "` by `" + tuple.getT2()
                        .getInfo().author + "`");
                GUESS_THE_SONG_TRACKS.remove(guild.getIdLong());
                AudioManager.endGuessTheSong(guild);
              
                run(Either.right(event), event.getGuild(), member.getVoiceState().getChannel());
                return;
            }

            AudioTrackInfo info = track.getInfo();
            if (message.equalsIgnoreCase(info.title) || (message.length() >= 4 && info.title.toLowerCase()
                    .startsWith(message.toLowerCase()))) {
                GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
                if(config == null) {
                    config = new GuildConfig(guild.getIdLong());
                    Database.getDatabase().guildConfig.insertOne(config);
                }

                String replyContent = "🎵 Correct! The song was `" + info.title + "` by `" + info.author + "`";
                if(config.isLevellingEnabled()) {
                    User user = event.getAuthor();

                    // get xp relative to the current position in the song
                    int xp = (int) ((track.getDuration() - track.getPosition()) / 1000);
                    xp = MathUtils.clamp(xp, 1, 100);
                    LevellingManager.INSTANCE.addXP(guild, user, xp, new LevellingManager.LevelUpMessage(guild, Optional.of(event.getMessage())));
                    replyContent += ". You gained " + xp + " XP!";
                }

                reply(event, replyContent);
                GUESS_THE_SONG_TRACKS.remove(guild.getIdLong());
                AudioManager.endGuessTheSong(guild);
                run(Either.right(event), event.getGuild(), member.getVoiceState().getChannel());
            }
        }
    }

    @Override
    public String getHowToUse() {
        return "/guesssong\n\n- Type `skip` to skip the current song.\n- Type `hint` to get a hint.\n- Type `give up` to give up on the current song.";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }
}
