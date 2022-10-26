package dev.darealturtywurty.superturtybot.modules.economy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class WorkCommand extends EconomyCommand {
    private static final List<String> RESPONSES;

    static {
        JsonObject json;
        try {
            json = Constants.GSON.fromJson(
                IOUtils.toString(Objects.requireNonNull(TurtyBot.class.getResourceAsStream("/work_responses.json")), StandardCharsets.UTF_8),
                JsonObject.class);
        } catch (final IOException exception) {
            throw new IllegalStateException("Could not load work responses!", exception);
        }

        RESPONSES = new ArrayList<>();
        json.getAsJsonArray("responses").forEach(element -> RESPONSES.add(element.getAsString()));
    }

    @Override
    public String getDescription() {
        return "Work to earn money!";
    }

    @Override
    public String getName() {
        return "work";
    }

    @Override
    public String getRichName() {
        return "Work";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final User user = event.getUser();
        final Guild guild = event.getGuild();
        final Economy account = EconomyManager.fetchAccount(guild, user);
        if(account.getBank() <= 0) {
            reply(event, "❌ You must have money in your bank to be able to work!", false, true);
            return;
        }

        if (account.getNextWork() > System.currentTimeMillis()) {
            reply(event, "❌ You must wait " + (account.getNextWork() - System.currentTimeMillis()) / 1000 + "s to "
                + "work again!", false, true);
            return;
        }

        final int amount = ThreadLocalRandom.current().nextInt(1000);
        account.setWallet(EconomyManager.addMoney(account, amount));
        account.setNextWork(System.currentTimeMillis() + 3600000L);
        EconomyManager.updateAccount(account);
        reply(event, getResponse(user, amount));
    }

    private static String getResponse(User user, int amount) {
        return RESPONSES.get(ThreadLocalRandom.current().nextInt(RESPONSES.size())).replace("<>", "$")
            .replace("{user}", user.getAsMention()).replace("{amount}", String.valueOf(amount));
    }
}
