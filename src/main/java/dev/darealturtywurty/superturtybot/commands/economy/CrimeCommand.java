package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// TODO: Crime levels (also possibly heists and other "boss-like" crimes)
public class CrimeCommand extends EconomyCommand {
    private static final Responses RESPONSES;

    static {
        JsonObject json;
        try(final InputStream stream = TurtyBot.loadResource("crime_responses.json")) {
            if (stream == null)
                throw new IllegalStateException("Unable to find sex work responses!");

            json = Constants.GSON.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), JsonObject.class);
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to read sex work responses!", exception);
        }

        final List<String> success = new ArrayList<>();
        final List<String> fail = new ArrayList<>();

        json.getAsJsonArray("success").forEach(element -> success.add(element.getAsString()));
        json.getAsJsonArray("fail").forEach(element -> fail.add(element.getAsString()));
        RESPONSES = new Responses(success, fail);
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.getNextCrime() > System.currentTimeMillis()) {
            event.getHook().editOriginal("❌ You must wait %s before committing another crime!"
                    .formatted(TimeFormat.RELATIVE.format(account.getNextCrime()))).queue();
            return;
        }

        account.setNextCrime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

        if (ThreadLocalRandom.current().nextBoolean()) {
            final int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.addMoney(account, amount);
            event.getHook().editOriginal(getSuccess(config, event.getUser(), amount)).queue();
        } else {
            final int amount = ThreadLocalRandom.current().nextInt(100, 1000);
            EconomyManager.removeMoney(account, amount, true);
            event.getHook().editOriginal(getFail(config, event.getUser(), amount)).queue();
        }

        EconomyManager.updateAccount(account);
    }

    private static String getSuccess(GuildData config, User user, int amount) {
        return RESPONSES.success().get(ThreadLocalRandom.current().nextInt(RESPONSES.success().size()))
                .replace("<>", config.getEconomyCurrency()).replace("{user}", user.getAsMention())
                .replace("{amount}", StringUtils.numberFormat(amount));
    }

    private static String getFail(GuildData config, User user, int amount) {
        return RESPONSES.fail().get(ThreadLocalRandom.current().nextInt(RESPONSES.fail().size()))
                .replace("<>", config.getEconomyCurrency()).replace("{user}", user.getAsMention())
                .replace("{amount}", StringUtils.numberFormat(amount));
    }

    private record Responses(List<String> success, List<String> fail) {
    }
}
