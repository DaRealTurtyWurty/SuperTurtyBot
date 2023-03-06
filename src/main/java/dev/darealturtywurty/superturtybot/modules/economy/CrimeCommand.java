package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.concurrent.ThreadLocalRandom;

public class CrimeCommand extends EconomyCommand {
    public CrimeCommand() {}

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
        if(!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!");
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        if(ThreadLocalRandom.current().nextBoolean()) {
            int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.addMoney(account, amount);
            EconomyManager.updateAccount(account);
            reply(event, "You successfully committed a crime and earned <>%d!".replace("<>", "$").formatted(amount));
        } else {
            int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.removeMoney(account, amount, true);
            EconomyManager.updateAccount(account);
            reply(event, "You were caught committing a crime and were fined <>%d!".replace("<>", "$").formatted(amount));
        }
    }
}
