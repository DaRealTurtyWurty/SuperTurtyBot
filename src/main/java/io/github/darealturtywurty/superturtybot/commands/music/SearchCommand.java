package io.github.darealturtywurty.superturtybot.commands.music;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.codepoetics.ambivalence.Either;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SearchCommand extends CoreCommand {
    public SearchCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "search_term", "The term to find search results for", true));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Searches for a specific song, showing the results";
    }
    
    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getRichName() {
        return "Search";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }
        
        final String search = event.getOption("search_term").getAsString().trim();
        final CompletableFuture<Either<List<AudioTrack>, FriendlyException>> future = AudioManager
            .search(event.getGuild(), "ytsearch:" + search);
        future.thenAccept(either -> {
            if (either.isLeft()) {
                final List<AudioTrack> results = either.left().orElse(List.of());
                if (results.isEmpty()) {
                    reply(event, "❌ No results have been found!", false, true);
                } else {
                    final List<AudioTrack> truncated = results.stream().limit(5).toList();
                    
                    final var strBuilder = new StringBuilder();
                    int index = 1;
                    for (final AudioTrack result : truncated) {
                        strBuilder.append(index++ + ". **"
                            + result.getInfo().title.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~")
                            + "**\nPosted By: " + result.getInfo().author + "\nLink: " + result.getInfo().uri
                            + "\nDuration: [" + QueueCommand.millisecondsFormatted(result.getDuration()) + "]\n\n");
                    }

                    reply(event,
                        new EmbedBuilder().setColor(Color.BLUE).setTimestamp(Instant.now())
                            .setFooter(
                                "Searched By: " + event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                                event.getUser().getEffectiveAvatarUrl())
                            .setDescription(strBuilder).setTitle("Search results for: " + search));
                }
            } else {
                final FriendlyException exception = either.right().orElse(
                    new FriendlyException("Results are missing and error is not present!", Severity.SUSPICIOUS, null));
                reply(event, "❌ There has been an error loading the results: " + exception.getLocalizedMessage(), false,
                    true);
            }
        });
    }
}
