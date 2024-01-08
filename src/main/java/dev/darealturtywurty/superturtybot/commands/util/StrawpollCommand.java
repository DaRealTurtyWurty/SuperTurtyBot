package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.TimeUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StrawpollCommand extends CoreCommand {
    protected static final String STRAWPOLL_URL = "https://strawpoll.com/api/poll";

    public StrawpollCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "question", "The question that this strawpoll should ask", true),
                new OptionData(OptionType.STRING, "option1", "The first option", true),
                new OptionData(OptionType.STRING, "option2", "The second option", true),
                new OptionData(OptionType.STRING, "option3", "The third option", false),
                new OptionData(OptionType.STRING, "option4", "The fourth option", false),
                new OptionData(OptionType.STRING, "option5", "The fith option", false),
                new OptionData(OptionType.STRING, "description", "The description of the poll", false),
                new OptionData(OptionType.BOOLEAN, "private", "Whether or not the poll is private", false),
                new OptionData(OptionType.BOOLEAN, "multiple_choice", "Whether or not the poll is multiple choice", false),
                new OptionData(OptionType.BOOLEAN, "name_required", "Whether or not the participants name must be entered",
                        false),
                new OptionData(OptionType.BOOLEAN, "re-captcha", "Whether or not the participant must complete a reCAPTCHA",
                        false),
                new OptionData(OptionType.BOOLEAN, "allow_vpn", "Whether or not the participants are allowed to use a VPN",
                        false),
                new OptionData(OptionType.BOOLEAN, "allow_comments", "Whether or not this poll allows comments", false),
                new OptionData(OptionType.STRING, "duplication_type", "The type of duplication checking that will be used",
                        false).addChoice("ip", "ip").addChoice("browser", "browser"),
                new OptionData(OptionType.STRING, "deadline",
                        "When this poll ends [year-month-day]{-hour-minute-second-millisecond} | [] = required, {} = optional",
                        false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Creates a strawpoll";
    }

    @Override
    public String getHowToUse() {
        return "/strawpoll [question] [option1] [option2] (optional: [option3] [option4] [option5] [description] [isPrivate] [isMultipleChoice] [isNameRequired] [isReCAPTCHArequired] [isVPNallowed] [allowComments] [ip|browser] [deadline])";
    }

    @Override
    public String getName() {
        return "strawpoll";
    }

    @Override
    public String getRichName() {
        return "Create Strawpoll";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String question = event.getOption("question", null, OptionMapping::getAsString);
        final String option1 = event.getOption("option1", null, OptionMapping::getAsString);
        final String option2 = event.getOption("option2", null, OptionMapping::getAsString);
        if (question == null) {
            reply(event, "You must supply a question!", false, true);
            return;
        }

        if (option1 == null || option2 == null) {
            reply(event, "You must supply at least two options!", false, true);
            return;
        }

        final String option3 = event.getOption("option3", null, OptionMapping::getAsString);
        final String option4 = event.getOption("option4", null, OptionMapping::getAsString);
        final String option5 = event.getOption("option5", null, OptionMapping::getAsString);
        final String description = event.getOption("description", "", OptionMapping::getAsString);
        final boolean isPrivate = event.getOption("private", false, OptionMapping::getAsBoolean);
        final boolean multipleChoice = event.getOption("multiple_choice", false, OptionMapping::getAsBoolean);
        final boolean nameRequired = event.getOption("name_required", false, OptionMapping::getAsBoolean);
        final boolean reCAPTCHA = event.getOption("re-captcha", false, OptionMapping::getAsBoolean);
        final boolean allowVPN = event.getOption("allow_vpn", true, OptionMapping::getAsBoolean);
        final boolean allowComments = event.getOption("allow_comments", true, OptionMapping::getAsBoolean);
        final DuplicationType duplicationType = event.getOption("duplication_type", DuplicationType.IP,
                mapping -> DuplicationType.valueOf(mapping.getAsString()));
        final Date deadline = (Date) event.getOption("deadline", null, mapping -> {
            final String dateStr = mapping.getAsString();
            final String[] parts = dateStr.split("-");
            if (parts.length < 3) {
                reply(event, "You must supply a valid date format! [year-month-day]{-hour-minute-second-millisecond} | [] = required, {} = optional", false, true);
                return null;
            }

            try {
                return DateFormat.getInstance().parseObject(TimeUtils.parseDate(parts));
            } catch (final ParseException exception) {
                reply(event, "There was an issue parsing this date. Please report the following to the bot owner:\n"
                        + exception.getMessage() + "\n" + ExceptionUtils.getMessage(exception), false);
                return null;
            }
        });

        if (deadline == null && event.getInteraction().isAcknowledged())
            return;

        event.deferReply()
                .setContent(
                        createPoll(new StrawpollEntry(question, description, isPrivate, multipleChoice, nameRequired, reCAPTCHA,
                                allowVPN, allowComments, duplicationType, deadline, option1, option2, option3, option4, option5)))
                .mentionRepliedUser(false).queue();
    }

    private static String createPoll(StrawpollEntry entry) {
        try {
            final var connection = (HttpsURLConnection) new URI(STRAWPOLL_URL).toURL().openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);
            final var parent = new JsonObject();
            final var poll = new JsonObject();
            poll.addProperty("title", entry.title());
            if (entry.description() != null && !entry.description().isBlank()) {
                poll.addProperty("description", entry.description());
            }

            poll.addProperty("priv", entry.isPrivate());
            poll.addProperty("ma", entry.multiple());
            poll.addProperty("enter_name", entry.enterName());
            poll.addProperty("captcha", entry.reCAPTCHA());
            poll.addProperty("vpn", entry.allowVPN());
            poll.addProperty("co", entry.allowComments());
            poll.addProperty("mip", entry.duplicationCheck() == DuplicationType.BROWSER);
            if (entry.deadline() != null) {
                poll.addProperty("deadline", DateFormat.getInstance().format(entry.deadline()));
            }

            final var options = new JsonArray();
            for (final String option : entry.options()) {
                if (option != null && !option.isBlank()) {
                    options.add(option);
                }
            }

            poll.add("answers", options);
            parent.add("poll", poll);

            final String json = Constants.GSON.toJson(parent);
            connection.getOutputStream().write(json.getBytes());

            final var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            final var response = Constants.GSON.fromJson(String.join("\n", IOUtils.readLines(reader)),
                    JsonObject.class);

            final String url = "https://strawpoll.com/" + response.get("content_id").getAsString();
            Constants.LOGGER.info("FLOAT strawpoll was just created: {}", url);
            return url;
        } catch (IOException | JsonSyntaxException | URISyntaxException exception) {
            Constants.LOGGER.error("Failed to create strawpoll!", exception);
            return "There has been an error connecting to strawpoll, please report the following to the bot owner:\n"
                    + exception.getMessage() + "\n" + ExceptionUtils.getMessage(exception);
        }
    }

    public enum DuplicationType {
        IP, BROWSER
    }

    public record StrawpollEntry(String title, String description, boolean isPrivate, boolean multiple,
                                 boolean enterName, boolean reCAPTCHA, boolean allowVPN, boolean allowComments,
                                 DuplicationType duplicationCheck,
                                 Date deadline, String... options) {
    }
}
