package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.time.Instant;
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

        reply(event, createLoveEmbed(event.getUser(), otherUser));
    }

    @Override
    protected void runUserCtx(UserContextInteractionEvent event) {
        User otherUser = event.getTarget();
        reply(event, createLoveEmbed(event.getUser(), otherUser));
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

    public static EmbedBuilder createLoveEmbed(User user1, User user2) {
        float love = calculateLove(user1.getIdLong(), user2.getIdLong());
        String loveBar = makeProgressBar(100, love, 10, "💝", "🖤");
        return new EmbedBuilder()
                .setTitle("Love Calculator")
                .setDescription("**" + user1.getAsMention() + "** and **" + user2.getAsMention() + "** are " + String.format("%.2f", love) + "% compatible!\n" + loveBar)
                .setTimestamp(Instant.now())
                .setColor(calculateColorOfPercentage(love))
                .setFooter("Requested by " + user1.getName(), user1.getEffectiveAvatarUrl());
    }

    public static Color calculateColorOfPercentage(double percentage) {
        // Ensure the percentage is within the valid range [0, 100]
        percentage = Math.max(0, Math.min(100, percentage));

        // Calculate the RGB values based on the percentage
        int red;
        int green;
        int blue = 0; // Set blue to 0 for simplicity

        if (percentage < 50) {
            red = 255;
            green = (int) (255 * (percentage / 50));
        } else {
            red = (int) (255 * ((100 - percentage) / 50));
            green = 255;
        }

        // Create and return the Color object
        return new Color(red, green, blue);
    }
}
