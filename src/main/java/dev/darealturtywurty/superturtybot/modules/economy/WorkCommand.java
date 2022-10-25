package dev.darealturtywurty.superturtybot.modules.economy;

import com.google.gson.JsonObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WorkCommand extends EconomyCommand {
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
        Guild guild = event.getGuild();
        Economy account = EconomyManager.fetchAccount(guild, user);
        if (account.getNextWorkTime() > System.currentTimeMillis()) {
            reply(event,
                    "❌ You must wait " + (account.getNextWorkTime() - System.currentTimeMillis()) / 1000 + "s to " +
                            "work again!",
                    false, true);
            return;
        }

        int amount = (int) (Math.random() * 1000);
        EconomyManager.addMoney(guild, user, amount);

        account.setNextWorkTime(System.currentTimeMillis() + 3600000);
        Database.getDatabase().economy.updateOne(
                Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong())),
                Updates.set("nextWorkTime", account.getNextWorkTime()));
        reply(event, getResponse(user, amount));
    }

    private static String getResponse(User user, int amount) {
        return RESPONSES.get(ThreadLocalRandom.current().nextInt(RESPONSES.size()))
                        .replace("{user}", user.getAsMention()).replace("{amount}", String.valueOf(amount));
    }

    public static final List<String> RESPONSES;

    static {
        JsonObject json;
        try {
            json = Constants.GSON.fromJson(IOUtils.toString(TurtyBot.class.getResourceAsStream("/work_responses.json"),
                    StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load work responses!", exception);
        }

        RESPONSES = new ArrayList<>();
        json.getAsJsonArray("responses").forEach(element -> RESPONSES.add(element.getAsString()));
    }
}
