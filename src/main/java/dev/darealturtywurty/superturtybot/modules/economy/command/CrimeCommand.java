package dev.darealturtywurty.superturtybot.modules.economy.command;

import java.util.concurrent.ThreadLocalRandom;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class CrimeCommand extends EconomyCommand {
    public CrimeCommand() {
    }
    
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
        
        final Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        if (account.getNextCrime() > System.currentTimeMillis()) {
            reply(event, "❌ You must wait another `%d` seconds until you can rob someone!"
                .formatted((account.getNextCrime() - System.currentTimeMillis()) / 1000));
            return;
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            final int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.addMoney(account, amount);
            account.setNextCrime(System.currentTimeMillis() + 320000L);
            EconomyManager.updateAccount(account);
            reply(event, "You successfully committed a crime and earned <>%d!".replace("<>", "$").formatted(amount));
        } else {
            final int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.removeMoney(account, amount, true);
            account.setNextCrime(System.currentTimeMillis() + 320000L);
            EconomyManager.updateAccount(account);
            reply(event,
                "You were caught committing a crime and were fined <>%d!".replace("<>", "$").formatted(amount));
        }
    }
}
