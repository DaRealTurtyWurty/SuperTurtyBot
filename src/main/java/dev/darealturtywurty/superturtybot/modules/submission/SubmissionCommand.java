package dev.darealturtywurty.superturtybot.modules.submission;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SubmissionCategory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SubmissionCommand extends CoreCommand {
    private static final String DESCRIPTION_TOO_LONG = "❌ The description of the submission category must be less than 1024 characters!";

    public SubmissionCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Creates a new submission category for the server.";
    }

    @Override
    public String getName() {
        return "submission";
    }

    @Override
    public String getRichName() {
        return "Submission";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (event.isWebhookMessage() || event.getAuthor().isBot() || !event.isFromGuild())
            return;

        Guild guild = event.getGuild();
        Message message = event.getMessage();
        if (!canManageCategories(event.getMember(), guild))
            return;

        String content = message.getContentRaw();
        if (!content.contains(getName()))
            return;

        String[] args = content.split("\\s+");
        if (args.length < 2) {
            reply(event, getUsageMessage());
            return;
        }

        String subcommand = args[1].trim().toLowerCase(Locale.ROOT);
        if (subcommand.equals("create")) {
            if (args.length < 3) {
                reply(event, "❌ You must provide a name for the submission category!");
                return;
            }

            String name = args[2].trim().toLowerCase(Locale.ROOT);
            if (name.length() > 64) {
                reply(event, "❌ The name of the submission category must be less than 64 characters!");
                return;
            }

            if (!name.matches("^[a-zA-Z0-9_]+$")) {
                reply(event, "❌ The name of the submission category cannot contain mentions!");
                return;
            }

            ParsedCategoryOptions parsedOptions = parseCategoryOptions(args);
            String description = parsedOptions.description();
            if (!isValidDescription(description, event))
                return;

            List<SubmissionCategory> categories = Database.getDatabase().submissionCategories.find(
                    Filters.and(
                            Filters.eq("guild", guild.getIdLong()),
                            Filters.eq("name", name)
                    )).into(new ArrayList<>());
            if (!categories.isEmpty()) {
                reply(event, "❌ A submission category with that name already exists!");
                return;
            }

            var category = new SubmissionCategory(
                    name,
                    description,
                    guild.getIdLong(),
                    parsedOptions.anonymous(),
                    parsedOptions.nsfw(),
                    parsedOptions.media(),
                    parsedOptions.allowMultipleSubmissions()
            );
            Database.getDatabase().submissionCategories.insertOne(category);
            reply(event, "✅ Successfully created submission category!");
        } else if (subcommand.equals("delete")) {
            if (args.length < 3) {
                reply(event, "❌ You must provide a name for the submission category!");
                return;
            }

            String name = args[2].trim().toLowerCase(Locale.ROOT);
            if(name.length() > 64) {
                reply(event, "❌ The name of the submission category must be less than 64 characters!");
                return;
            }

            if (!name.matches("^[a-zA-Z0-9_]+$")) {
                reply(event, "❌ The name of the submission category cannot contain mentions!");
                return;
            }

            Bson filter = Filters.and(
                    Filters.eq("guild", guild.getIdLong()),
                    Filters.eq("name", name)
            );

            List<SubmissionCategory> categories = Database.getDatabase()
                    .submissionCategories
                    .find(filter)
                    .into(new ArrayList<>());
            if (categories.isEmpty()) {
                reply(event, "❌ A submission category with that name does not exist!");
                return;
            }

            Database.getDatabase().submissionCategories.deleteOne(filter);
            reply(event, "✅ Successfully deleted submission category!");
        } else if (subcommand.equals("list")) {
            List<SubmissionCategory> categories = Database.getDatabase()
                    .submissionCategories
                    .find(Filters.eq("guild", guild.getIdLong()))
                    .into(new ArrayList<>());
            if (categories.isEmpty()) {
                reply(event, "❌ There are no submission categories for this server!");
                return;
            }

            var builder = new StringBuilder();
            for (SubmissionCategory category : categories) {
                builder.append("`").append(category.getName()).append("`\n");
            }

            reply(event, "Submission Categories:\n" + builder);
        } else if (subcommand.equals("view")) {
            if (args.length < 3) {
                reply(event, "❌ You must provide a name for the submission category!");
                return;
            }

            String name = args[2].trim().toLowerCase(Locale.ROOT);
            SubmissionCategory category = Database.getDatabase().submissionCategories.find(Filters.and(
                    Filters.eq("guild", guild.getIdLong()),
                    Filters.eq("name", name)
            )).first();
            if (category == null) {
                reply(event, "❌ A submission category with that name does not exist!");
                return;
            }

            String details = """
                    Submission Category: `%s`
                    Description: %s
                    Anonymous: `%s`
                    NSFW: `%s`
                    Media Only: `%s`
                    Allow Multiple Submissions: `%s`
                    Submission Count: `%s`
                    """.formatted(
                    category.getName(),
                    category.getDescription(),
                    category.isAnonymous(),
                    category.isNSFW(),
                    category.isMedia(),
                    category.isAllowMultipleSubmissions(),
                    category.getSubmissions().size());
            reply(event, details);
        } else {
            reply(event, getUsageMessage());
        }
    }

    private static boolean canManageCategories(Member member, Guild guild) {
        if (member == null)
            return false;

        if (member.isOwner() || member.hasPermission(Permission.MANAGE_SERVER))
            return true;

        GuildData config = GuildData.getOrCreateGuildData(guild);
        List<Long> allowedRoleIds = GuildData.getLongs(config.getSubmissionManagerRoles());
        if (allowedRoleIds.isEmpty())
            return false;

        for (Role role : member.getRoles()) {
            if (allowedRoleIds.contains(role.getIdLong()))
                return true;
        }

        return false;
    }

    private static ParsedCategoryOptions parseCategoryOptions(String[] args) {
        boolean anonymous = false;
        boolean nsfw = false;
        boolean media = false;
        boolean allowMultipleSubmissions = true;
        List<String> descriptionTokens = new ArrayList<>();

        for (int index = 3; index < args.length; index++) {
            String token = args[index].trim();
            if (token.matches("\\{[a-zA-Z_]+=(true|false)}")) {
                String content = token.substring(1, token.length() - 1);
                String[] keyValue = content.split("=", 2);
                boolean enabled = Boolean.parseBoolean(keyValue[1]);

                switch (keyValue[0].toLowerCase(Locale.ROOT)) {
                    case "anonymous" -> anonymous = enabled;
                    case "nsfw" -> nsfw = enabled;
                    case "media" -> media = enabled;
                    case "multiple" -> allowMultipleSubmissions = enabled;
                    default -> descriptionTokens.add(token);
                }
            } else {
                descriptionTokens.add(token);
            }
        }

        String description = descriptionTokens.isEmpty() ?
                "No description provided." :
                descriptionTokens.stream().collect(Collectors.joining(" "));
        return new ParsedCategoryOptions(anonymous, nsfw, media, allowMultipleSubmissions, description);
    }

    private static boolean isValidDescription(String description, MessageReceivedEvent event) {
        if (description.length() > 1024) {
            reply(event, DESCRIPTION_TOO_LONG);
            return false;
        }

        if (description.contains("@everyone") || description.contains("@here") || description.contains("<@")) {
            reply(event, "❌ The description of the submission category cannot contain mentions!");
            return false;
        }

        return true;
    }

    private static String getUsageMessage() {
        return """
                Submission usage:
                `.submission create <name> [description] [{anonymous=true}] [{nsfw=true}] [{media=true}] [{multiple=false}]`
                `.submission delete <name>`
                `.submission list`
                `.submission view <name>`
                """;
    }

    private record ParsedCategoryOptions(boolean anonymous, boolean nsfw, boolean media,
                                         boolean allowMultipleSubmissions, String description) {
    }
}
