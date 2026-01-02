package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.math.BigInteger;
import java.time.Instant;

public class HeistProfileSubcommand extends HeistSubcommand {
    public HeistProfileSubcommand() {
        super("profile", "View your heist profile");
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        Member member = event.getMember();
        if (member == null) {
            event.getHook().editOriginal("❌ You must be in a server to use this command!").queue();
            return;
        }

        int crimeLevel = Math.max(1, account.getCrimeLevel());
        long totalHeists = account.getTotalHeists();
        long remaining = Math.max(0, crimeLevel - totalHeists);
        String nextHeist = account.getNextHeist() > System.currentTimeMillis()
                ? TimeFormat.RELATIVE.format(account.getNextHeist())
                : "Now";
        int heistLevel = account.getHeistLevel() + 1;
        BigInteger maxPayout = BigInteger.valueOf(500_000L)
                .multiply(BigInteger.valueOf(heistLevel))
                .multiply(BigInteger.valueOf(heistLevel));
        BigInteger setupCost = BigInteger.valueOf(EconomyManager.determineHeistSetupCost(account));

        var embed = new EmbedBuilder();
        embed.setTitle("Heist Profile - " + member.getEffectiveName());
        embed.setColor(member.getColorRaw());
        embed.setTimestamp(Instant.now());
        embed.addField("Heist Level", String.valueOf(account.getHeistLevel()), true);
        embed.addBlankField(true);
        embed.addField("Crime Level", String.valueOf(crimeLevel), true);
        embed.addField("Total Heists", String.valueOf(totalHeists), true);
        embed.addField("Heists Remaining", String.valueOf(remaining), true);
        embed.addField("Next Heist", nextHeist, false);
        embed.addField("Setup Cost", StringUtils.numberFormat(setupCost, config), true);
        embed.addField("Max Payout", StringUtils.numberFormat(maxPayout, config), true);

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
