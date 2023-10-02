package dev.darealturtywurty.superturtybot.commands.economy;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.concurrent.ThreadLocalRandom;

public class CrimeCommand extends EconomyCommand {
    @Override
    public String getDescription() {
        return "Commit a crime to either earn lots money or get a large fine. High risk, high reward.";
    }

    @Override
    public String getName() {
        return "crime";
    }

    @Override
    public String getRichName() {
        return "Crime";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!");
            return;
        }

        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", event.getGuild().getIdLong()))
                .first();
        if (config == null) {
            config = new GuildConfig(event.getGuild().getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        event.deferReply().queue();

        final Economy account = EconomyManager.getAccount(event.getGuild(), event.getUser());
        if (account.getNextCrime() > System.currentTimeMillis()) {
            event.getHook().editOriginal("❌ You must wait %s before committing another crime!"
                    .formatted(TimeFormat.RELATIVE.format(account.getNextCrime()))).queue();
            return;
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            final int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.addMoney(account, amount);
            account.setNextCrime(System.currentTimeMillis() + 320000L);
            EconomyManager.updateAccount(account);
            event.getHook().editOriginal("✅ You successfully committed a crime and earned %s%d!"
                    .formatted(config.getEconomyCurrency(), amount)).queue();
        } else {
            final int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.removeMoney(account, amount, true);
            account.setNextCrime(System.currentTimeMillis() + 320000L);
            EconomyManager.updateAccount(account);
            event.getHook().editOriginal("⚠️ You were caught committing a crime and were fined %s%d!"
                    .formatted(config.getEconomyCurrency(), amount)).queue();
        }
    }
}
