package dev.darealturtywurty.superturtybot.modules.economy.command;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.WeightedRandomBag;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;

import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SlotsCommand extends EconomyCommand {
    private static final WeightedRandomBag<String> EMOJIS = new WeightedRandomBag<>();
    private static final Map<String, Outcome> WINNING_FORMATS = new HashMap<>();

    static {
        EMOJIS.addEntry("🍎", 100);
        EMOJIS.addEntry("🍊", 50);
        EMOJIS.addEntry("🍐", 45);
        EMOJIS.addEntry("🍋", 40);
        EMOJIS.addEntry("🍉", 35);
        EMOJIS.addEntry("🍇", 30);
        EMOJIS.addEntry("🍓", 25);
        EMOJIS.addEntry("🍒", 20);
        EMOJIS.addEntry("🍑", 15);
        EMOJIS.addEntry("🍍", 10);
        EMOJIS.addEntry("🥝", 5);
        EMOJIS.addEntry("🍆", 5);
        EMOJIS.addEntry("🥔", 5);
        EMOJIS.addEntry("🧅", 4);
        EMOJIS.addEntry("🥜", 3);
        EMOJIS.addEntry("🌰", 2);
        EMOJIS.addEntry("🍄", 1);

        EMOJIS.addEntry("⭐", 1);
        EMOJIS.addEntry("💎", 1);
        EMOJIS.addEntry("🎲", 25);

        WINNING_FORMATS.put("🍎🍎🍎", new Outcome(Outcome.OutcomeType.WIN, 50));
        WINNING_FORMATS.put("🍊🍊🍊", new Outcome(Outcome.OutcomeType.WIN, 100));
        WINNING_FORMATS.put("🍐🍐🍐", new Outcome(Outcome.OutcomeType.WIN, 200));
        WINNING_FORMATS.put("🍋🍋🍋", new Outcome(Outcome.OutcomeType.WIN, 250));
        WINNING_FORMATS.put("🍉🍉🍉", new Outcome(Outcome.OutcomeType.WIN, 500));
        WINNING_FORMATS.put("🍇🍇🍇", new Outcome(Outcome.OutcomeType.WIN, 750));
        WINNING_FORMATS.put("🍓🍓🍓", new Outcome(Outcome.OutcomeType.WIN, 1000));
        WINNING_FORMATS.put("🍒🍒🍒", new Outcome(Outcome.OutcomeType.WIN, 1250));
        WINNING_FORMATS.put("🍑🍑🍑", new Outcome(Outcome.OutcomeType.WIN, 1500));
        WINNING_FORMATS.put("🍍🍍🍍", new Outcome(Outcome.OutcomeType.WIN, 1750));
        WINNING_FORMATS.put("🥝🥝🥝", new Outcome(Outcome.OutcomeType.WIN, 2000));
        WINNING_FORMATS.put("🍆🍆🍆", new Outcome(Outcome.OutcomeType.WIN, 2500));
        WINNING_FORMATS.put("🥔🥔🥔", new Outcome(Outcome.OutcomeType.WIN, 5000));
        WINNING_FORMATS.put("🧅🧅🧅", new Outcome(Outcome.OutcomeType.WIN, 9000));
        WINNING_FORMATS.put("🥜🥜🥜", new Outcome(Outcome.OutcomeType.WIN, 10000));
        WINNING_FORMATS.put("🌰🌰🌰", new Outcome(Outcome.OutcomeType.WIN, 15000));
        WINNING_FORMATS.put("🍄🍄🍄", new Outcome(Outcome.OutcomeType.WIN, 20000));

        WINNING_FORMATS.put("⭐⭐⭐", new Outcome(Outcome.OutcomeType.WIN, 100000));
        WINNING_FORMATS.put("💎💎💎", new Outcome(Outcome.OutcomeType.WIN, 1000000));
        WINNING_FORMATS.put("🎲", new Outcome(Outcome.OutcomeType.FREE_SPIN, 0));
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 1L);
    }

    @Override
    public String getDescription() {
        return "Play the slots!";
    }

    @Override
    public String getName() {
        return "slots";
    }

    @Override
    public String getRichName() {
        return "Slots";
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "bet-amount", "The amount of money to bet on the slots.", true)
                        .setRequiredRange(1, Integer.MAX_VALUE));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (!event.isFromGuild() || guild == null) {
            reply(event, "❌ You must be in a server to use this command!");
            return;
        }

        int betAmount = event.getOption("bet-amount", 1, OptionMapping::getAsInt);

        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
                .first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        event.deferReply().queue();

        final Economy account = EconomyManager.getAccount(guild, event.getUser());
        if (account.getWallet() < betAmount) {
            event.getHook().editOriginal("❌ You do not have enough money in your wallet to bet that much!").queue();
            return;
        }

        List<Outcome> outcomes = spin(betAmount, false);
        Outcome outcome = outcomes.get(0);

        var embed = new EmbedBuilder();
        embed.setColor(outcome.type().getColor());
        embed.setTimestamp(event.getTimeCreated());
        embed.setFooter(event.getUser().getName() + "'s Slots", event.getUser().getEffectiveAvatarUrl());

        embed.addField("Slots",
                outcome.emojis()[0] + " | " + outcome.emojis()[1] + " | " + outcome.emojis()[2],
                false);

        embed.addField("Outcome", WordUtils.capitalize(
                outcome.type()
                        .name()
                        .replace("_", " ")
                        .toLowerCase(Locale.ROOT)),
                false);

        embed.addField("Winnings", formatCurrency(config.getEconomyCurrency(), outcome.amount()), false);
        event.getHook().editOriginalEmbeds(embed.build()).queue();

        account.setWallet(account.getWallet() + outcome.amount());
        if (outcome.amount() > 0) {
            account.setTotalBetWin(account.getTotalBetWin() + outcome.amount());
        } else {
            account.setTotalBetLoss(account.getTotalBetLoss() + outcome.amount());
        }

        if(outcomes.size() > 1) {
            for (int i = 1; i < outcomes.size(); i++) {
                Outcome freeSpinOutcome = outcomes.get(i);
                var freeSpinEmbed = new EmbedBuilder();
                freeSpinEmbed.setTitle("Free Spin!");
                freeSpinEmbed.setColor(freeSpinOutcome.type().getColor());
                freeSpinEmbed.setTimestamp(event.getTimeCreated());
                freeSpinEmbed.setFooter(event.getUser().getName() + "'s Slots", event.getUser().getEffectiveAvatarUrl());

                freeSpinEmbed.addField("Slots",
                        freeSpinOutcome.emojis()[0] + " | " + freeSpinOutcome.emojis()[1] + " | " + freeSpinOutcome.emojis()[2],
                        false);

                freeSpinEmbed.addField("Outcome", WordUtils.capitalize(
                        freeSpinOutcome.type()
                                .name()
                                .replace("_", " ")
                                .toLowerCase(Locale.ROOT)),
                        false);

                freeSpinEmbed.addField("Winnings", formatCurrency(config.getEconomyCurrency(), freeSpinOutcome.amount()), false);
                event.getHook().sendMessageEmbeds(freeSpinEmbed.build()).queue();

                account.setWallet(account.getWallet() + freeSpinOutcome.amount());
                if (freeSpinOutcome.amount() > 0) {
                    account.setTotalBetWin(account.getTotalBetWin() + freeSpinOutcome.amount());
                } else {
                    account.setTotalBetLoss(account.getTotalBetLoss() + freeSpinOutcome.amount());
                }
            }
        }

        EconomyManager.updateAccount(account);
    }

    private static String formatCurrency(String currencySymbol, int amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        String formattedAmount = currencyFormatter.format(amount);

        // Remove the default currency symbol and replace it with the custom symbol
        formattedAmount = formattedAmount.replace(NumberFormat.getCurrencyInstance(Locale.US).getCurrency().getSymbol(), currencySymbol);

        // Remove the decimal point and the cents
        formattedAmount = formattedAmount.replace(".00", "");

        return formattedAmount;
    }

    private static List<Outcome> spin(int betAmount, boolean isFreeSpin) {
        WeightedRandomBag<String>.Entry entry1 = EMOJIS.getRandomEntry();
        WeightedRandomBag<String>.Entry entry2 = EMOJIS.getRandomEntry();
        WeightedRandomBag<String>.Entry entry3 = EMOJIS.getRandomEntry();
        while (entry1 == null || (isFreeSpin && Objects.equals("🎲", entry1.getObject()))) {
            entry1 = EMOJIS.getRandomEntry();
        } while (entry2 == null || (isFreeSpin && Objects.equals("🎲", entry2.getObject()))) {
            entry2 = EMOJIS.getRandomEntry();
        } while (entry3 == null || (isFreeSpin && Objects.equals("🎲", entry3.getObject()))) {
            entry3 = EMOJIS.getRandomEntry();
        }

        int freeSpins = 0;
        if (Objects.equals("🎲", entry1.getObject())) {
            freeSpins++;
        }

        if (Objects.equals("🎲", entry2.getObject())) {
            freeSpins++;
        }

        if (Objects.equals("🎲", entry3.getObject())) {
            freeSpins++;
        }

        List<Outcome> outcomes = new ArrayList<>();

        Outcome outcome = fetchOutcome(entry1, entry2, entry3, betAmount);
        outcome.setEmoji(0, entry1.getObject());
        outcome.setEmoji(1, entry2.getObject());
        outcome.setEmoji(2, entry3.getObject());
        outcomes.add(outcome);

        if (!isFreeSpin && freeSpins > 0) {
            for (int spin = 0; spin < freeSpins; spin++) {
                Outcome freeSpinOutcome = spin(betAmount, true).get(0);
                if(freeSpinOutcome.amount() < 0)
                    freeSpinOutcome.amount = 0;
                outcomes.add(freeSpinOutcome);
            }
        }

        return outcomes;
    }

    private static Outcome fetchOutcome(WeightedRandomBag<String>.Entry entry1, WeightedRandomBag<String>.Entry entry2, WeightedRandomBag<String>.Entry entry3, int betAmount) {
        if (Objects.equals(entry1, entry2) && Objects.equals(entry2, entry3)) {
            int winnings;
            String fullString = entry1.getObject() + entry2.getObject() + entry3.getObject();
            for (Map.Entry<String, Outcome> outcomeEntry : WINNING_FORMATS.entrySet()) {
                String pattern = outcomeEntry.getKey();
                Outcome outcome = outcomeEntry.getValue();

                if (fullString.contains(pattern)) {
                    winnings = outcome.amount();
                    if (outcome.type() == Outcome.OutcomeType.WIN) {
                        winnings *= betAmount;
                        return new Outcome(Outcome.OutcomeType.WIN, winnings);
                    } else if (outcome.type() == Outcome.OutcomeType.FREE_SPIN) {
                        return new Outcome(Outcome.OutcomeType.FREE_SPIN, betAmount * 2);
                    }
                }
            }

            return new Outcome(Outcome.OutcomeType.FREE_SPIN, 0);
        }

        return new Outcome(Outcome.OutcomeType.LOSS, -betAmount);
    }

    public static class Outcome {
        private final SlotsCommand.Outcome.OutcomeType type;
        private final String[] emojis = new String[3];

        private int amount;

        public Outcome(SlotsCommand.Outcome.OutcomeType type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        public SlotsCommand.Outcome.OutcomeType type() {
            return this.type;
        }

        public int amount() {
            return this.amount;
        }

        public String[] emojis() {
            return this.emojis;
        }

        public void setEmoji(int index, String emoji) {
            this.emojis[index] = emoji;
        }

        public enum OutcomeType {
            WIN(Color.GREEN),
            LOSS(Color.RED),
            FREE_SPIN(Color.YELLOW),
            MULTIPLIER(Color.BLUE);

            private final Color color;

            OutcomeType(Color color) {
                this.color = color;
            }

            public Color getColor() {
                return this.color;
            }
        }
    }
}
