package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
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
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// TODO: Possibly heists and other "boss-like" crimes
public class CrimeCommand extends EconomyCommand {
    private static final Responses RESPONSES;

    static {
        JsonObject json;
        try(final InputStream stream = TurtyBot.loadResource("crime_responses.json")) {
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
                        .addOption(OptionType.INTEGER, "level", "Optionally define a crime level", false)
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
        int crimeLevel = event.getOption("level", account.getCrimeLevel(), OptionMapping::getAsInt);

        if(subcommand.equalsIgnoreCase("profile")) {
            var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());

            embed.addField("Crime Level", String.valueOf(crimeLevel), false);
            embed.addField("Chances of success", "for level " + crimeLevel, false);
            for(CrimeLevel level : CrimeLevel.values()) {
                embed.addField(level.name(), String.format("%.2f", level.getChanceForLevel(crimeLevel) * 100f) + "%", false);
            }
            embed.setColor(Color.GREEN);
            event.getHook().editOriginalEmbeds(embed.build()).queue();
            return;
        }

        var level = CrimeLevel.byName(subcommand);
        if(level == null) {
            event.getHook().editOriginal("❌ That is not a valid crime level!").queue();
            return;
        }

        if (account.getNextCrime() > System.currentTimeMillis()) {
            event.getHook().editOriginal("❌ You must wait %s before committing another crime!"
                    .formatted(TimeFormat.RELATIVE.format(account.getNextCrime()))).queue();
            return;
        }

        account.setNextCrime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

        var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setFooter(member.getEffectiveName(), member.getEffectiveAvatarUrl());

        if (level.hasSuccess(account.getCrimeLevel())) {
            int amount = EconomyManager.successfulCrime(account, level);
            int newCrimeLevel = account.getCrimeLevel();
            embed.setDescription(getSuccess(config, event.getUser(), amount) +
                    (crimeLevel != newCrimeLevel ? "\n\n✅ You are now a level %d criminal!".formatted(newCrimeLevel) : ""));
            embed.setColor(0x00AA00);
        } else {
            int jobLevel = account.getJobLevel();
            int amount = EconomyManager.caughtCrime(account, level);
            int newJobLevel = account.getJobLevel();
            embed.setDescription(getFail(config, event.getUser(), amount) +
                    (jobLevel != newJobLevel ? "\n\n❌ You have been demoted to level %d(%d levels) for your job!".formatted(newJobLevel, newJobLevel - jobLevel) : ""));
            embed.setColor(0xAA0000);
        }

        event.getHook().editOriginalEmbeds(embed.build()).queue();
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

    @Getter
    @AllArgsConstructor
    @ToString
    public enum CrimeLevel {
        BEGINNER(0.5F, 500, 5000),
        INTERMEDIATE(0.25F, 5000, 10000),
        ADVANCED(0.125F, 10000, 20000),
        EXPERT(0.05F, 20000, 50000),
        MASTER(0.01F, 50000, 100000);

        private final float successChance;
        private final int minBaseAmount;
        private final int maxBaseAmount;

        public int getBaseAmount() {
            return ThreadLocalRandom.current().nextInt(this.minBaseAmount, this.maxBaseAmount + 1);
        }

        public int getAmountForLevel(int level) {
            return (int) (getBaseAmount() * Math.pow(1.05, level));
        }

        public float getChanceForLevel(int level) {
            return Math.min(1.0F, this.successChance + (level * 0.05F));
        }

        public boolean hasSuccess(int level) {
            return ThreadLocalRandom.current().nextFloat() <= getChanceForLevel(level);
        }

        public static @Nullable CrimeLevel byName(String name) {
            for (CrimeLevel level : values()) {
                if (level.name().equalsIgnoreCase(name)) {
                    return level;
                }
            }

            return null;
        }
    }
}
