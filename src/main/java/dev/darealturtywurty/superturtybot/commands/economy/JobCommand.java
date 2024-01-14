package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class JobCommand extends EconomyCommand {
    private static final List<String> RESPONSES;

    static {
        JsonObject json;
        try (final InputStream stream = TurtyBot.loadResource("work_responses.json")) {
            if (stream == null)
                throw new IllegalStateException("Could not load work_responses.json!");

            json = Constants.GSON.fromJson(IOUtils.toString(stream, StandardCharsets.UTF_8), JsonObject.class);
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
                new SubcommandData("profile", "View your job profile"),
                new SubcommandData("promote", "Promote yourself to the next job level"));
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
        Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());

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

                int money = EconomyManager.work(account);
                if (money == 0) {
                    event.getHook().editOriginal("❌ You are not able to work right now!").queue();
                    EconomyManager.setNextWork(account, System.currentTimeMillis());
                    EconomyManager.updateAccount(account);
                    return;
                }

                String levelUpMessage = "";
                if (account.isReadyForPromotion()) {
                    levelUpMessage = "You are ready for a promotion! Type `/job promote` to start the promotion minigame!";
                }

                event.getHook().editOriginal("✅ You worked and earned %s%d! %s"
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
            case "promote" -> {
                if (!EconomyManager.hasJob(account)) {
                    event.getHook().editOriginal("❌ You must have a job to promote yourself!").queue();
                    return;
                }
                
                if (!account.isReadyForPromotion()) {
                    event.getHook().editOriginal("❌ You are not ready for a promotion!").queue();
                    return;
                }

                if (event.getChannelType().isThread()) {
                    event.getHook().editOriginal("❌ You cannot promote yourself inside a thread!").queue();
                    return;
                }

                startMinigame(event, account);
            }
            case null, default -> event.getHook().editOriginal("❌ You must specify a valid subcommand!").queue();
        }
    }

    private static String getResponse(GuildData config, User user, int amount) {
        return RESPONSES.get(ThreadLocalRandom.current().nextInt(RESPONSES.size()))
                .replace("<>", config.getEconomyCurrency()).replace("{user}", user.getAsMention())
                .replace("{amount}", String.valueOf(amount));
    }

    private static void startMinigame(SlashCommandInteractionEvent event, Economy account) {
        switch (account.getJob()) {
            case MATHEMATICIAN -> startMathChallenge(event, account);
            case PROGRAMMER -> startCodeGuesser(event, account);
//            case DOCTOR -> startBodyPartNamer(event, account);
//            case MUSICIAN -> startLyricGuesser(event, account);
//            case ARTIST -> startDrawingGame(event, account);
            case null, default -> {
                // TODO: Remove this when all jobs have minigames
                if(ThreadLocalRandom.current().nextBoolean())
                    startMathChallenge(event, account);
                else
                    startCodeGuesser(event, account);

                // event.getHook().editOriginal("⚠️ This job does not have a minigame yet!").queue();
            }
        }
    }

    private static void startCodeGuesser(SlashCommandInteractionEvent event, Economy account) {
        var code = Code.findCode();
        event.getHook()
                .editOriginal("✅ You have started the promotion minigame! You have 10 seconds to guess the programming language.")
                .flatMap(message -> message.createThreadChannel(event.getUser().getName() + "'s Promotion"))
                .queue(channel -> {
                    channel.addThreadMember(event.getUser()).queue();
                    channel.sendMessageFormat(
                                    "Guess the programming language of the following code to get promoted to the next job level!\n\n```\n%s```",
                                    code.code().substring(0, MathUtils.clamp(code.code().length(), 0, 1900)))
                            .queue(message -> TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                                    .condition(e -> e.getChannel().getIdLong() == channel.getIdLong()
                                            && e.getAuthor().getIdLong() == event.getUser().getIdLong()
                                            && e.getMessage().getContentRaw().equalsIgnoreCase(code.language().name()))
                                    .timeout(10, TimeUnit.SECONDS)
                                    .timeoutAction(() -> {
                                        channel.sendMessage("❌ You took too long to answer!")
                                                .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                        account.setReadyForPromotion(false);
                                        EconomyManager.updateAccount(account);
                                    })
                                    .success(messageEvent -> {
                                        channel.sendMessageFormat("✅ You have been promoted to level %d!",
                                                        account.getJobLevel() + 1)
                                                .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                        account.setJobLevel(account.getJobLevel() + 1);
                                        account.setReadyForPromotion(false);
                                        EconomyManager.updateAccount(account);
                                    }).build());
                });
    }

    private static void startMathChallenge(SlashCommandInteractionEvent event, Economy account) {
        var challenge = MathChallenge.generateMathChallenge();
        int time = challenge.numberOfOperations() * 10;
        event.getHook()
                .editOriginalFormat("✅ You have started the promotion minigame! You have %d seconds to answer the question!", time)
                .flatMap(message -> message.createThreadChannel(event.getUser().getName() + "'s Promotion"))
                .queue(channel -> {
                    channel.addThreadMember(event.getUser()).queue();
                    channel.sendMessageFormat(
                            "Solve the following math problem to get promoted to the next job level!\n\n%s",
                            challenge.question() + " = ?"
                    ).queue(message -> TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                            .condition(e -> e.getChannel().getIdLong() == channel.getIdLong()
                                    && e.getAuthor().getIdLong() == event.getUser().getIdLong()
                                    && StringUtils.isNumber(e.getMessage().getContentRaw()))
                            .timeout(time, TimeUnit.SECONDS)
                            .timeoutAction(() -> {
                                channel.sendMessage("❌ You took too long to answer!")
                                        .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                account.setReadyForPromotion(false);
                                EconomyManager.updateAccount(account);
                            })
                            .success(messageEvent -> {
                                double answer = Double.parseDouble(messageEvent.getMessage().getContentRaw());
                                if (answer == challenge.result()) {
                                    channel.sendMessageFormat("✅ You have been promoted to level %d!",
                                                    account.getJobLevel() + 1)
                                            .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                    account.setJobLevel(account.getJobLevel() + 1);
                                } else {
                                    channel.sendMessageFormat("❌ That is not the correct answer! The correct answer was %s!",
                                                    challenge.result())
                                            .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                }

                                account.setReadyForPromotion(false);
                                EconomyManager.updateAccount(account);
                            }).build());
                });
    }

    public record Code(String code, Language language) {
        public record Language(String name, String extension) {
        }

        public static Code findCode() throws IllegalStateException {
            Either<Code, HttpStatus> response = ApiHandler.findCode();
            if (response.isLeft())
                return response.getLeft();

            throw new IllegalStateException("Could not find code! Status code: " + response.getRight().getCode());
        }
    }

    public record MathChallenge(String question, double result, int numberOfOperations) {
        private static final char[] OPERATORS = {'+', '-'};
        private static final Random RANDOM = new Random();
        private static final Context CONTEXT = Context.newBuilder("js").allowAllAccess(true).build();

        public static MathChallenge generateMathChallenge() {
            String equation = generateRandomEquation().replace(".0", "");
            double result = evaluateEquationWithGraalVM(equation);
            return new MathChallenge(equation, result, equation.split(" ").length / 2);
        }

        public static String generateRandomEquation() {
            int numberOfOperations = RANDOM.nextInt(5) + 1; // Between 2 and 6 operations
            var equation = new StringBuilder(generateRandomOperand() + " ");

            for (int i = 0; i < numberOfOperations; i++) {
                char operator = generateRandomOperator();
                double operand = generateRandomOperand();
                equation.append(operator).append(" ").append(operand).append(" ");
            }

            return equation.toString().trim();
        }

        private static char generateRandomOperator() {
            return OPERATORS[RANDOM.nextInt(OPERATORS.length)];
        }

        private static double generateRandomOperand() {
            return RANDOM.nextInt(100) + 1; // Between 1 and 100
        }

        public static double evaluateEquationWithGraalVM(String equation) {
            Value value = CONTEXT.eval("js", equation);
            return value.asDouble();
        }
    }
}