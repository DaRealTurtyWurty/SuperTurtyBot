package dev.darealturtywurty.superturtybot.commands.minigames;

import com.codepoetics.ambivalence.Either;
import com.github.topisenpai.lavasrc.spotify.SpotifyAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

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

        run(Either.ofLeft(event), event.getGuild(), event.getChannel().asTextChannel(), channel);
    }

    private static void run(Either<SlashCommandInteractionEvent, MessageReceivedEvent> event, Guild guild, TextChannel channel, AudioChannel voiceChannel) {
        if (event.isLeft()) {
            reply(event.left().orElse(null), "🎵 Guess the song has started!");
        } else {
            reply(event.right().orElse(null), "🎵 Guess the song has started!");
        }

        var messageId = new AtomicLong();

        if (event.isLeft()) {
            event.left().orElse(null).getHook().sendMessage("🎵 Loading...")
                    .queue(message -> messageId.set(message.getIdLong()));
        } else {
            event.right().orElse(null).getChannel().sendMessage("🎵 Loading...")
                    .queue(message -> messageId.set(message.getIdLong()));
        }

        CompletableFuture<Either<AudioTrack, FriendlyException>> future = AudioManager.playGuessTheSong(guild,
                voiceChannel, PLAYLIST);

        future.thenAcceptAsync(result -> {
            if (result.isLeft()) {
                GUESS_THE_SONG_TRACKS.put(guild.getIdLong(),
                        Tuples.of(channel.getIdLong(), result.left().orElse(null), 0));
                if (event.isLeft()) {
                    event.left().orElse(null).getChannel().deleteMessageById(messageId.get()).queue();
                } else {
                    event.right().orElse(null).getChannel().deleteMessageById(messageId.get()).queue();
                }
            } else {
                if (event.isLeft()) {
                    event.left().orElse(null).getHook().editOriginal("❌ Guess the song has failed to start!").queue();
                } else {
                    reply(event.right().orElse(null), "❌ Guess the song has failed to start!");
                }
            }
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
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
                GUESS_THE_SONG_TRACKS.remove(guild.getIdLong());
                AudioManager.endGuessTheSong(guild);
                return;
            }

            if ("skip".equalsIgnoreCase(message)) {
                reply(event, "🎵 The song was `" + tuple.getT2().getInfo().title + "` by `" + tuple.getT2()
                        .getInfo().author + "`");
                GUESS_THE_SONG_TRACKS.remove(guild.getIdLong());
                AudioManager.endGuessTheSong(guild);
                run(Either.ofRight(event), guild, event.getChannel().asTextChannel(),
                        member.getVoiceState().getChannel());
                return;
            }

            AudioTrackInfo info = track.getInfo();
            if (message.equalsIgnoreCase(info.title) || (message.length() >= 4 && info.title.toLowerCase()
                    .startsWith(message.toLowerCase()))) {
                reply(event, "🎵 Correct! The song was `" + info.title + "` by `" + info.author + "`");
                GUESS_THE_SONG_TRACKS.remove(guild.getIdLong());
                AudioManager.endGuessTheSong(guild);
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
