package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RobCommand extends EconomyCommand {
    private static final Responses RESPONSES;

    static {
        JsonObject json;
        try (final InputStream stream = TurtyBot.loadResource("rob_responses.json")) {
            if (stream == null)
                throw new IllegalStateException("Unable to read rob_responses.json!");

            json = Constants.GSON.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException exception) {
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
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

        if (CrashCommand.isPlaying(guild.getIdLong(), user.getIdLong())) {
            event.getHook().editOriginal("❌ You can't rob a user who is playing the crash game!").queue();
            return;
        }

        account.setNextRob(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30));

        final Economy robAccount = EconomyManager.getOrCreateAccount(guild, user);
        if (robAccount.getWallet() <= 0) {
            event.getHook().editOriginal("❌ Better luck next time, this user's wallet is empty!").queue();
            EconomyManager.updateAccount(account);
            return;
        }

        final Random random = ThreadLocalRandom.current();
        if (random.nextBoolean()) {
            final long robbedAmount = random.nextLong(1, robAccount.getWallet() / 4);
            account.addWallet(robbedAmount);
            robAccount.removeWallet(robbedAmount);

            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setTimestamp(Instant.now())
                            .setColor(Color.GREEN)
                            .setDescription(RESPONSES.getSuccess(config, event.getUser(), user, robbedAmount))
                            .build())
                    .queue();
        } else {
            int crimeLevel = account.getCrimeLevel();
            long bank = account.getBank();
            long bankFine = (bank / 100) * crimeLevel;
            long fineAmount = random.nextLong(1_000, bankFine);

            account.removeWallet(fineAmount);
            robAccount.addWallet(fineAmount);

            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setTimestamp(Instant.now())
                            .setColor(Color.RED)
                            .setDescription(RESPONSES.getFail(config, event.getUser(), user, fineAmount))
                            .build())
                    .queue();
        }

        EconomyManager.updateAccount(account);
        EconomyManager.updateAccount(robAccount);
    }

    public record Responses(List<String> success, List<String> fail) {
        public String getSuccess(GuildData config, User robber, User robbed, long amount) {
            return success().get(ThreadLocalRandom.current().nextInt(success().size()))
                    .replace("{robber}", robber.getAsMention()).replace("{robbed}", robbed.getAsMention())
                    .replace("{amount}", StringUtils.numberFormat(amount)).replace("<>", config.getEconomyCurrency());
        }

        public String getFail(GuildData config, User robber, User robbed, long amount) {
            return fail().get(ThreadLocalRandom.current().nextInt(fail().size()))
                    .replace("{robber}", robber.getAsMention()).replace("{robbed}", robbed.getAsMention())
                    .replace("{amount}", StringUtils.numberFormat(amount)).replace("<>", config.getEconomyCurrency());
        }
    }
}