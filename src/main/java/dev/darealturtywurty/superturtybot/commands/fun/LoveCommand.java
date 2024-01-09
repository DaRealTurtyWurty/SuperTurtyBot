package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// TODO: Make this command only store the love for a certain amount of time and then it will be recalculated.
public class LoveCommand extends CoreCommand {
    private static final Map<Pair<Long, Long>, Float> USER_LOVE_MAP = new HashMap<>();

    public LoveCommand() {
        super(new Types(true, false, false, true));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.USER, "user", "The other user to calculate love with!", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Calculates the love between you and another user!";
    }

    @Override
    public String getName() {
        return "love";
    }

    @Override
    public String getRichName() {
        return "Love";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        User otherUser = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        if (otherUser == null) {
            reply(event, "❌ You need to specify a user!", false, true);
            return;
        }

        float love = calculateLove(event.getUser().getIdLong(), otherUser.getIdLong());
        String loveBar = makeProgressBar(100, love, 10, "❤", "♡");
        reply(event, "💖 **" + event.getUser().getAsMention() + "** and **" + otherUser.getAsMention() + "** are " + String.format("%.2f", love) + "% compatible!\n" + loveBar);
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        User otherUser = event.getTarget();
        float love = calculateLove(event.getUser().getIdLong(), otherUser.getIdLong());
        String loveBar = makeProgressBar(100, love, 10, "❤", "♡");
        reply(event, "💖 **" + event.getUser().getAsMention() + "** and **" + otherUser.getAsMention() + "** are " + String.format("%.2f", love) + "% compatible!\n" + loveBar);
    }

    public static float calculateLove(long user1, long user2) {
        float love;
        if (user1 == user2)
            return 100;

        // check if the love has already been calculated
        love = USER_LOVE_MAP.entrySet().stream()
                .filter(entry -> (entry.getKey().getLeft() == user1 && entry.getKey().getRight() == user2)
                        || (entry.getKey().getLeft() == user2 && entry.getKey().getRight() == user1))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(-1f);

        if (love == -1f) {
            love = (float) (Math.random() * 100);
            USER_LOVE_MAP.put(Pair.of(user1, user2), love);
        }

        return love;
    }

    public static String makeProgressBar(int max, float current, int length, String filled, String empty) {
        int filledLength = (int) (length * (current / max));
        int emptyLength = length - filledLength;

        return filled.repeat(Math.max(0, filledLength)) +
                empty.repeat(Math.max(0, emptyLength));
    }
}
