package dev.darealturtywurty.superturtybot.modules.economy.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RobCommand extends EconomyCommand {
    private static final Responses RESPONSES;

    static {
        JsonObject json;
        try {
            json = Constants.GSON.fromJson(
                    IOUtils.toString(Objects.requireNonNull(TurtyBot.class.getResourceAsStream("/rob_responses.json")),
                            StandardCharsets.UTF_8), JsonObject.class);
        } catch (JsonSyntaxException | IOException exception) {
            throw new IllegalStateException("Unable to parse economy rob responses!", exception);
        }

        final List<String> success = new ArrayList<>();
        final List<String> fail = new ArrayList<>();

        json.getAsJsonArray("success").forEach(element -> success.add(element.getAsString()));
        json.getAsJsonArray("fail").forEach(element -> fail.add(element.getAsString()));

        RESPONSES = new Responses(success, fail);
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
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        event.deferReply().queue();

        final Guild guild = event.getGuild();
        final Economy account = EconomyManager.getAccount(guild, event.getUser());
        if (account.getNextRob() > System.currentTimeMillis()) {
            event.getHook().editOriginal("❌ You can rob again %s!"
                    .formatted(TimeFormat.RELATIVE.format(account.getNextRob()))).queue();
            return;
        }

        final User user = Objects.requireNonNull(event.getOption("user")).getAsUser();
        if (!guild.isMember(user)) {
            event.getHook().editOriginal("❌ The user to rob must be in this server!").queue();
            return;
        }

        account.setNextRob(System.currentTimeMillis() + 60 * 120 * 1000);
        Database.getDatabase().economy.updateOne(
                Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", event.getUser().getIdLong())),
                Updates.set("nextRobTime", account.getNextRob()));

        final Economy robAccount = EconomyManager.getAccount(guild, user);
        if (robAccount.getWallet() <= 0) {
            event.getHook().editOriginal("❌ Better luck next time, this user's wallet is empty!").queue();
            return;
        }

        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
                .first();
        if (config == null) {
            config = new GuildConfig(event.getGuild().getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        final Random random = ThreadLocalRandom.current();
        if (random.nextBoolean()) {
            final int robbedAmount = random.nextInt(1, robAccount.getWallet());
            account.addWallet(robbedAmount);
            robAccount.removeWallet(robbedAmount);

            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.GREEN)
                    .setDescription(RESPONSES.getSuccess(config, event.getUser(), user, robbedAmount)).build()).queue();
        } else {
            final int fineAmount = random.nextInt(1, EconomyManager.getBalance(account));
            account.removeWallet(fineAmount);
            robAccount.addWallet(fineAmount);

            event.getHook().sendMessageEmbeds(new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.RED)
                    .setDescription(RESPONSES.getFail(config, event.getUser(), user, fineAmount)).build()).queue();
        }

        Database.getDatabase().economy.updateOne(
                Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", event.getUser().getIdLong())),
                Updates.set("wallet", account.getWallet()));
        Database.getDatabase().economy.updateOne(
                Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong())),
                Updates.set("wallet", robAccount.getWallet()));
    }

    public record Responses(List<String> success, List<String> fail) {
        public String getSuccess(GuildConfig config, User robber, User robbed, int amount) {
            return success().get(ThreadLocalRandom.current().nextInt(success().size()))
                    .replace("{robber}", robber.getAsMention()).replace("{robbed}", robbed.getAsMention())
                    .replace("{amount}", Integer.toString(amount)).replace("<>", config.getEconomyCurrency());
        }

        public String getFail(GuildConfig config, User robber, User robbed, int amount) {
            return fail().get(ThreadLocalRandom.current().nextInt(fail().size()))
                    .replace("{robber}", robber.getAsMention()).replace("{robbed}", robbed.getAsMention())
                    .replace("{amount}", Integer.toString(amount)).replace("<>", config.getEconomyCurrency());
        }
    }
}