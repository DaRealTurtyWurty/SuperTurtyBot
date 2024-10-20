package dev.darealturtywurty.superturtybot.commands.music;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.darealturtywurty.superturtybot.commands.music.manager.AudioManager;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    public String getHowToUse() {
        return "/search [term]";
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
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        String rawSearch = event.getOption("search_term", "", OptionMapping::getAsString);
        final String search = rawSearch.trim();

        event.deferReply().queue();

        final CompletableFuture<Either<List<AudioTrack>, FriendlyException>> future = AudioManager
            .search(event.getGuild(), "ytsearch:" + search);
        future.thenAccept(either -> {
            if (either.isLeft()) {
                final List<AudioTrack> results = either.getLeft();
                if (results.isEmpty()) {
                    event.getHook().editOriginal("❌ No results found for `" + search + "`!").queue();
                    return;
                }

                var contents = new PaginatedEmbed.ContentsBuilder();
                for (AudioTrack track : results) {
                    String title = track.getInfo().title.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~");
                    String author = track.getInfo().author.trim();
                    if (author.length() > 20) {
                        author = author.substring(0, 20) + "...";
                    }

                    String uri = track.getInfo().uri.trim();
                    String duration = TimeUtils.millisecondsFormatted(track.getDuration());

                    contents.field(title, "Artist: %s\nLink: %s\nDuration: [%s]".formatted(author, uri, duration));
                }

                PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                        .title("Search results for: " + search)
                        .description("Searched By: " + event.getUser().getAsMention())
                        .color(Color.BLUE)
                        .footer("Searched By: " + event.getUser().getEffectiveName(), event.getMember().getEffectiveAvatarUrl())
                        .thumbnail(event.getGuild().getIconUrl())
                        .timestamp(Instant.now())
                        .build(event.getJDA());

                embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ No results found for `" + search + "`!").queue());
            } else {
                final FriendlyException exception = either.toOptional().orElse(
                    new FriendlyException("Results are missing and error is not present!", Severity.SUSPICIOUS, null));
                event.getHook().editOriginal("❌ There has been an error loading the results: " + exception.getLocalizedMessage()).queue();
            }
        });
    }
}
