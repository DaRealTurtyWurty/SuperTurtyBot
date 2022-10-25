package dev.darealturtywurty.superturtybot.modules.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SexWorkCommand extends EconomyCommand {
    private static final Responses RESPONSES;

    static {
        JsonObject json;
        try {
            json = Constants.GSON.fromJson(IOUtils.toString(
                    Objects.requireNonNull(TurtyBot.class.getResourceAsStream("/sex_work_responses" + ".json")),
                    StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read sex work responses!", exception);
        }

        List<String> success = new ArrayList<>();
        List<String> fail = new ArrayList<>();

        json.getAsJsonArray("success").forEach(element -> success.add(element.getAsString()));
        json.getAsJsonArray("fail").forEach(element -> fail.add(element.getAsString()));
        RESPONSES = new Responses(success, fail);
    }

    public SexWorkCommand() {
    }

    @Override
    public String getDescription() {
        return "Earn money by doing sex-related acts.";
    }

    @Override
    public String getName() {
        return "sex-work";
    }

    @Override
    public String getRichName() {
        return "Sex Work";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());
        if (account.getNextSexWork() > System.currentTimeMillis()) {
            reply(event,
                    "❌ You must wait another " + (account.getNextSexWork() - System.currentTimeMillis()) * 1000 + "s" + " " + "until you can do sex-work again!",
                    false, true);
            return;
        }

        Random random = ThreadLocalRandom.current();
        if (random.nextBoolean()) {
            int amount = random.nextInt(100, 1000);
            EconomyManager.addMoney(account, amount);
            EconomyManager.updateAccount(account);
            reply(event, getSuccess(event.getUser(), amount));
        } else {
            int amount = random.nextInt(500, 1000);
            EconomyManager.removeMoney(account, amount, true);
            EconomyManager.updateAccount(account);
            reply(event, getFail(event.getUser(), amount));
        }
    }

    private static String getSuccess(User user, int amount) {
        return RESPONSES.success().get(ThreadLocalRandom.current().nextInt(RESPONSES.success().size()))
                .replace("<>", "$").replace("{user}", user.getAsMention())
                .replace("{amount}", Integer.toString(amount));
    }

    private static String getFail(User user, int amount) {
        return RESPONSES.fail().get(ThreadLocalRandom.current().nextInt(RESPONSES.fail().size())).replace("<>", "$")
                .replace("{user}", user.getAsMention()).replace("{amount}", Integer.toString(amount));
    }

    private record Responses(List<String> success, List<String> fail) {
    }
}
