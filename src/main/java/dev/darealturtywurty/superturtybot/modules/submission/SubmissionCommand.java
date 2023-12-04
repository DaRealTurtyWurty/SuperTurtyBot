package dev.darealturtywurty.superturtybot.modules.submission;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.SubmissionCategory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SubmissionCommand extends CoreCommand {
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
        if(event.isWebhookMessage() || event.getAuthor().isBot() || !event.isFromGuild())
            return;

        Guild guild = event.getGuild();
        if(guild.getOwnerIdLong() != Environment.INSTANCE.ownerId().orElse(-1L))
            return;

        Message message = event.getMessage();

        // TODO: Configurable permission
        if(message.getAuthor().getIdLong() != guild.getOwnerIdLong())
            return;

        String content = message.getContentRaw();
        if(!content.contains(getName()))
            return;

        String[] args = content.split("\\s+");
        if(args.length < 2) {
            reply(event, "❌ You must provide a subcommand!");
            return;
        }

        String subcommand = args[1].trim().toLowerCase(Locale.ROOT);
        if(subcommand.startsWith("create")) {
            boolean anonymous = false;
            boolean nsfw = false;
            boolean media = false;
            boolean allowMultipleSubmissions = true;
            if(subcommand.contains("{anonymous=true}")) {
                anonymous = true;
            }

            if(subcommand.contains("{nsfw=true}")) {
                nsfw = true;
            }

            if(subcommand.contains("{media=true}")) {
                media = true;
            }

            if(subcommand.contains("{multiple=false}")) {
                allowMultipleSubmissions = false;
            }

            if(args.length < 3) {
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

            String description = "No description provided.";
            if(args.length > 3) {
                description = content.substring(content.indexOf(args[3]));
                if(description.length() > 1024) {
                    reply(event, "❌ The description of the submission category must be less than 1024 characters!");
                    return;
                }

                if(description.contains("@everyone") || description.contains("@here")) {
                    reply(event, "❌ The description of the submission category cannot contain mentions!");
                    return;
                }

                if(description.contains("<@")) {
                    reply(event, "❌ The description of the submission category cannot contain mentions!");
                    return;
                }
            }

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
                    anonymous,
                    nsfw,
                    media,
                    allowMultipleSubmissions
            );
            Database.getDatabase().submissionCategories.insertOne(category);
            reply(event, "✅ Successfully created submission category!");
        } else if(subcommand.equalsIgnoreCase("delete")) {
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
        } else if(subcommand.equalsIgnoreCase("list")) {
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
        } else {
            reply(event, "❌ Unknown subcommand!");
        }
    }
}
