package dev.darealturtywurty.superturtybot.commands.economy.property;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.time.Instant;
import java.util.List;

public class ListPropertiesCommand extends SubcommandCommand {
    public ListPropertiesCommand(CoreCommand parent) {
        super(parent, "list", "List all properties you own.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().setContent("Loading properties...").queue();

        Guild guild = event.getGuild();
        Member member = event.getMember();
        Economy account = EconomyManager.getOrCreateAccount(guild, member.getUser());
        List<Property> properties = account.getProperties();
        if (properties.isEmpty()) {
            event.getHook().editOriginal("❌ You do not own any properties!").queue();
            return;
        }

        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(config);
        }

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (Property property : properties) {
            String builder = "Description: %s%nWorth: %s%s".formatted(
                    property.getDescription(),
                    config.getEconomyCurrency(), StringUtils.numberFormat(property.calculateCurrentWorth()));

            contents.field(property.getName(), builder, false);
        }

        PaginatedEmbed embed = new PaginatedEmbed.Builder(10, contents)
                .authorOnly(member.getIdLong())
                .title(member.getEffectiveName() + "'s Properties")
                .description("You own " + properties.size() + " properties!")
                .color(member.getColorRaw())
                .footer("Requested by " + member.getEffectiveName(), member.getEffectiveAvatarUrl())
                .timestamp(Instant.now())
                .build(event.getJDA());
        embed.send(event.getHook());
    }
}
