package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CrimeCommand extends EconomyCommand {
    private static final Responses RESPONSES;

    static {
        JsonObject json;
        try (final InputStream stream = TurtyBot.loadResource("crime_responses.json")) {
            if (stream == null)
                throw new IllegalStateException("Unable to find crime responses!");

            json = Constants.GSON.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), JsonObject.class);
        } catch (final IOException exception) {
            throw new IllegalStateException("Unable to read crime responses!", exception);
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
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("beginner", "Commit a beginner crime"),
                new SubcommandData("intermediate", "Commit an intermediate crime"),
                new SubcommandData("advanced", "Commit an advanced crime"),
                new SubcommandData("expert", "Commit an expert crime"),
                new SubcommandData("master", "Commit a master crime"),
                new SubcommandData("profile", "Info about your crime level")
                        .addOptions(new OptionData(OptionType.INTEGER, "level", "Optionally define a crime level", false)
                                .setRequiredRange(0, Integer.MAX_VALUE))
        );
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must specify a level of crime to commit!").queue();
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.getHook().editOriginal("❌ You must be in a guild to commit a crime!").queue();
            return;
        }

        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if(account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot commit crimes! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        int crimeLevel = event.getOption("level", account.getCrimeLevel(), OptionMapping::getAsInt);

        if (subcommand.equalsIgnoreCase("profile")) {
            var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());

            embed.setDescription("Crime Level: %d%nHere are the chances, earnings and losses for each crime level:".formatted(crimeLevel));
            float promotionChance = EconomyManager.getCrimePromotionChance();
            String promotionChanceDisplay = String.format(Locale.ROOT, "%.1f", promotionChance * 100);
            for (var level : CrimeType.values()) {
                float chance = level.getChanceForLevel(crimeLevel);
                float combinedPromotionChance = chance * promotionChance;
                String combinedPromotionDisplay = String.format(Locale.ROOT, "%.1f", combinedPromotionChance * 100);
                BigInteger minAmount = level.getMinAmountForLevel(crimeLevel);
                BigInteger maxAmount = level.getMaxAmountForLevel(crimeLevel);

                BigInteger minLoss = minAmount.divide(BigInteger.TWO);
                BigInteger maxLoss = maxAmount.divide(BigInteger.TWO);
                embed.addField(StringUtils.upperSnakeToSpacedPascal(level.name()), "Chance: %s%%\nPromotion (on success): %s%% (%s%% total)\nEarnings: %s-%s\nLosses: %s-%s".formatted(
                        (int) (chance * 100),
                        promotionChanceDisplay,
                        combinedPromotionDisplay,
                        StringUtils.numberFormat(minAmount, config),
                        StringUtils.numberFormat(maxAmount, config),
                        StringUtils.numberFormat(minLoss, config),
                        StringUtils.numberFormat(maxLoss, config)), false);
            }

            event.getHook().editOriginalEmbeds(embed.build()).queue();
            return;
        }

        var level = CrimeType.byName(subcommand);
        if (level == null) {
            event.getHook().editOriginal("❌ That is not a valid crime level!").queue();
            return;
        }

        if (account.getNextCrime() > System.currentTimeMillis() && !Environment.INSTANCE.isDevelopment()) {
            event.getHook().editOriginalFormat("❌ You can commit a crime again %s!",
                    TimeFormat.RELATIVE.format(account.getNextCrime())).queue();
            return;
        }

        long crimeCooldownMillis = TimeUnit.MINUTES.toMillis(10);
        if (account.getCrimeBoostUntil() > System.currentTimeMillis()) {
            crimeCooldownMillis = Math.max(TimeUnit.MINUTES.toMillis(3), Math.round(crimeCooldownMillis * 0.75f));
        }

        account.setNextCrime(System.currentTimeMillis() + crimeCooldownMillis);

        var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());

        if (level.hasSuccess(account.getCrimeLevel())) {
            BigInteger amount = EconomyManager.successfulCrime(account, level);
            int newCrimeLevel = account.getCrimeLevel();
            boolean promoted = crimeLevel != newCrimeLevel;

            var content = new StringBuilder(getSuccess(config, event.getUser(), amount));
            if (promoted) {
                content.append("\n\n✅ You have been promoted, by %d level%s. You are now a %d level criminal!"
                        .formatted(newCrimeLevel - crimeLevel, newCrimeLevel - crimeLevel == 1 ? "" : "s", newCrimeLevel));
            }

            embed.setDescription(content.toString());
            embed.setColor(0x00AA00);
        } else {
            int prevJobLevel = account.getJobLevel();
            BigInteger amount = EconomyManager.caughtCrime(account, level);
            int newJobLevel = account.getJobLevel();
            boolean demoted = prevJobLevel != newJobLevel;

            var content = new StringBuilder(getFail(config, event.getUser(), amount));
            if (demoted) {
                content.append("\n\n❌ You have been demoted, by %d level%s, to level %d in your current job."
                        .formatted(prevJobLevel - newJobLevel, prevJobLevel - newJobLevel == 1 ? "" : "s", newJobLevel));
            }

            // 5% chance to be imprisoned
            boolean imprisoned = ThreadLocalRandom.current().nextFloat() <= 0.05f;
            if (imprisoned) {
                long imprisonDurationMillis = TimeUnit.MINUTES.toMillis(ThreadLocalRandom.current().nextLong(5, 60));
                account.setImprisonedUntil(System.currentTimeMillis() + imprisonDurationMillis);
                content.append("\n\n🚨 You have been been imprisoned! You will be released %s."
                        .formatted(TimeFormat.RELATIVE.format(account.getImprisonedUntil())));
            }

            embed.setDescription(content.toString());
            embed.setColor(0xAA0000);
        }

        embed.appendDescription("\n\nYou can commit a crime again %s!".formatted(TimeFormat.RELATIVE.format(account.getNextCrime())));

        event.getHook().editOriginalEmbeds(embed.build()).queue();
        EconomyManager.updateAccount(account);
    }

    private static String getSuccess(GuildData config, User user, BigInteger amount) {
        return RESPONSES.success().get(ThreadLocalRandom.current().nextInt(RESPONSES.success().size()))
                .replace("{user}", user.getAsMention())
                .replace("{amount}", StringUtils.numberFormat(amount, config));
    }

    private static String getFail(GuildData config, User user, BigInteger amount) {
        return RESPONSES.fail().get(ThreadLocalRandom.current().nextInt(RESPONSES.fail().size()))
                .replace("{user}", user.getAsMention())
                .replace("{amount}", StringUtils.numberFormat(amount, config));
    }

    private record Responses(List<String> success, List<String> fail) {
    }

    @Getter
    @AllArgsConstructor
    @ToString
    public enum CrimeType {
        BEGINNER(0.5F, 500, 10_000),
        INTERMEDIATE(0.25F, 10_000, 25_000),
        ADVANCED(0.125F, 25_000, 50_000),
        EXPERT(0.05F, 50_000, 250_000),
        MASTER(0.01F, 250_000, 1_000_000);

        private final float successChance;
        private final long minBaseAmount;
        private final long maxBaseAmount;

        public long getRandomBaseAmount() {
            return ThreadLocalRandom.current().nextLong(this.minBaseAmount, this.maxBaseAmount + 1);
        }

        public BigInteger getMinAmountForLevel(int level) {
            return BigDecimal.valueOf(this.minBaseAmount).multiply(BigDecimal.valueOf(Math.pow(1.02, level))).toBigInteger();
        }

        public BigInteger getMaxAmountForLevel(int level) {
            return BigDecimal.valueOf(this.maxBaseAmount).multiply(BigDecimal.valueOf(Math.pow(1.02, level))).toBigInteger();
        }

        public BigInteger getRandomAmountForLevel(int level) {
            return BigDecimal.valueOf(getRandomBaseAmount()).multiply(BigDecimal.valueOf(Math.pow(1.02, level))).toBigInteger();
        }

        public float getChanceForLevel(int level) {
            float maxChance = Math.min(0.75F, this.successChance + 0.25F);
            float chance = this.successChance + (level * 0.0025F);
            return Math.min(maxChance, chance);
        }

        public boolean hasSuccess(int level) {
            return ThreadLocalRandom.current().nextFloat() <= getChanceForLevel(level);
        }

        public static @Nullable CrimeCommand.CrimeType byName(String name) {
            for (CrimeType level : values()) {
                if (level.name().equalsIgnoreCase(name))
                    return level;
            }

            return null;
        }
    }
}
