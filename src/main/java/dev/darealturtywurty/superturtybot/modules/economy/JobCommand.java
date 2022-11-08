package dev.darealturtywurty.superturtybot.modules.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class JobCommand extends EconomyCommand {
    private static final List<String> RESPONSES;

    static {
        JsonObject json;
        try {
            json = Constants.GSON.fromJson(
                    IOUtils.toString(Objects.requireNonNull(TurtyBot.class.getResourceAsStream("/work_responses.json")),
                            StandardCharsets.UTF_8), JsonObject.class);
        } catch (final IOException exception) {
            throw new IllegalStateException("Could not load work responses!", exception);
        }

        RESPONSES = new ArrayList<>();
        json.getAsJsonArray("responses").forEach(element -> RESPONSES.add(element.getAsString()));
    }

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("work", "Work for your job"),
                new SubcommandData("register", "Register for a job").addOptions(
                        new OptionData(OptionType.STRING, "job", "The job you want to register for",
                                true).setAutoComplete(true)), new SubcommandData("quit", "Quit your job"),
                new SubcommandData("profile", "View your job profile"));
    }

    @Override
    public String getDescription() {
        return "Manage your job";
    }

    @Override
    public String getName() {
        return "job";
    }

    @Override
    public String getRichName() {
        return "Job";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());

        switch (subcommand) {
            case "work" -> {
                if (!EconomyManager.hasJob(account)) {
                    int amount = EconomyManager.workNoJob(account);
                    if (amount == 0) {
                        reply(event, "❌ You are not able to work right now!", false, true);
                        return;
                    }

                    reply(event, getResponse(event.getUser(), amount));
                    return;
                }

                if (EconomyManager.canWork(account)) {
                    String timestamp = convertToTimestamp(account.getNextWork());
                    reply(event, "❌ You can start working " + timestamp + "!", false, true);
                    return;
                }

                int jobLevel = account.getJobLevel();
                int money = EconomyManager.work(account);
                if (money == 0) {
                    reply(event, "❌ You are not able to work right now!", false, true);
                    return;
                }

                String levelUpMessage = "";
                if (jobLevel != account.getJobLevel()) {
                    levelUpMessage = " You were just promoted to level " + account.getJobLevel() + "!";
                }

                reply(event, "✅ You worked and earned %s%d!%s".formatted("$", money, levelUpMessage));
            }
            case "register" -> {
                String job = event.getOption("job").getAsString();
                if (EconomyManager.hasJob(account)) {
                    reply(event, "❌ You already have a job!", false, true);
                    return;
                }

                if (!EconomyManager.registerJob(account, job)) {
                    reply(event, "❌ That is not a valid job!", false, true);
                    return;
                }

                reply(event, "✅ You have registered for the %s job!".formatted(job));
            }
            case "quit" -> {
                if (!EconomyManager.hasJob(account)) {
                    reply(event, "❌ You must have a job to quit!", false, true);
                    return;
                }

                EconomyManager.quitJob(account);
                reply(event, "✅ You have quit your job!");
            }
            case "profile" -> {
                if (!EconomyManager.hasJob(account)) {
                    reply(event, "❌ You must have a job to view your profile!", false, true);
                    return;
                }

                reply(event, EconomyManager.getJobProfile(account));
            }
            default -> reply(event, "❌ That is not a valid subcommand!", false, true);
        }
    }

    public static String convertToTimestamp(long millis) {
        return "<t:%d:R>".formatted(millis);
    }

    private static String getResponse(User user, int amount) {
        return RESPONSES.get(ThreadLocalRandom.current().nextInt(RESPONSES.size())).replace("<>", "$")
                        .replace("{user}", user.getAsMention()).replace("{amount}", String.valueOf(amount));
    }
}
