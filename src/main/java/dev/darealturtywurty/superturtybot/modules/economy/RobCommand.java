package dev.darealturtywurty.superturtybot.modules.economy;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RobCommand extends EconomyCommand {
    private static final Responses RESPONSES;
    
    static {
        JsonObject json;
        try {
            json = Constants.GSON.fromJson(
                IOUtils.toString(TurtyBot.class.getResourceAsStream("/rob_responses.json"), StandardCharsets.UTF_8),
                JsonObject.class);
        } catch (JsonSyntaxException | IOException exception) {
            throw new IllegalStateException("Unable to parse economy rob responses!", exception);
        }
        
        final List<String> success = new ArrayList<>();
        final List<String> fail = new ArrayList<>();
        
        json.getAsJsonArray("success").forEach(element -> success.add(element.getAsString()));
        json.getAsJsonArray("fail").forEach(element -> fail.add(element.getAsString()));
        
        RESPONSES = new Responses(success, fail);
    }
    
    public RobCommand() {
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The user to rob", true));
    }

    @Override
    public String getDescription() {
        return "Robs a user of the money in their economy wallet.";
    }

    @Override
    public String getHowToUse() {
        return "/rob [user]";
    }

    @Override
    public String getName() {
        return "rob";
    }

    @Override
    public String getRichName() {
        return "Rob";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        final Guild guild = event.getGuild();

        final Economy account = EconomyManager.fetchAccount(guild, event.getUser());
        final long nextRobTime = account.getNextRobTime();
        if (nextRobTime > System.currentTimeMillis()) {
            reply(event, "❌ You must wait another `%d` seconds until you can rob someone!"
                .formatted((nextRobTime - System.currentTimeMillis()) / 1000));
            return;
        }

        final User user = event.getOption("user").getAsUser();
        if (!guild.isMember(user)) {
            reply(event, "❌ The user to rob must be in this server!", false, true);
            return;
        }
        
        account.setNextRobTime(System.currentTimeMillis() + 60 * 120 * 1000);
        Database.getDatabase().economy.updateOne(
            Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", event.getUser().getIdLong())),
            Updates.set("nextRobTime", account.getNextRobTime()));

        final Economy robAccount = EconomyManager.fetchAccount(guild, user);
        if (robAccount.getWallet() <= 0) {
            reply(event, "❌ Better luck next time, this user's wallet is empty!", false, true);
            return;
        }

        final Random random = ThreadLocalRandom.current();
        if (random.nextBoolean()) {
            final int robbedAmount = random.nextInt(1, robAccount.getWallet());
            reply(event, new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.GREEN)
                .setDescription(RESPONSES.getSuccess(event.getUser(), user, robbedAmount)));
            account.addWallet(robbedAmount);
            robAccount.removeWallet(robbedAmount);
        } else {
            final int fineAmount = random.nextInt(1, account.getBalance());
            reply(event, new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.RED)
                .setDescription(RESPONSES.getFail(event.getUser(), user, fineAmount)));
            account.removeWallet(fineAmount);
            robAccount.addWallet(fineAmount);
        }
        
        Database.getDatabase().economy.updateOne(
            Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", event.getUser().getIdLong())),
            Updates.set("wallet", account.getWallet()));
        Database.getDatabase().economy.updateOne(
            Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong())),
            Updates.set("wallet", robAccount.getWallet()));
    }
    
    public static record Responses(List<String> success, List<String> fail) {
        public String getSuccess(User robber, User robbed, int amount) {
            return success().get(ThreadLocalRandom.current().nextInt(success().size()))
                .replace("{robber}", robber.getAsMention()).replace("{robbed}", robbed.getAsMention())
                .replace("{amount}", Integer.toString(amount));
        }

        public String getFail(User robber, User robbed, int amount) {
            return fail().get(ThreadLocalRandom.current().nextInt(fail().size()))
                .replace("{robber}", robber.getAsMention()).replace("{robbed}", robbed.getAsMention())
                .replace("{amount}", Integer.toString(amount));
        }
    }
}
