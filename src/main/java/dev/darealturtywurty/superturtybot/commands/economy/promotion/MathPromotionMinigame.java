package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MathPromotionMinigame implements PromotionMinigame {
    private static BufferedImage createMathChallengeImage(String question) {
        Font font = new Font("Arial", Font.PLAIN, 20);
        FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().getFontMetrics(font);
        int width = metrics.stringWidth(question + " = ") + 5;
        int height = metrics.getHeight() + 5;

        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        graphics.drawString(question + " = ", 5, height - 5);
        graphics.dispose();

        return image;
    }

    private static FileUpload createMathChallengeImageUpload(String question) {
        BufferedImage image = createMathChallengeImage(question);
        var stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write image!", exception);
        }

        return FileUpload.fromData(stream.toByteArray(), "math_challenge.png");
    }

    @Override
    public void start(SlashCommandInteractionEvent event, Economy account) {
        var challenge = MathChallenge.generateMathChallenge();
        int time = challenge.numberOfOperations() * 10;
        event.getHook()
                .editOriginalFormat("✅ You have started the promotion minigame! You have %d seconds to answer the question!", time)
                .flatMap(message -> message.createThreadChannel(event.getUser().getName() + "'s Promotion"))
                .queue(channel -> {
                    channel.addThreadMember(event.getUser()).queue();

                    channel.sendMessage("Solve the following math problem to get promoted to the next job level!")
                            .addFiles(createMathChallengeImageUpload(challenge.question()))
                            .queue(message -> TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
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
                                        int answer = Integer.parseInt(messageEvent.getMessage().getContentRaw());
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

    public record MathChallenge(String question, int result, int numberOfOperations) {
        private static final char[] OPERATORS = {'+', '-'};
        private static final Random RANDOM = new Random();
        private static final Context CONTEXT = Context.newBuilder("js").allowAllAccess(true).build();

        public static MathChallenge generateMathChallenge() {
            String equation = generateRandomEquation();
            int result = evaluateEquationWithGraalVM(equation);
            return new MathChallenge(equation, result, equation.split(" ").length / 2);
        }

        private static int evaluateEquationWithGraalVM(String equation) {
            Value result = CONTEXT.eval("js", equation);
            return result.asInt();
        }

        private static String generateRandomEquation() {
            int numOps = RANDOM.nextInt(1, 5);

            StringBuilder equation = new StringBuilder();
            for (int i = 0; i < numOps; i++) {
                int num = RANDOM.nextInt(10, 101);
                char operator = OPERATORS[RANDOM.nextInt(OPERATORS.length)];
                equation.append(num).append(" ").append(operator).append(" ");
            }

            int num = RANDOM.nextInt(10, 101);
            equation.append(num);

            return equation.toString();
        }
    }
}
