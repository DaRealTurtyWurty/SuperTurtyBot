package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.util.Pair;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class BanCommand extends CoreCommand {
    public BanCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to ban!", true),
            new OptionData(OptionType.INTEGER, "delete_days", "Number of days to delete this user's messages", false)
                .setRequiredRange(0, 7),
            new OptionData(OptionType.STRING, "reason", "The ban reason", false));
    }

    @Override
    public String getAccess() {
        return "Moderators (Ban Permission)";
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Bans a user";
    }

    @Override
    public String getHowToUse() {
        return "/ban [user]\n/ban [user] [deleteDays]\n/ban [user] [reason]\n/ban [user] [deleteDays] [reason]\n/ban [user] [reason] [deleteDays]";
    }

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getRichName() {
        return "Ban User";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }
        
        final User user = event.getOption("user").getAsUser();
        if (event.getInteraction().getMember().hasPermission(event.getGuildChannel(), Permission.BAN_MEMBERS)) {
            boolean canInteract = true;
            if (event.getOption("user").getAsMember() != null
                && !event.getInteraction().getMember().canInteract(event.getOption("user").getAsMember())) {
                canInteract = false;
            }
            
            if (canInteract) {
                final int deleteDays = event.getOption("delete_days", 0, OptionMapping::getAsInt);
                String reason = event.getOption("reason", "Unspecified", OptionMapping::getAsString);
                if (reason.length() > 512) {
                    reason = reason.substring(0, 512);
                    // TODO: Confirmation of whether they still want to ban
                }
                
                final String finalReason = reason;
                user.openPrivateChannel().queue(channel -> channel.sendMessage(
                    "You have been banned from `" + event.getGuild().getName() + "` for reason: `" + finalReason + "`!")
                    .queue(success -> {
                    }, error -> {
                    }));
                
                event.getGuild().ban(user, deleteDays, TimeUnit.DAYS).reason(finalReason).queue(success -> {
                    event.deferReply().setContent("Successfully banned " + user.getAsMention() + "!")
                        .mentionRepliedUser(false).queue();
                    final Pair<Boolean, TextChannel> logging = canLog(event.getGuild());
                    if (Boolean.TRUE.equals(logging.getKey())) {
                        log(logging.getValue(), event.getMember().getAsMention() + " has banned " + user.getAsMention()
                            + " for reason: `" + finalReason + "`!", false);
                    }
                }, error -> {
                    if (error instanceof InsufficientPermissionException || error instanceof HierarchyException) {
                        event.deferReply(true).setContent("I do not have permission to ban " + user.getAsMention())
                            .mentionRepliedUser(false).queue();
                    } else {
                        final var embed = new EmbedBuilder();
                        embed.setTitle("Please report this to TurtyWurty#5690!", "https://discord.gg/d5cGhKQ");
                        embed.setDescription("**" + error.getMessage() + "**\n" + ExceptionUtils.getStackTrace(error));
                        embed.setTimestamp(Instant.now());
                        embed.setColor(Color.red);
                        event.deferReply(true).addEmbeds(embed.build()).mentionRepliedUser(true).queue();
                    }
                });
                return;
            }
        }

        event.deferReply(true).setContent("You do not have permission to ban " + user.getAsMention())
            .mentionRepliedUser(false).queue();
    }
    
    public static Pair<Boolean, TextChannel> canLog(Guild guild) {
        final Bson filter = Filters.eq("guild", guild.getIdLong());
        GuildConfig config = Database.getDatabase().guildConfig.find(filter).first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        final long modLogging = config.getModLogging();
        final TextChannel channel = guild.getTextChannelById(modLogging);
        return Pair.create(channel != null, channel);
    }

    public static void log(TextChannel channel, String message, boolean positive) {
        final var embed = new EmbedBuilder();
        embed.setColor(positive ? Color.GREEN : Color.RED);
        embed.setTimestamp(Instant.now());
        embed.setDescription(positive ? "✅ " : "❌ ");
        embed.appendDescription(message);
        
        channel.sendMessageEmbeds(embed.build()).queue();
    }
    
    public static void logSlowmode(TextChannel channel, String message, int time) {
        final var embed = new EmbedBuilder();
        embed.setColor(time <= 0 ? Color.GREEN : Color.RED);
        embed.setTimestamp(Instant.now());
        embed.setDescription(time <= 0 ? "✅ " : "⏳ ");
        embed.appendDescription(message);
        
        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
