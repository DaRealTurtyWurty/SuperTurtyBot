package dev.darealturtywurty.superturtybot.commands.nsfw;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import dev.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class LoliCommand extends NSFWCommand {
    private static final String[] GIFS = { "https://media.giphy.com/media/jmSjPi6soIoQCFwaXJ/giphy.gif",
        "https://media.giphy.com/media/7zSO56YSB0DhNvBUWt/giphy.gif",
        "https://media.giphy.com/media/3o6wNPIj7WBQcJCReE/giphy.gif",
        "https://media.giphy.com/media/kxevHLcRrMbtC1SXa8/giphy.gif",
        "https://media.giphy.com/media/3dkPQ9JnxxQnVVMdOl/giphy.gif",
        "https://media.giphy.com/media/3osBLaQjYdcuVYpgXu/giphy.gif",
        "https://media.giphy.com/media/3oFzmnMZAOzN5XLh5K/giphy.gif",
        "https://media.giphy.com/media/l1IBiRf6RDmlnhguQ/giphy.gif" };

    public static final String FBI_AUDIO = "src/main/resources/audio/fbi_open_up.mp3";

    public LoliCommand() {
        super(NSFWCategory.FAKE);
    }

    @Override
    public String getDescription() {
        return "Some loli for you 🥵";
    }

    @Override
    public String getName() {
        return "loli";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        // TODO: Check user config to see if they have denied NSFW usage
        // TODO: Check server config to see if the server has disabled this command
        if (event.isFromGuild() && !event.getChannel().asTextChannel().isNSFW())
            return;

        final var embed = new EmbedBuilder();
        embed.setTitle("You are under arrest!");
        embed.setDescription(event.getAuthor().getAsMention() + ", you have been spotted looking for loli!");
        embed.setImage(GIFS[ThreadLocalRandom.current().nextInt(GIFS.length)]);
        embed.setTimestamp(Instant.now());
        event.getMessage().replyEmbeds(embed.build()).mentionRepliedUser(false).queue();

        if (event.isFromGuild() && event.getMember().getVoiceState().inAudioChannel()) {
            AudioManager.play(event.getGuild(), event.getMember().getVoiceState().getChannel(),
                Path.of(FBI_AUDIO).toFile());
        }
    }
}
