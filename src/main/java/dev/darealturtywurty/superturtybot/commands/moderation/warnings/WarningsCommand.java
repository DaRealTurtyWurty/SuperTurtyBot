package dev.darealturtywurty.superturtybot.commands.moderation.warnings;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WarningsCommand extends CoreCommand {
    public WarningsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to clear warns from", true));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Gets all warnings for a user";
    }

    @Override
    public String getHowToUse() {
        return "/warnings\n/warnings [user]";
    }

    @Override
    public String getName() {
        return "warnings";
    }

    @Override
    public String getRichName() {
        return "Gather Warnings";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "This command can only be used inside of a server!", false, true);
            return;
        }

        final User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);

        Guild guild = event.getGuild();
        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if(config != null && config.isWarningsModeratorOnly() && !event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            reply(event, "❌ You must be a moderator to use this command!", false, true);
            return;
        }

        final Set<Warning> warns = WarnManager.getWarns(guild, user);
        if (warns.isEmpty()) {
            reply(event, "❌ This user has no warns!", false, true);
            return;
        }

        event.deferReply().queue();

        final List<Warning> sortedWarns = warns.stream().sorted(Comparator.comparing(Warning::getWarnedAt)).toList();

        CompletableFuture<?> completed = new CompletableFuture<>();
        var contents = new PaginatedEmbed.ContentsBuilder();
        for (int index = 0; index < sortedWarns.size(); index++) {
            Warning warning = sortedWarns.get(index);

            int finalIndex = index;
            event.getJDA().retrieveUserById(warning.getWarner()).queue(warner -> {
                contents.field("Warn #" + finalIndex, "Reason: `%s`\nUUID: `%s`\nModerator: %s\nOccurred: %s".formatted(
                        warning.getReason(),
                        warning.getUuid(),
                        warner.getAsMention(),
                        TimeFormat.RELATIVE.format(warning.getWarnedAt()))
                );

                if (finalIndex == sortedWarns.size() - 1) {
                    completed.complete(null);
                }
            }, error -> {
                contents.field("Warn #" + finalIndex, "Reason: `%s`\nUUID: `%s`\nModerator: %s\nOccurred: %s".formatted(
                        warning.getReason(),
                        warning.getUuid(),
                        "Unknown",
                        TimeFormat.RELATIVE.format(warning.getWarnedAt()))
                );

                if (finalIndex == sortedWarns.size() - 1) {
                    completed.complete(null);
                }
            });
        }

        completed.thenRun(() -> {
            PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                    .title(user.getName() + "'s warnings!")
                    .description("This user has " + warns.size() + " warns!")
                    .color(Color.BLUE)
                    .footer("Requested by " + event.getUser().getName(), event.getMember().getEffectiveAvatarUrl())
                    .timestamp(Instant.now())
                    .thumbnail(user.getEffectiveAvatarUrl())
                    .authorOnly(event.getUser().getIdLong())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ This user has no warnings!").queue());
        });
    }
}
