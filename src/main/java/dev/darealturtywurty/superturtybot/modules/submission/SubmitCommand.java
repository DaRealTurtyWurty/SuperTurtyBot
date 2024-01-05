package dev.darealturtywurty.superturtybot.modules.submission;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.Submission;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SubmissionCategory;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SubmitCommand extends CoreCommand {
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{ "http", "https" });

    public SubmitCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(
                        OptionType.STRING,
                        "category",
                        "The submission category to submit to.",
                        true,
                        true),
                new OptionData(
                        OptionType.STRING,
                        "submission",
                        "The content to submit.",
                        true)
        );
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        AutoCompleteQuery query = event.getFocusedOption();
        if(!query.getName().equals("category"))
            return;

        List<SubmissionCategory> categories = Database.getDatabase().submissionCategories.find(
                Filters.eq("guild", event.getGuild() != null ? event.getGuild().getIdLong() : -1L))
                .into(new ArrayList<>())
                .stream()
                .filter(submissionCategory -> submissionCategory.getName()
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        .contains(query.getValue()
                                .trim()
                                .toLowerCase(Locale.ROOT)))
                .limit(25)
                .toList();

        event.replyChoiceStrings(categories.stream().map(SubmissionCategory::getName).toList()).queue(unused -> {}, throwable -> {});
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Submits a submission (of the chosen category) to either the bot or a specific server.";
    }

    @Override
    public String getName() {
        return "submit";
    }

    @Override
    public String getRichName() {
        return "Submit";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String category = event.getOption("category", null, OptionMapping::getAsString);
        if (category == null) {
            reply(event, "❌ You must provide a submission category!", true);
            return;
        }

        String submissionContent = event.getOption("submission", null, OptionMapping::getAsString);
        if (submissionContent == null) {
            reply(event, "❌ You must provide a submission!", true);
            return;
        }

        SubmissionCategory submissionCategory = Database.getDatabase().submissionCategories.find(
                Filters.and(
                        Filters.eq("guild", event.getGuild() != null ? event.getGuild().getIdLong() : -1L),
                        Filters.eq("name", category.toLowerCase(Locale.ROOT))))
                .first();
        if (submissionCategory == null) {
            reply(event, "❌ That submission category does not exist!", false, true);
            return;
        }

        if (submissionCategory.isGuildSpecific() && !event.isFromGuild()) {
            reply(event, "❌ That submission category is only allowed in servers!", false, true);
            return;
        }

        if (submissionCategory.isNSFW() && (event.isFromGuild() && !event.getChannel().asTextChannel().isNSFW())) {
            reply(event, "❌ That submission category is not allowed in non-NSFW channels!", false, true);
            return;
        }

        if(!submissionCategory.isAllowMultipleSubmissions() && submissionCategory.findSubmission(event.getUser().getIdLong()).isPresent()) {
            reply(event, "❌ You have already submitted to that category!", false, true);
            return;
        }

        if(submissionCategory.isMedia() && !URL_VALIDATOR.isValid(submissionContent)) {
            reply(event, "❌ That submission category only allows media submissions!", false, true);
            return;
        } else if(!submissionCategory.isMedia() && URL_VALIDATOR.isValid(submissionContent)) {
            reply(event, "❌ That submission category does not allow media submissions!", false, true);
            return;
        }

        // determine if the url is an image
        if (submissionCategory.isMedia() && submissionContent.endsWith(".png") || submissionContent.endsWith(".jpg") || submissionContent.endsWith(".jpeg")) {
            try(InputStream stream = new URI(submissionContent).toURL().openStream()) {
                byte[] bytes = stream.readAllBytes();
                if (bytes.length > 8_000_000) {
                    reply(event, "❌ That submission is too large!", false, true);
                    return;
                }

                if (bytes.length < 1_000) {
                    reply(event, "❌ That submission is too small!", false, true);
                    return;
                }

                submissionContent = Base64.getEncoder().encodeToString(bytes);
            } catch (IOException | URISyntaxException exception) {
                reply(event, "❌ That submission is invalid!", false, true);
                return;
            }
        }

        var submission = new Submission(
                submissionCategory.isAnonymous() ? -1L : event.getUser().getIdLong(),
                submissionContent,
                System.currentTimeMillis());
        submissionCategory.addSubmission(submission);

        reply(event, "✅ Successfully submitted!", false, true);
    }
}
