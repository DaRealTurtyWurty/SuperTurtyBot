package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
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
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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

        System.out.println("Loaded " + FINGERPRINTS.size() + " fingerprints!");
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        Member member = event.getMember();
        if (member == null) {
            event.getHook().editOriginal("❌ You must be in a server to use this command!").queue();
            return;
        }

        final Economy account = EconomyManager.getOrCreateAccount(guild, event.getUser());
        if (account.getNextHeist() > System.currentTimeMillis()) {
            event.getHook().editOriginalFormat("❌ You can start another heist %s!",
                    TimeFormat.RELATIVE.format(account.getNextHeist())).queue();
            return;
        }

        long balance = EconomyManager.getBalance(account);
        long setupCost = EconomyManager.determineHeistSetupCost(account);
        if (balance < setupCost) {
            event.getHook().editOriginalFormat("❌ You need another %s%s to start a heist!",
                    config.getEconomyCurrency(),
                    StringUtils.numberFormat(setupCost - balance)
            ).queue();
            return;
        }

        event.getHook().editOriginalFormat("❓ Would you like to start a heist? The setup cost is %s%s.",
                        config.getEconomyCurrency(), StringUtils.numberFormat(EconomyManager.determineHeistSetupCost(account)))
                .setActionRow(Button.success("heist:yes", "Yes"), Button.danger("heist:no", "No"))
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
                .success(event -> {
                    event.deferEdit().queue();

                    if (event.getComponentId().equals("heist:no")) {
                        message.editMessage("❌ Heist setup has been cancelled!").setComponents().queue();
                        return;
                    }

                    long balance = EconomyManager.getBalance(account);
                    long setupCost = EconomyManager.determineHeistSetupCost(account);
                    if (balance < setupCost) {
                        message.editMessageFormat("❌ You need another %s%s to start a heist!",
                                config.getEconomyCurrency(),
                                StringUtils.numberFormat(setupCost - balance)
                        ).setComponents().queue();
                        return;
                    }

                    EconomyManager.removeMoney(account, -EconomyManager.determineHeistSetupCost(account), true);

                    if (!Environment.INSTANCE.isDevelopment()) {
                        account.setNextHeist(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));
                    }

                    EconomyManager.updateAccount(account);

                    message.editMessage("✅ Heist started!")
                            .setComponents()
                            .flatMap(msg -> msg.createThreadChannel(Objects.requireNonNull(event.getMember()).getEffectiveName() + "'s Heist"))
                            .queue(thread -> {
                                Fingerprint fingerprint = FINGERPRINTS.get(ThreadLocalRandom.current().nextInt(FINGERPRINTS.size()));
                                List<Integer> positions = new ArrayList<>();
                                List<Quadrant> quadrants = new ArrayList<>();
                                BufferedImage matcher = createFingerprintMatcher(fingerprint, null, positions, quadrants);
                                try (FileUpload upload = createUpload(matcher)) {
                                    thread.sendMessageFormat("🔍 **Fingerprint Matcher** %s", event.getUser().getAsMention())
                                            .setFiles(upload)
                                            .setComponents(createHeistButtons(null))
                                            .queue(msg -> {
                                                var heist = new Heist(guild.getIdLong(), event.getUser().getIdLong(), thread.getIdLong(),
                                                        msg.getIdLong(), fingerprint, positions);
                                                heist.getQuadrants().addAll(quadrants);
                                                createHeistWaiter(guild, thread, member, msg, heist, config, account).build();

                                                msg.replyFormat("Remember, there are 4 matching quadrants!\n\nThe heist ends %s.",
                                                                TimeFormat.RELATIVE.format(heist.startTime + TimeUnit.MINUTES.toMillis(1)))
                                                        .queue();
                                            });
                                } catch (IOException exception) {
                                    Constants.LOGGER.error("Failed to send fingerprint matcher!", exception);
                                }
                            });
                });
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
            List<Button> buttons = new ArrayList<>();
            for (int index = 1; index <= 8; index++) {
                buttons.add(Button.primary("heist:" + index, String.valueOf(index)));
            }

            return List.of(
                    ActionRow.of(buttons.subList(0, 2)),
                    ActionRow.of(buttons.subList(2, 4)),
                    ActionRow.of(buttons.subList(4, 6)),
                    ActionRow.of(buttons.subList(6, 8)),
                    ActionRow.of(Button.success("heist:confirm", "Confirm"))
            );
        }

        List<Button> buttons = new ArrayList<>();
        for (int index = 1; index <= 8; index++) {
            var button = Button.primary("heist:" + index, String.valueOf(index));
            if (heist.isQuadrantSelected(index - 1)) {
                button = button.withStyle(ButtonStyle.SUCCESS);
            }

            buttons.add(button);
        }

        return List.of(
                ActionRow.of(buttons.subList(0, 2)),
                ActionRow.of(buttons.subList(2, 4)),
                ActionRow.of(buttons.subList(4, 6)),
                ActionRow.of(buttons.subList(6, 8)),
                ActionRow.of(Button.success("heist:confirm", "Confirm"))
        );
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createHeistWaiter(Guild guild, ThreadChannel thread, Member member, Message message, Heist heist, GuildData config, Economy account) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(event -> event.isFromGuild() &&
                        Objects.requireNonNull(event.getGuild()).getIdLong() == guild.getIdLong() &&
                        Objects.requireNonNull(event.getMember()).getIdLong() == member.getIdLong() &&
                        event.getChannel().getIdLong() == message.getChannel().getIdLong() &&
                        event.getMessageIdLong() == message.getIdLong() &&
                        event.getComponentId().startsWith("heist:"))
                .timeout(1, TimeUnit.MINUTES)
                .timeoutAction(() -> message.editMessage("❌ Heist has timed out!")
                        .queue(ignored -> close(thread)))
                .failure(() -> message.editMessage("❌ An error occurred while processing the heist!")
                        .queue(ignored -> close(thread)))
                .success(event -> {
                    event.deferEdit().queue();

                    if (event.getComponentId().equals("heist:confirm")) {
                        if (heist.isHeistComplete()) {
                            Pair<Long, Boolean> heistResult = EconomyManager.heistCompleted(account, System.currentTimeMillis() - heist.startTime);
                            EconomyManager.updateAccount(account);
                            thread.sendMessage("✅ **Heist successful!** You have earned %s%s!%n%n%s".formatted(
                                            config.getEconomyCurrency(),
                                            StringUtils.numberFormat(heistResult.getLeft()),
                                            heistResult.getRight() ? "🎉 You have levelled up! You are now level %d!".formatted(account.getHeistLevel()) : "").trim())
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
                                    .queue(ignored -> thread.sendMessage("❌ **Heist failed!**")
                                            .queue(ignored_ -> close(thread)));
                        }

                        return;
                    }

                    int quadrant = Integer.parseInt(event.getComponentId().split(":")[1]);
                    if (quadrant < 1 || quadrant > 8) {
                        event.getHook().editOriginal("❌ Invalid quadrant!").queue();
                        return;
                    }

                    if (event.getComponent().getStyle() == ButtonStyle.SUCCESS) {
                        heist.deselectQuadrant(quadrant - 1);
                    } else {
                        heist.selectQuadrant(quadrant - 1);
                    }

                    try (FileUpload upload = createUpload(createFingerprintMatcher(heist.fingerprint, heist, new ArrayList<>(), heist.getQuadrants()))) {
                        event.getHook().editOriginalFormat("🔍 **Fingerprint Matcher** %s", member.getAsMention())
                                .setFiles(upload)
                                .setComponents(createHeistButtons(heist))
                                .queue(ignored -> createHeistWaiter(guild, thread, member, message, heist, config, account).build());
                    } catch (IOException exception) {
                        Constants.LOGGER.error("Failed to send fingerprint matcher!", exception);
                        thread.sendMessage("❌ **An error occurred while processing the heist!**").queue(
                                ignored -> thread.getManager().setArchived(true).setLocked(true).queue());
                    }
                });
    }

    private static void close(ThreadChannel thread) {
        thread.getManager().setArchived(true).setLocked(true).queue();
    }

    // Fingerprints are 448x478
    private static BufferedImage createFingerprintMatcher(Fingerprint fingerprint, @Nullable Heist heist, List<Integer> outPositions, List<Quadrant> inOutQuadrants) {
        var image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 1000, 1000);

        // Draw target fingerprint on the right
        URL targetUrl = fingerprint.path();
        try (InputStream targetStream = targetUrl.openStream()) {
            BufferedImage target = ImageIO.read(targetStream);
            graphics.drawImage(target, 500, 250, 500, 500, null);
        } catch (IOException exception) {
            Constants.LOGGER.error("Failed to read target image!", exception);
        }

        if (inOutQuadrants.isEmpty()) {
            // Must include at least 3 matching quadrants
            List<URL> quadrants = new ArrayList<>();

            // pick 4 random quadrants from the fingerprint
            int printIndex = fingerprint.printIndex();
            int fingerIndex = fingerprint.fingerIndex();
            for (int i = 0; i < 4; i++) {
                URL quadrantUrl = fingerprint.pickRandomNoDupe(quadrants);
                quadrants.add(quadrantUrl);

                var quadrant = new Quadrant(printIndex, fingerIndex, fingerprint.getIndexByUrl(quadrantUrl));
                inOutQuadrants.add(quadrant);
            }

            while (quadrants.size() < 8) {
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

        // 2x4 grid
        for (int i = 0; i < 8; i++) {
            Quadrant quadrant = inOutQuadrants.get(i);
            URL quadrantUrl = quadrant.getUrl();
            if (quadrantUrl == null) {
                Constants.LOGGER.error("Quadrant URL is null! Quadrant: {}", quadrant);
                continue;
            }

            try (InputStream quadrantStream = quadrantUrl.openStream()) {
                BufferedImage quadrantImg = ImageIO.read(quadrantStream);
                graphics.drawImage(quadrantImg, i % 2 * 250, i / 2 * 250, 250, 250, null);
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to read quadrant image!", exception);
            }
        }

        // draw border around selected quadrants
        if (heist != null) {
            graphics.setColor(Color.GREEN);
            graphics.setStroke(new BasicStroke(10));
            for (int i = 0; i < 8; i++) {
                if (heist.isQuadrantSelected(i)) {
                    graphics.drawRect(i % 2 * 250, i / 2 * 250, 250, 250);
                }
            }
        }

        graphics.dispose();
        return image;
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
        private final Fingerprint fingerprint;
        private final Set<Integer> fingerprintPositions = new HashSet<>();
        private final Set<Integer> selectedQuadrants = new HashSet<>();
        private final List<Quadrant> quadrants = new ArrayList<>();

        public Heist(long guildId, long userId, long channelId, long messageId, Fingerprint fingerprint, Collection<Integer> positions) {
            this.guildId = guildId;
            this.userId = userId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.startTime = System.currentTimeMillis();
            this.fingerprint = fingerprint;
            this.fingerprintPositions.addAll(positions);
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
}
