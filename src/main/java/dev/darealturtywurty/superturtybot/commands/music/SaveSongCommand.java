package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SaveSongCommand extends CoreCommand {
    public SaveSongCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
                new SubcommandData("add", "Adds the currently playing song to your saved songs list."),
                new SubcommandData("remove", "Removes a song from the saved songs list.").addOptions(
                        new OptionData(OptionType.STRING, "name", "The song name to remove."),
                        new OptionData(OptionType.STRING, "url", "The song url to remove.")
                ),
                new SubcommandData("list", "Lists all saved songs."),
                new SubcommandData("clear", "Clears all saved songs."),
                new SubcommandData("playall", "Plays your saved songs.")
                        .addOption(OptionType.BOOLEAN, "shuffle", "Whether to shuffle the songs."),
                new SubcommandData("play", "Plays a saved song.").addOptions(
                        new OptionData(OptionType.STRING, "name", "The song name to play."),
                        new OptionData(OptionType.STRING, "url", "The song url to play.")
                )
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Saves a song to your saved songs list.";
    }

    @Override
    public String getName() {
        return "savesong";
    }

    @Override
    public String getRichName() {
        return "Save Song";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must provide a subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "add" -> {
                String name = null;
                String url = null;

                Guild guild = event.getGuild();
                Member member = event.getMember();
                if (guild == null || member == null || member.getVoiceState() == null) {
                    reply(event, "❌ You must be in a voice channel!", false, true);
                    return;
                }

                if (!guild.getAudioManager().isConnected()) {
                    reply(event, "❌ I must be in a voice channel!", false, true);
                    return;
                }

                AudioChannel channel = member.getVoiceState().getChannel();
                if (channel == null) {
                    reply(event, "❌ You must be in a voice channel!", false, true);
                    return;
                }

                AudioTrack currentlyPlaying = AudioManager.getCurrentlyPlaying(guild);
                if (currentlyPlaying == null) {
                    reply(event, "❌ There is no song currently playing!", false, true);
                    return;
                }

                name = currentlyPlaying.getInfo().title;
                url = currentlyPlaying.getInfo().uri;

                if (name == null || url == null) {
                    reply(event, "❌ There is no song currently playing!", false, true);
                    return;
                }

                Pair<Boolean, String> result = AudioManager.saveSong(event.getUser(), name, url);
                if (result.getLeft()) {
                    reply(event, "✅ Successfully saved song!");
                } else {
                    reply(event, "❌ " + result.getRight(), false, true);
                }
            }

            case "remove" -> {
                String name = event.getOption("name", null, OptionMapping::getAsString);
                String url = event.getOption("url", null, OptionMapping::getAsString);
                if (name == null && url == null) {
                    reply(event, "❌ You must provide a name and url!", false, true);
                    return;
                }

                Pair<Boolean, String> result = AudioManager.removeSongs(event.getUser(), name, url);
                if (result.getLeft()) {
                    reply(event, "✅ Successfully removed matching songs!");
                } else {
                    reply(event, "❌ " + result.getRight(), false, true);
                }
            }

            case "list" -> {
                event.deferReply().queue();
                Map<String, String> songs = AudioManager.getSavedSongs(event.getUser());
                if (songs.isEmpty()) {
                    event.getHook().sendMessage("❌ No saved songs found!").mentionRepliedUser(false).queue();
                    return;
                }

                var contents = new PaginatedEmbed.ContentsBuilder();
                songs.forEach(contents::field);

                PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                        .title(event.getUser().getName() + "'s Saved Songs")
                        .description("Here are all of your saved songs!")
                        .color(event.getMember() == null ? Color.LIGHT_GRAY : new Color(event.getMember().getColorRaw()))
                        .timestamp(Instant.now())
                        .authorOnly(event.getUser().getIdLong())
                        .build(event.getJDA());

                embed.send(event.getHook(), () -> event.getHook().sendMessage("❌ No saved songs found!").mentionRepliedUser(false).queue());
            }

            case "clear" -> {
                Pair<Boolean, String> result = AudioManager.clearSavedSongs(event.getUser());
                if (result.getLeft()) {
                    reply(event, "✅ Successfully cleared saved songs!");
                } else {
                    reply(event, "❌ " + result.getRight(), false, true);
                }
            }

            case "playall" -> {
                if(event.getGuild() == null) {
                    reply(event, "❌ You must be in a guild to use this command!", false, true);
                    return;
                }

                Map<String, String> songs = AudioManager.getSavedSongs(event.getUser());
                if (songs.isEmpty()) {
                    reply(event, "❌ No saved songs found!", false, true);
                    return;
                }

                if (Objects.requireNonNull(event.getMember()).getVoiceState() == null || !event.getMember().getVoiceState()
                        .inAudioChannel()) {
                    reply(event, "❌ You must be in a voice channel to use this command!", false, true);
                    return;
                }

                final AudioChannel channel = event.getMember().getVoiceState().getChannel();
                if (channel == null) {
                    reply(event, "❌ You must be in a voice channel to use this command!", false, true);
                    return;
                }

                if (!event.getGuild().getAudioManager().isConnected()) {
                    event.getGuild().getAudioManager().openAudioConnection(channel);
                    event.getChannel().sendMessage("✅ I have joined " + channel.getAsMention() + "!").mentionRepliedUser(false)
                            .queue();
                }

                if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
                    reply(event, "❌ You must be in the same voice channel as me to use this command!", false, true);
                    return;
                }

                if(AudioManager.isGuessSongRunning(event.getGuild())) {
                    reply(event, "❌ You cannot play songs while a game of Guess That Song is running!", false, true);
                    return;
                }

                boolean shuffle = event.getOption("shuffle", false, OptionMapping::getAsBoolean);
                AudioManager.playSongs(event.getChannel().asTextChannel(), event.getMember(), songs.values(), shuffle);
                reply(event, "✅ Started playing your saved songs!");
            }

            case "play" -> {
                if(event.getGuild() == null) {
                    reply(event, "❌ You must be in a guild to use this command!", false, true);
                    return;
                }

                Map<String, String> songs = AudioManager.getSavedSongs(event.getUser());
                if (songs.isEmpty()) {
                    reply(event, "❌ No saved songs found!", false, true);
                    return;
                }

                if (Objects.requireNonNull(event.getMember()).getVoiceState() == null || !event.getMember().getVoiceState()
                        .inAudioChannel()) {
                    reply(event, "❌ You must be in a voice channel to use this command!", false, true);
                    return;
                }

                final AudioChannel channel = event.getMember().getVoiceState().getChannel();
                if (channel == null) {
                    reply(event, "❌ You must be in a voice channel to use this command!", false, true);
                    return;
                }

                if (!event.getGuild().getAudioManager().isConnected()) {
                    event.getGuild().getAudioManager().openAudioConnection(channel);
                    event.getChannel().sendMessage("✅ I have joined " + channel.getAsMention() + "!").mentionRepliedUser(false)
                            .queue();
                }

                if (event.getMember().getVoiceState().getChannel().getIdLong() != channel.getIdLong()) {
                    reply(event, "❌ You must be in the same voice channel as me to use this command!", false, true);
                    return;
                }

                if(AudioManager.isGuessSongRunning(event.getGuild())) {
                    reply(event, "❌ You cannot play songs while a game of Guess That Song is running!", false, true);
                    return;
                }

                String name = event.getOption("name", null, OptionMapping::getAsString);
                String url = event.getOption("url", null, OptionMapping::getAsString);

                if(name == null && url == null) {
                    reply(event, "❌ You must provide a name or url!", false, true);
                    return;
                }

                if(name != null) {
                    if(!songs.containsKey(name)) {
                        reply(event, "❌ No saved song found with that name!", false, true);
                        return;
                    }

                    url = songs.get(name);
                } else {
                    if(!songs.containsValue(url)) {
                        reply(event, "❌ No saved song found with that url!", false, true);
                        return;
                    }
                }

                if(url == null) {
                    reply(event, "❌ You must provide a name or url!", false, true);
                    return;
                }

                event.deferReply().queue();

                CompletableFuture<Pair<Boolean, String>> result =
                        AudioManager.play(event.getMember().getVoiceState().getChannel(), event.getChannel().asTextChannel(), url, event.getUser());
                result.thenAccept(pair -> {
                    if(pair.getLeft()) {
                        event.getHook().sendMessage("✅ Successfully played song!").mentionRepliedUser(false).queue();
                    } else {
                        event.getHook().sendMessage("❌ " + pair.getRight()).mentionRepliedUser(false).queue();
                    }
                });
            }
        }
    }
}
