package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.core.util.object.WeightedRandomBag;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;

import java.awt.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

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

        WINNING_FORMATS.put("🍎🍎🍎", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(50)));
        WINNING_FORMATS.put("🍊🍊🍊", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(100)));
        WINNING_FORMATS.put("🍐🍐🍐", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(200)));
        WINNING_FORMATS.put("🍋🍋🍋", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(250)));
        WINNING_FORMATS.put("🍉🍉🍉", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(500)));
        WINNING_FORMATS.put("🍇🍇🍇", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(750)));
        WINNING_FORMATS.put("🍓🍓🍓", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(1000)));
        WINNING_FORMATS.put("🍒🍒🍒", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(1250)));
        WINNING_FORMATS.put("🍑🍑🍑", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(1500)));
        WINNING_FORMATS.put("🍍🍍🍍", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(1750)));
        WINNING_FORMATS.put("🥝🥝🥝", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(2000)));
        WINNING_FORMATS.put("🍆🍆🍆", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(2500)));
        WINNING_FORMATS.put("🥔🥔🥔", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(5000)));
        WINNING_FORMATS.put("🧅🧅🧅", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(9000)));
        WINNING_FORMATS.put("🥜🥜🥜", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(10000)));
        WINNING_FORMATS.put("🌰🌰🌰", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(15000)));
        WINNING_FORMATS.put("🍄🍄🍄", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(20000)));

        WINNING_FORMATS.put("⭐⭐⭐", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(100000)));
        WINNING_FORMATS.put("💎💎💎", new Outcome(Outcome.OutcomeType.WIN, BigInteger.valueOf(1000000)));
        WINNING_FORMATS.put("🎲", new Outcome(Outcome.OutcomeType.FREE_SPIN, BigInteger.ZERO));
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
                new OptionData(OptionType.STRING, "bet-amount", "The amount of money to bet on the slots.", true));
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        BigInteger amount = event.getOption("bet-amount", StringUtils.getAsBigInteger(event));
        if (amount == null) return;
        if (amount.signum() <= 0) {
            event.getHook().editOriginal("❌ You cannot bet less than %s1!".formatted(config.getEconomyCurrency())).queue();
            return;
        }

        play(event.getHook(), event.getMember(), guild, config, amount);
    }

    private static EmbedBuilder createEmbed(Outcome outcome, Member member, GuildData config, String title) {
        var embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setColor(outcome.getType().getColor());
        embed.setTimestamp(Instant.now());
        embed.setFooter(member.getEffectiveName() + "'s Slots", member.getEffectiveAvatarUrl());

        embed.addField("Slots",
                outcome.getEmojis()[0] + " | " + outcome.getEmojis()[1] + " | " + outcome.getEmojis()[2],
                false);

        embed.addField("Outcome", WordUtils.capitalize(
                        outcome.getType()
                                .name()
                                .replace("_", " ")
                                .toLowerCase(Locale.ROOT)),
                false);

        embed.addField("Winnings", StringUtils.numberFormat(outcome.getAmount(), config), false);

        return embed;
    }

    // Use the refactored method in place of the old ones
    private static EmbedBuilder createFreeSpinEmbed(Outcome outcome, Member member, GuildData config) {
        return createEmbed(outcome, member, config, "Free Spin!");
    }

    private static EmbedBuilder createNormalEmbed(Outcome outcome, Member member, GuildData config) {
        return createEmbed(outcome, member, config, "Slots");
    }

    private static void play(Consumer<String> editText, Function<MessageEmbed, RestAction<Message>> editEmbeds, Member member, Guild guild, GuildData config, BigInteger betAmount) {
        if (betAmount.signum() <= 0) {
            editText.accept("❌ You cannot bet less than %s1!".formatted(config.getEconomyCurrency()));
            return;
        }

        final Economy account = EconomyManager.getOrCreateAccount(guild, member.getUser());
        if (account.getWallet().compareTo(betAmount) < 0) {
            betAmount = account.getWallet();

            if (betAmount.signum() <= 0) {
                editText.accept("❌ You do not have enough money in your wallet to bet that much!");
                return;
            }
        }

        List<Outcome> outcomes = spin(betAmount, false);
        Outcome outcome = outcomes.getFirst();
        var embed = createNormalEmbed(outcome, member, config);

        final BigInteger finalBetAmount = betAmount;
        editEmbeds.apply(embed.build()).queue(message -> {
            if (outcomes.size() == 1) {
                // create button to play again
                if (account.getWallet().compareTo(finalBetAmount) >= 0) {
                    message.editMessageComponents(ActionRow.of(Button.primary("slots-play-again", "Replay"))).queue();
                    createButtonWaiter(message, member, guild, message.getChannel().getIdLong(),
                            message.getIdLong(), config, finalBetAmount).build();
                }
            }
        });

        EconomyManager.addMoney(account, outcome.getAmount());
        account.addTransaction(outcome.getAmount(), MoneyTransaction.SLOTS);
        if (outcome.getAmount().signum() > 0) {
            EconomyManager.betWin(account, outcome.getAmount());
        } else {
            EconomyManager.betLoss(account, outcome.getAmount().negate());
        }

        if (outcomes.size() > 1) {
            for (int i = 1; i < outcomes.size(); i++) {
                Outcome freeSpinOutcome = outcomes.get(i);
                var freeSpinEmbed = createFreeSpinEmbed(freeSpinOutcome, member, config);
                int finalI = i;
                editEmbeds.apply(freeSpinEmbed.build()).queue(message -> {
                    if (finalI == outcomes.size() - 1 && account.getWallet().compareTo(finalBetAmount) >= 0) {
                        message.editMessageComponents(ActionRow.of(Button.primary("slots-play-again", "Replay"))).queue();
                        createButtonWaiter(message, member, guild, message.getChannel().getIdLong(),
                                message.getIdLong(), config, finalBetAmount).build();
                    }
                });

                EconomyManager.addMoney(account, freeSpinOutcome.getAmount());
                if (freeSpinOutcome.getAmount().signum() > 0) {
                    account.addTransaction(freeSpinOutcome.getAmount(), MoneyTransaction.SLOTS);
                    EconomyManager.betWin(account, freeSpinOutcome.getAmount());
                }
            }
        }

        EconomyManager.updateAccount(account);
    }

    private static void play(InteractionHook hook, Member member, Guild guild, GuildData config, BigInteger betAmount) {
        play(str -> hook.editOriginal(str).queue(), hook::editOriginalEmbeds, member, guild, config, betAmount);
    }

    private static void play(Message message, Member member, Guild guild, GuildData config, BigInteger betAmount) {
        play(str -> message.editMessage(str).queue(), message::editMessageEmbeds, member, guild, config, betAmount);
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createButtonWaiter(Message message, Member member, Guild guild, long channelId, long messageId, GuildData config, BigInteger betAmount) {
        //noinspection DataFlowIssue - This is a false positive
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .timeout(1, TimeUnit.MINUTES)
                .timeoutAction(() -> message.editMessageComponents().queue())
                .condition(event -> event.getUser().getIdLong() == member.getIdLong() &&
                        event.getMessageIdLong() == messageId &&
                        event.getComponentId().equals("slots-play-again") &&
                        event.isFromGuild() &&
                        event.getGuild().getIdLong() == guild.getIdLong() &&
                        event.getChannel().getIdLong() == channelId)
                .failure(() -> message.editMessageComponents().queue())
                .success(event ->
                        message.editMessageComponents()
                                .flatMap(msg -> msg.reply("Playing again..."))
                                .queue(msg -> play(msg, member, guild, config, betAmount)));
    }

    private static List<Outcome> handleFreeSpins(BigInteger betAmount, int freeSpins) {
        List<Outcome> outcomes = new ArrayList<>();
        for (int spin = 0; spin < freeSpins; spin++) {
            Outcome freeSpinOutcome = spin(betAmount, true).getFirst();
            if (freeSpinOutcome.getAmount().signum() < 0)
                freeSpinOutcome.amount = BigInteger.ZERO;
            outcomes.add(freeSpinOutcome);
        }
        return outcomes;
    }

    private static List<Outcome> spin(BigInteger betAmount, boolean isFreeSpin) {
        WeightedRandomBag<String>.Entry entry1 = EMOJIS.getRandomEntry();
        WeightedRandomBag<String>.Entry entry2 = EMOJIS.getRandomEntry();
        WeightedRandomBag<String>.Entry entry3 = EMOJIS.getRandomEntry();
        while (entry1 == null || (isFreeSpin && Objects.equals("🎲", entry1.getObject()))) {
            entry1 = EMOJIS.getRandomEntry();
        }
        while (entry2 == null || (isFreeSpin && Objects.equals("🎲", entry2.getObject()))) {
            entry2 = EMOJIS.getRandomEntry();
        }
        while (entry3 == null || (isFreeSpin && Objects.equals("🎲", entry3.getObject()))) {
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
            outcomes.addAll(handleFreeSpins(betAmount, freeSpins));
        }

        return outcomes;
    }

    private static Outcome fetchOutcome(WeightedRandomBag<String>.Entry entry1, WeightedRandomBag<String>.Entry entry2, WeightedRandomBag<String>.Entry entry3, BigInteger betAmount) {
        if (!Objects.equals(entry1, entry2) || !Objects.equals(entry2, entry3)) {
            return new Outcome(Outcome.OutcomeType.LOSS, betAmount.negate());
        }
        BigInteger winnings;
        String fullString = entry1.getObject() + entry2.getObject() + entry3.getObject();
        for (Map.Entry<String, Outcome> outcomeEntry : WINNING_FORMATS.entrySet()) {
            String pattern = outcomeEntry.getKey();
            Outcome outcome = outcomeEntry.getValue();

            if (!fullString.contains(pattern)) continue;
            winnings = outcome.getAmount();
            if (outcome.getType() == Outcome.OutcomeType.WIN) {
                return new Outcome(Outcome.OutcomeType.WIN, winnings.multiply(betAmount));
            } else if (outcome.getType() == Outcome.OutcomeType.FREE_SPIN) {
                return new Outcome(Outcome.OutcomeType.FREE_SPIN, betAmount.add(betAmount));
            }
        }

        return new Outcome(Outcome.OutcomeType.FREE_SPIN, BigInteger.ZERO);
    }

    @Getter
    public static class Outcome {
        private final SlotsCommand.Outcome.OutcomeType type;
        private final String[] emojis = new String[3];

        private BigInteger amount;

        public Outcome(SlotsCommand.Outcome.OutcomeType type, BigInteger amount) {
            this.type = type;
            this.amount = amount;
        }

        public void setEmoji(int index, String emoji) {
            this.emojis[index] = emoji;
        }

        @Getter
        public enum OutcomeType {
            WIN(Color.GREEN),
            LOSS(Color.RED),
            FREE_SPIN(Color.YELLOW),
            MULTIPLIER(Color.BLUE);

            private final Color color;

            OutcomeType(Color color) {
                this.color = color;
            }
        }
    }
}
