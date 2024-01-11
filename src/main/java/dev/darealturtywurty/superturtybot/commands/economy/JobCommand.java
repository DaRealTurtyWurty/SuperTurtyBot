package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
                                true, true)),
                new SubcommandData("quit", "Quit your job"),
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
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(getName()) && Objects.requireNonNull(event.getSubcommandName())
                .equalsIgnoreCase("register") && event.getFocusedOption().getName().equalsIgnoreCase("job")) {
            event.replyChoices(Arrays.stream(Economy.Job.values()).map(Economy.Job::name).map(String::toLowerCase)
                    .map(str -> str.substring(0, 1).toUpperCase() + str.substring(1))
                    .map(job -> new Command.Choice(job, job)).toList()).queue();
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must specify a subcommand!").queue();
            return;
        }

        Economy account = EconomyManager.getAccount(guild, event.getUser());

        switch (subcommand) {
            case "work" -> {
                if (!EconomyManager.hasJob(account)) {
                    if (!EconomyManager.canWork(account)) {
                        event.getHook().editOriginal("❌ You can start working %s!"
                                .formatted(TimeFormat.RELATIVE.format(account.getNextWork()))).queue();
                        return;
                    }

                    int amount = EconomyManager.workNoJob(account);
                    if (amount == 0) {
                        event.getHook().editOriginal("❌ You are not able to work right now!").queue();
                        EconomyManager.setNextWork(account, System.currentTimeMillis());
                        EconomyManager.updateAccount(account);
                        return;
                    }

                    event.getHook().editOriginal(getResponse(config, event.getUser(), amount)).queue();
                    return;
                }

                if (!EconomyManager.canWork(account)) {
                    event.getHook().editOriginal("❌ You can start working %s!"
                            .formatted(TimeFormat.RELATIVE.format(account.getNextWork()))).queue();
                    return;
                }

                int jobLevel = account.getJobLevel();
                int money = EconomyManager.work(account);
                if (money == 0) {
                    event.getHook().editOriginal("❌ You are not able to work right now!").queue();
                    EconomyManager.setNextWork(account, System.currentTimeMillis());
                    EconomyManager.updateAccount(account);
                    return;
                }

                String levelUpMessage = "";
                if (jobLevel != account.getJobLevel()) {
                    levelUpMessage = " You were just promoted to level " + account.getJobLevel() + "!";
                }

                event.getHook().editOriginal("✅ You worked and earned %s%d!%s"
                        .formatted(config.getEconomyCurrency(), money, levelUpMessage)).queue();
            }
            case "register" -> {
                String job = Objects.requireNonNull(event.getOption("job")).getAsString();
                if (EconomyManager.hasJob(account)) {
                    event.getHook().editOriginal("❌ You already have a job!").queue();
                    return;
                }

                if (!EconomyManager.registerJob(account, job)) {
                    event.getHook().editOriginal("❌ That is not a valid job!").queue();
                    return;
                }

                event.getHook().editOriginal("✅ You have registered for the %s job!".formatted(job)).queue();
            }
            case "quit" -> {
                if (!EconomyManager.hasJob(account)) {
                    event.getHook().editOriginal("❌ You must have a job to quit your job!").queue();
                    return;
                }

                EconomyManager.quitJob(account);
                event.getHook().editOriginal("✅ You have quit your job!").queue();
            }
            case "profile" -> {
                if (!EconomyManager.hasJob(account)) {
                    event.getHook().editOriginal("❌ You must have a job to view your job profile!").queue();
                    return;
                }

                event.getHook().sendMessageEmbeds(EconomyManager.getJobProfile(account).build()).queue();
            }
            default -> event.getHook().editOriginal("❌ You must specify a valid subcommand!").queue();
        }
    }

    private static String getResponse(GuildData config, User user, int amount) {
        return RESPONSES.get(ThreadLocalRandom.current().nextInt(RESPONSES.size()))
                .replace("<>", config.getEconomyCurrency()).replace("{user}", user.getAsMention())
                .replace("{amount}", String.valueOf(amount));
    }
}