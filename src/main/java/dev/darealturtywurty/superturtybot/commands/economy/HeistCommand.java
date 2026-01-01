package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import lombok.Getter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeistCommand extends EconomyCommand {
    private static final List<Fingerprint> FINGERPRINTS = new ArrayList<>();

    static {
        for (int printIndex = 101; printIndex < 111; printIndex++) {
            for (int fingerIndex = 0; fingerIndex < 8; fingerIndex++) {
                String name = printIndex + "_" + fingerIndex + ".tif";
                URL path = TurtyBot.getResource("/fingerprints/" + name);
                URL topLeft = TurtyBot.getResource("/fingerprints/top_left_" + name);
                URL topRight = TurtyBot.getResource("/fingerprints/top_right_" + name);
                URL bottomLeft = TurtyBot.getResource("/fingerprints/bottom_left_" + name);
                URL bottomRight = TurtyBot.getResource("/fingerprints/bottom_right_" + name);

                if (path == null || topLeft == null || topRight == null || bottomLeft == null || bottomRight == null)
                    continue;

                FINGERPRINTS.add(new Fingerprint(path, topLeft, topRight, bottomLeft, bottomRight, printIndex, fingerIndex));
            }
        }

        Constants.LOGGER.info("Loaded {} fingerprints!", FINGERPRINTS.size());
    }

    @Override
    public String getDescription() {
        return "Start a heist!";
    }

    @Override
    public String getName() {
        return "heist";
    }

    @Override
    public String getRichName() {
        return "Heist";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 2L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Member member = event.getMember();
        if (member == null) {
            event.getHook().editOriginal("❌ You must be in a server to use this command!").queue();
            return;
        }

        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if(account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot commit crimes! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        int crimeLevel = Math.max(1, account.getCrimeLevel());
        long totalHeists = account.getTotalHeists();
        if (totalHeists >= crimeLevel) {
            event.getHook().editOriginalFormat(
                    "❌ You have already completed %d/%d heists for your current crime level.",
                    totalHeists, crimeLevel).queue();
            return;
        }

        if (account.getNextHeist() > System.currentTimeMillis()) {
            event.getHook().editOriginalFormat("❌ You can start another heist %s!",
                    TimeFormat.RELATIVE.format(account.getNextHeist())).queue();
            return;
        }

        BigInteger balance = EconomyManager.getBalance(account);
        BigInteger setupCost = BigInteger.valueOf(EconomyManager.determineHeistSetupCost(account));
        if (balance.compareTo(setupCost) < 0) {
            event.getHook().editOriginalFormat("❌ You need another %s to start a heist!",
                    StringUtils.numberFormat(setupCost.subtract(balance), config)
            ).queue();
            return;
        }

        event.getHook().editOriginalFormat("❓ Would you like to start a heist? The setup cost is %s.",
                        StringUtils.numberFormat(setupCost, config))
                .setComponents(ActionRow.of(Button.success("heist:yes", "Yes"), Button.danger("heist:no", "No")))
                .queue(message -> createHeistSetupWaiter(guild, member, message, config, account).build());
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createHeistSetupWaiter(Guild guild, Member member, Message message, GuildData config, Economy account) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .timeout(2, TimeUnit.MINUTES)
                .timeoutAction(() -> message.editMessage("❌ Heist setup has timed out!").setComponents().queue())
                .failure(() -> message.editMessage("❌ An error occurred while setting up the heist!").setComponents().queue())
                .condition(event -> event.isFromGuild() &&
                        Objects.requireNonNull(event.getGuild()).getIdLong() == guild.getIdLong() &&
                        Objects.requireNonNull(event.getMember()).getIdLong() == member.getIdLong() &&
                        event.getChannel().getIdLong() == message.getChannel().getIdLong() &&
                        event.getMessageIdLong() == message.getIdLong() &&
                        event.getComponentId().startsWith("heist:"))
                .success(event ->
                        handleInitialButtonPressed(guild, member, message, config, account, event));
    }

    private static void handleInitialButtonPressed(Guild guild, Member member, Message message, GuildData config, Economy account, ButtonInteractionEvent event) {
        event.deferEdit().queue();

        if (event.getComponentId().equals("heist:no")) {
            message.editMessage("❌ Heist setup has been cancelled!").setComponents().queue();
            return;
        }

        BigInteger balance = EconomyManager.getBalance(account);
        BigInteger setupCost = BigInteger.valueOf(EconomyManager.determineHeistSetupCost(account));
        if (balance.compareTo(setupCost) < 0) {
            message.editMessageFormat("❌ You need another %s to start a heist!",
                    StringUtils.numberFormat(setupCost.subtract(balance), config)
            ).setComponents().queue();
            return;
        }

        EconomyManager.removeMoney(account, setupCost, true);
        account.addTransaction(setupCost.negate(), MoneyTransaction.HEIST_SETUP);

        if (!Environment.INSTANCE.isDevelopment()) {
            account.setNextHeist(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
        }

        EconomyManager.updateAccount(account);

        message.editMessage("✅ Heist started!")
                .setComponents()
                .flatMap(msg -> msg.createThreadChannel(Objects.requireNonNull(event.getMember()).getEffectiveName() + "'s Heist"))
                .queue(thread ->
                        startGame(guild, member, config, account, event, thread));
    }

    private static void startGame(Guild guild, Member member, GuildData config, Economy account, ButtonInteractionEvent event, ThreadChannel thread) {
        int totalStages = determineHeistStages(account.getHeistLevel());
        int gridColumns = determineHeistGridColumns(account.getHeistLevel());
        int gridRows = 4;
        long stageDurationMillis = determineHeistStageDurationMillis(account.getHeistLevel());
        HeistStage stage = generateHeistStage(gridColumns, gridRows);
        try (FileUpload upload = createUpload(stage.matcher())) {
            thread.sendMessageFormat("🔍 **Fingerprint Matcher** (Stage 1/%d) %s", totalStages, event.getUser().getAsMention())
                    .setFiles(upload)
                    .setComponents(createHeistButtons(null, gridColumns, gridRows))
                    .queue(msg -> {
                        var heist = new Heist(guild.getIdLong(), event.getUser().getIdLong(), thread.getIdLong(),
                                msg.getIdLong(), totalStages, gridColumns, gridRows, stageDurationMillis);
                        heist.setStageData(stage.fingerprint(), stage.positions(), stage.quadrants());
                        registerHeistWaiters(guild, thread, member, msg, heist, config, account);

                        String inputHint = gridColumns > 5
                                ? "Type the quadrant numbers (e.g., `1 3 5 7`) to choose."
                                : "Use the buttons or type the quadrant numbers (e.g., `1 3 5 7`) to choose.";
                        msg.replyFormat("%s%n%nRemember, there are 4 matching quadrants! Stage 1/%d ends %s.",
                                        inputHint,
                                        totalStages,
                                        TimeFormat.RELATIVE.format(heist.getStageStartTime() + heist.getStageDurationMillis()))
                                .queue();
                    });
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to send fingerprint matcher!", exception);
        }
    }

    private static FileUpload createUpload(BufferedImage image) {
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to write fingerprint matcher!", exception);
        }

        return FileUpload.fromData(baos.toByteArray(), "fingerprint_matcher.png");
    }

    private static List<ActionRow> createHeistButtons(@Nullable Heist heist) {
        if (heist == null) {
            return createHeistButtons(null, 2, 4);
        }

        return createHeistButtons(heist, heist.getGridColumns(), heist.getGridRows());
    }

    private static List<ActionRow> createHeistButtons(@Nullable Heist heist, int gridColumns, int gridRows) {
        if (gridColumns > 5) {
            return List.of(ActionRow.of(Button.success("heist:confirm", "Confirm")));
        }

        int totalTiles = gridColumns * gridRows;
        List<Button> buttons = new ArrayList<>();
        for (int index = 1; index <= totalTiles; index++) {
            var button = Button.primary("heist:" + index, String.valueOf(index));
            if (heist != null && heist.isQuadrantSelected(index - 1)) {
                button = button.withStyle(ButtonStyle.SUCCESS);
            }

            buttons.add(button);
        }

        List<ActionRow> rows = new ArrayList<>();
        for (int row = 0; row < gridRows; row++) {
            int start = row * gridColumns;
            int end = start + gridColumns;
            rows.add(ActionRow.of(buttons.subList(start, end)));
        }

        rows.add(ActionRow.of(Button.success("heist:confirm", "Confirm")));
        return rows;
    }

    private static void registerHeistWaiters(Guild guild, ThreadChannel thread, Member member, Message message, Heist heist, GuildData config, Economy account) {
        long waiterToken = heist.incrementWaiterToken();
        createHeistWaiter(guild, thread, member, message, heist, config, account, waiterToken).build();
        createHeistMessageWaiter(guild, thread, member, message, heist, config, account, waiterToken).build();
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createHeistWaiter(Guild guild, ThreadChannel thread, Member member, Message message, Heist heist, GuildData config, Economy account, long waiterToken) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(event -> event.isFromGuild() &&
                        Objects.requireNonNull(event.getGuild()).getIdLong() == guild.getIdLong() &&
                        Objects.requireNonNull(event.getMember()).getIdLong() == member.getIdLong() &&
                        event.getChannel().getIdLong() == message.getChannel().getIdLong() &&
                        event.getMessageIdLong() == message.getIdLong() &&
                        event.getComponentId().startsWith("heist:") &&
                        heist.getWaiterToken() == waiterToken)
                .timeout(heist.getStageDurationMillis(), TimeUnit.MILLISECONDS)
                .timeoutAction(() -> {
                    if (heist.getWaiterToken() != waiterToken)
                        return;

                    message.editMessage("❌ Heist has timed out!")
                            .queue(ignored -> close(thread));
                })
                .failure(() -> {
                    if (heist.getWaiterToken() != waiterToken)
                        return;

                    message.editMessage("❌ An error occurred while processing the heist!")
                            .queue(ignored -> close(thread));
                })
                .success(event ->
                        onHeistButtonPressed(guild, thread, member, message, heist, config, account, event));
    }

    private static EventWaiter.Builder<MessageReceivedEvent> createHeistMessageWaiter(Guild guild, ThreadChannel thread, Member member, Message message, Heist heist, GuildData config, Economy account, long waiterToken) {
        return TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                .condition(event -> event.isFromGuild() &&
                        event.isFromThread() &&
                        Objects.requireNonNull(event.getGuild()).getIdLong() == guild.getIdLong() &&
                        Objects.requireNonNull(event.getMember()).getIdLong() == member.getIdLong() &&
                        event.getChannel().getIdLong() == message.getChannel().getIdLong() &&
                        heist.getWaiterToken() == waiterToken &&
                        event.getMessage().getContentRaw().matches(".*\\d.*"))
                .timeout(heist.getStageDurationMillis(), TimeUnit.MILLISECONDS)
                .timeoutAction(() -> {
                    if (heist.getWaiterToken() != waiterToken)
                        return;

                    message.editMessage("❌ Heist has timed out!")
                            .queue(ignored -> close(thread));
                })
                .failure(() -> {
                    if (heist.getWaiterToken() != waiterToken)
                        return;

                    message.editMessage("❌ An error occurred while processing the heist!")
                            .queue(ignored -> close(thread));
                })
                .success(event ->
                        onHeistMessageReceived(guild, thread, member, message, heist, config, account, waiterToken, event));
    }

    private static void onHeistButtonPressed(Guild guild, ThreadChannel thread, Member member, Message message, Heist heist, GuildData config, Economy account, ButtonInteractionEvent event) {
        event.deferEdit().queue();

        if (event.getComponentId().equals("heist:confirm")) {
            heist.incrementWaiterToken();

            if (heist.isHeistComplete()) {
                heist.completeCurrentStage();
                if (heist.hasNextStage()) {
                    heist.advanceStage();
                    HeistStage stage = generateHeistStage(heist.getGridColumns(), heist.getGridRows());
                    heist.setStageData(stage.fingerprint(), stage.positions(), stage.quadrants());
                    heist.startStage();
                    message.editMessage(message.getContentRaw())
                            .setComponents(createHeistButtons(heist).stream()
                                    .map(row -> ActionRow.of(row.getComponents().stream()
                                            .filter(Button.class::isInstance)
                                            .map(Button.class::cast)
                                            .map(Button::asDisabled)
                                            .toList()))
                                    .toList())
                            .queue();
                    try (FileUpload upload = createUpload(stage.matcher())) {
                        thread.sendMessageFormat("🔍 **Fingerprint Matcher** (Stage %d/%d) %s",
                                        heist.getCurrentStage(), heist.getTotalStages(), member.getAsMention())
                                .setFiles(upload)
                                .setComponents(createHeistButtons(heist))
                                .queue(newMessage -> {
                                    thread.sendMessageFormat("Stage %d/%d ends %s.",
                                                    heist.getCurrentStage(),
                                                    heist.getTotalStages(),
                                                    TimeFormat.RELATIVE.format(heist.getStageStartTime() + heist.getStageDurationMillis()))
                                            .queue(ignoredMessage -> registerHeistWaiters(guild, thread, member, newMessage, heist, config, account));
                                });
                    } catch (IOException exception) {
                        Constants.LOGGER.error("Failed to send fingerprint matcher!", exception);
                        heist.incrementWaiterToken();
                        thread.sendMessage("❌ **An error occurred while processing the heist!**").queue(
                                ignored -> close(thread));
                    }
                    return;
                }

                EconomyManager.HeistResult heistResult = EconomyManager.heistCompleted(account, heist.getAverageStageTime());
                EconomyManager.updateAccount(account);
                thread.sendMessage("✅ **Heist successful!** You have earned %s!%n%n%s".formatted(
                                StringUtils.numberFormat(BigInteger.valueOf(heistResult.earned()), config),
                                heistResult.leveledUp() ? "🎉 You have levelled up! You are now level %d!".formatted(account.getHeistLevel() + 1) : "").trim())
                        .queue(ignored -> close(thread));
            } else {
                event.getHook().editOriginal("❌ Failed to complete the heist!")
                        .setComponents(createHeistButtons(heist).stream()
                                .map(row -> ActionRow.of(row.getComponents().stream()
                                        .filter(Button.class::isInstance)
                                        .map(Button.class::cast)
                                        .map(Button::asDisabled)
                                        .toList()))
                                .toList())
                        .queue(ignored -> {
                            boolean accountWiped = EconomyManager.heistFailed(account);
                            if (accountWiped) {
                                EconomyManager.updateAccount(account);
                                thread.sendMessage("💥 **Disaster!** A freak raid wiped your entire account to 0. You're clearly having an unlucky day, this was only a 0.001% chance 😭!")
                                        .queue(ignored_ -> close(thread));
                            } else {
                                thread.sendMessage("❌ **Heist failed!**")
                                        .queue(ignored_ -> close(thread));
                            }
                        });
            }

            return;
        }

        int quadrant = Integer.parseInt(event.getComponentId().split(":")[1]);
        if (quadrant < 1 || quadrant > heist.getTotalTiles()) {
            event.getHook().editOriginal("❌ Invalid quadrant!").queue();
            registerHeistWaiters(guild, thread, member, message, heist, config, account);
            return;
        }

        if (event.getComponent().getStyle() == ButtonStyle.SUCCESS) {
            heist.deselectQuadrant(quadrant - 1);
        } else {
            heist.selectQuadrant(quadrant - 1);
        }

        try (FileUpload upload = createUpload(createFingerprintMatcher(heist.fingerprint, heist, new ArrayList<>(), heist.getQuadrants(),
                heist.getGridColumns(), heist.getGridRows()))) {
            event.getHook().editOriginalFormat("🔍 **Fingerprint Matcher** %s", member.getAsMention())
                    .setFiles(upload)
                    .setComponents(createHeistButtons(heist))
                    .queue(ignored -> registerHeistWaiters(guild, thread, member, message, heist, config, account));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to send fingerprint matcher!", exception);
            heist.incrementWaiterToken();
            thread.sendMessage("❌ **An error occurred while processing the heist!**").queue(
                    ignored -> close(thread));
        }
    }

    private static void onHeistMessageReceived(Guild guild, ThreadChannel thread, Member member, Message message, Heist heist, GuildData config, Economy account, long waiterToken, MessageReceivedEvent event) {
        if (heist.getWaiterToken() != waiterToken)
            return;

        QuadrantParseResult parseResult = parseQuadrantInput(event.getMessage().getContentRaw(), heist.getTotalTiles());
        if (parseResult.quadrants().isEmpty()) {
            if (parseResult.invalidDigits()) {
                thread.sendMessageFormat("❌ Please use numbers between 1 and %d to choose quadrants.",
                                heist.getTotalTiles())
                        .queue(ignored -> registerHeistWaiters(guild, thread, member, message, heist, config, account));
                return;
            }

            registerHeistWaiters(guild, thread, member, message, heist, config, account);
            return;
        }

        if (parseResult.invalidDigits()) {
            thread.sendMessageFormat("❌ Only numbers between 1 and %d are valid quadrants.",
                            heist.getTotalTiles())
                    .queue(ignored -> registerHeistWaiters(guild, thread, member, message, heist, config, account));
            return;
        }

        heist.getSelectedQuadrants().clear();
        parseResult.quadrants().forEach(quadrant -> heist.selectQuadrant(quadrant - 1));

        try (FileUpload upload = createUpload(createFingerprintMatcher(heist.fingerprint, heist, new ArrayList<>(), heist.getQuadrants(),
                heist.getGridColumns(), heist.getGridRows()))) {
            message.editMessageFormat("🔍 **Fingerprint Matcher** %s", member.getAsMention())
                    .setFiles(upload)
                    .setComponents(createHeistButtons(heist))
                    .queue(ignored -> registerHeistWaiters(guild, thread, member, message, heist, config, account));
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to send fingerprint matcher!", exception);
            heist.incrementWaiterToken();
            thread.sendMessage("❌ **An error occurred while processing the heist!**").queue(
                    ignored -> close(thread));
        }
    }

    private static void close(ThreadChannel thread) {
        thread.getManager().setArchived(true).setLocked(true).queue();
    }

    private static int determineHeistStages(int heistLevel) {
        int stages = 1;
        if (heistLevel > 10) stages++;
        if (heistLevel > 25) stages++;
        if (heistLevel > 50) stages++;
        if (heistLevel > 75) stages++;
        if (heistLevel > 100) stages++;
        return Math.min(stages, 6);
    }

    private static long determineHeistStageDurationMillis(int heistLevel) {
        int stages = determineHeistStages(heistLevel);
        return TimeUnit.MINUTES.toMillis(1) + TimeUnit.SECONDS.toMillis(10L * (stages - 1));
    }

    private static int determineHeistGridColumns(int heistLevel) {
        int columns = 2;
        if (heistLevel > 10) columns++;
        if (heistLevel > 25) columns++;
        if (heistLevel > 50) columns++;
        if (heistLevel > 75) columns++;
        if (heistLevel > 100) columns++;
        return Math.min(columns, 6);
    }

    private static HeistStage generateHeistStage(int gridColumns, int gridRows) {
        Fingerprint fingerprint = FINGERPRINTS.get(ThreadLocalRandom.current().nextInt(FINGERPRINTS.size()));
        List<Integer> positions = new ArrayList<>();
        List<Quadrant> quadrants = new ArrayList<>();
        BufferedImage matcher = createFingerprintMatcher(fingerprint, null, positions, quadrants, gridColumns, gridRows);
        return new HeistStage(fingerprint, positions, quadrants, matcher);
    }

    // Fingerprints are 448x478
    private static BufferedImage createFingerprintMatcher(Fingerprint fingerprint, @Nullable Heist heist, List<Integer> outPositions,
                                                          List<Quadrant> inOutQuadrants, int gridColumns, int gridRows) {
        int tileSize = 250;
        int targetSize = 500;
        int gridWidth = gridColumns * tileSize;
        int gridHeight = gridRows * tileSize;
        int imageWidth = gridWidth + targetSize;
        int imageHeight = Math.max(gridHeight, targetSize);
        int targetX = gridWidth;
        int targetY = (imageHeight - targetSize) / 2;
        var image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        // Draw target fingerprint on the right
        URL targetUrl = fingerprint.path();
        try (InputStream targetStream = targetUrl.openStream()) {
            BufferedImage target = ImageIO.read(targetStream);
            graphics.drawImage(target, targetX, targetY, targetSize, targetSize, null);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read target image!", exception);
        }

        if (inOutQuadrants.isEmpty()) {
            // Must include at least 3 matching quadrants
            List<URL> quadrants = new ArrayList<>();
            int totalTiles = gridColumns * gridRows;

            // pick 4 random quadrants from the fingerprint
            int printIndex = fingerprint.printIndex();
            int fingerIndex = fingerprint.fingerIndex();
            for (int i = 0; i < 4; i++) {
                URL quadrantUrl = fingerprint.pickRandomNoDupe(quadrants);
                quadrants.add(quadrantUrl);

                var quadrant = new Quadrant(printIndex, fingerIndex, fingerprint.getIndexByUrl(quadrantUrl));
                inOutQuadrants.add(quadrant);
            }

            while (quadrants.size() < totalTiles) {
                // pick a random quadrant from a random fingerprint
                Fingerprint randomFingerprint = FINGERPRINTS.get(ThreadLocalRandom.current().nextInt(FINGERPRINTS.size()));
                if (randomFingerprint.printIndex() == printIndex && randomFingerprint.fingerIndex() == fingerIndex)
                    continue;

                URL quadrantUrl = randomFingerprint.pickRandomNoDupe(quadrants);
                quadrants.add(quadrantUrl);

                var quadrant = new Quadrant(randomFingerprint.printIndex(), randomFingerprint.fingerIndex(), randomFingerprint.getIndexByUrl(quadrantUrl));
                inOutQuadrants.add(quadrant);
            }

            long seed = System.currentTimeMillis();
            var random1 = new Random(seed);
            var random2 = new Random(seed);
            Collections.shuffle(quadrants, random1);
            Collections.shuffle(inOutQuadrants, random2);

            for (int index = 0; index < inOutQuadrants.size(); index++) {
                Quadrant quadrant = inOutQuadrants.get(index);
                if (quadrant.printIndex() == fingerprint.printIndex() && quadrant.fingerIndex() == fingerprint.fingerIndex()) {
                    outPositions.add(index);
                }
            }
        }

        // Grid of quadrants
        for (int i = 0; i < gridColumns * gridRows; i++) {
            Quadrant quadrant = inOutQuadrants.get(i);
            URL quadrantUrl = quadrant.getUrl();
            if (quadrantUrl == null) {
                Constants.LOGGER.error("Quadrant URL is null! Quadrant: {}", quadrant);
                continue;
            }

            try (InputStream quadrantStream = quadrantUrl.openStream()) {
                BufferedImage quadrantImg = ImageIO.read(quadrantStream);
                graphics.drawImage(quadrantImg, i % gridColumns * tileSize, i / gridColumns * tileSize, tileSize, tileSize, null);
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to read quadrant image!", exception);
            }
        }

        // draw border around selected quadrants
        if (heist != null) {
            graphics.setColor(Color.GREEN);
            graphics.setStroke(new BasicStroke(10));
            for (int i = 0; i < gridColumns * gridRows; i++) {
                if (heist.isQuadrantSelected(i)) {
                    graphics.drawRect(i % gridColumns * tileSize, i / gridColumns * tileSize, tileSize, tileSize);
                }
            }
        }

        graphics.dispose();
        return image;
    }

    private static QuadrantParseResult parseQuadrantInput(String input, int maxQuadrant) {
        Matcher matcher = Pattern.compile("\\d+").matcher(input);
        Set<Integer> quadrants = new LinkedHashSet<>();
        boolean invalidDigits = false;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group());
            if (value >= 1 && value <= maxQuadrant) {
                quadrants.add(value);
            } else {
                invalidDigits = true;
            }
        }

        return new QuadrantParseResult(quadrants, invalidDigits);
    }

    public record Fingerprint(URL path, URL topLeft, URL topRight, URL bottomLeft, URL bottomRight, int printIndex,
                              int fingerIndex) {
        public URL pickRandomNoDupe(List<URL> current) {
            URL url = switch (ThreadLocalRandom.current().nextInt(4)) {
                case 0 -> topLeft;
                case 1 -> topRight;
                case 2 -> bottomLeft;
                case 3 -> bottomRight;
                default -> null;
            };

            if (url == null || current.contains(url)) {
                return pickRandomNoDupe(current);
            }

            return url;
        }

        public int getIndexByUrl(URL quadrant) {
            if (topLeft.toString().equals(quadrant.toString())) return 1;
            if (topRight.toString().equals(quadrant.toString())) return 2;
            if (bottomLeft.toString().equals(quadrant.toString())) return 3;
            if (bottomRight.toString().equals(quadrant.toString())) return 4;
            return -1;
        }

        @Deprecated
        public boolean isFromThis(URL url) {
            return path.toString().equals(url.toString()) || topLeft.toString().equals(url.toString())
                    || topRight.toString().equals(url.toString()) || bottomLeft.toString().equals(url.toString())
                    || bottomRight.toString().equals(url.toString());
        }
    }

    public record Quadrant(int printIndex, int fingerIndex, int quadrant) {
        public URL getUrl() {
            String name = printIndex + "_" + fingerIndex + ".tif";
            return switch (quadrant) {
                case 1 -> TurtyBot.getResource("/fingerprints/top_left_" + name);
                case 2 -> TurtyBot.getResource("/fingerprints/top_right_" + name);
                case 3 -> TurtyBot.getResource("/fingerprints/bottom_left_" + name);
                case 4 -> TurtyBot.getResource("/fingerprints/bottom_right_" + name);
                default -> null;
            };
        }
    }

    @Getter
    public static class Heist {
        private final long guildId, userId, channelId, messageId;
        private final long startTime;
        private final int totalStages;
        private final int gridColumns;
        private final int gridRows;
        private final long stageDurationMillis;
        private final Set<Integer> fingerprintPositions = new HashSet<>();
        private final Set<Integer> selectedQuadrants = new HashSet<>();
        private final List<Quadrant> quadrants = new ArrayList<>();
        private Fingerprint fingerprint;
        private int currentStage;
        private long stageStartTime;
        private long totalStageTime;
        private int completedStages;
        private long waiterToken;

        public Heist(long guildId, long userId, long channelId, long messageId, int totalStages, int gridColumns, int gridRows,
                     long stageDurationMillis) {
            this.guildId = guildId;
            this.userId = userId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.startTime = System.currentTimeMillis();
            this.totalStages = totalStages;
            this.gridColumns = gridColumns;
            this.gridRows = gridRows;
            this.stageDurationMillis = stageDurationMillis;
            this.currentStage = 1;
            this.stageStartTime = this.startTime;
        }

        public void setStageData(Fingerprint fingerprint, Collection<Integer> positions, Collection<Quadrant> quadrants) {
            this.fingerprint = fingerprint;
            this.fingerprintPositions.clear();
            this.fingerprintPositions.addAll(positions);
            this.selectedQuadrants.clear();
            this.quadrants.clear();
            this.quadrants.addAll(quadrants);
        }

        public boolean hasNextStage() {
            return this.currentStage < this.totalStages;
        }

        public void advanceStage() {
            if (hasNextStage()) {
                this.currentStage++;
            }
        }

        public void startStage() {
            this.stageStartTime = System.currentTimeMillis();
        }

        public void completeCurrentStage() {
            long duration = System.currentTimeMillis() - this.stageStartTime;
            this.totalStageTime += duration;
            this.completedStages++;
        }

        public long getAverageStageTime() {
            if (this.completedStages == 0) {
                return 0L;
            }

            return this.totalStageTime / this.completedStages;
        }

        public int getTotalTiles() {
            return this.gridColumns * this.gridRows;
        }

        public long incrementWaiterToken() {
            return ++this.waiterToken;
        }

        public void selectQuadrant(int quadrant) {
            this.selectedQuadrants.add(quadrant);
        }

        public void deselectQuadrant(int quadrant) {
            this.selectedQuadrants.remove(quadrant);
        }

        public boolean isQuadrantSelected(int quadrant) {
            return this.selectedQuadrants.contains(quadrant);
        }

        public boolean isHeistComplete() {
            return this.fingerprintPositions.size() == this.selectedQuadrants.size() &&
                    this.fingerprintPositions.containsAll(this.selectedQuadrants);
        }
    }

    private record QuadrantParseResult(Set<Integer> quadrants, boolean invalidDigits) {
    }

    private record HeistStage(Fingerprint fingerprint, List<Integer> positions, List<Quadrant> quadrants,
                              BufferedImage matcher) {
    }
}
