package dev.darealturtywurty.superturtybot.commands.util;

import com.google.common.collect.Lists;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.core.util.discord.BotUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserConfig;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LeaderboardCommand extends CoreCommand {
    private static final Color GOLD_COLOR = Color.decode("#ffd700");
    private static final Color SILVER_COLOR = Color.decode("#e7e7e7");
    private static final Color BRONZE_COLOR = Color.decode("#cd7f32");
    private static final int START_X = 80, START_Y = 568, PART_SIZE = 140, SPACING = 40;
    private static final String DISCORD_ICON_URL = "https://discord.com/assets/1f0bfc0865d324c2587920a7d80c609b.png";

    private static final Font FONT;

    static {
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try (final InputStream font = TurtyBot.loadResource("fonts/JetBrainsMono-Medium.ttf")) {
            if (font == null)
                throw new IllegalStateException("Unable to load font: 'fonts/JetBrainsMono-Medium.ttf'");

            FONT = Font
                    .createFont(Font.TRUETYPE_FONT, font)
                    .deriveFont(72f);
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException("Unable to load font", exception);
        }

        graphicsEnv.registerFont(FONT);
    }

    public LeaderboardCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gets the leaderboard for the server (for levels or economy).";
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getRichName() {
        return "Leaderboard";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("levels", "Gets the levelling leaderboard for this server."),
                new SubcommandData("economy", "Gets the economy leaderboard for this server.")
        );
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ You can only use this command inside of a server!", false, true);
            return;
        }

        event.deferReply().queue();

        String subcommand = event.getSubcommandName();
        switch (subcommand) {
            case "levels" -> {
                final List<Levelling> profiles = Database.getDatabase().levelling
                        .find(Filters.eq("guild", guild.getIdLong()))
                        .into(new ArrayList<>());
                if (profiles.isEmpty()) {
                    event.getHook().sendMessage("❌ This server has no levels!").mentionRepliedUser(false).queue();
                    return;
                }

                final List<Levelling> sorted = profiles.stream()
                        .sorted(Comparator.comparing(Levelling::getXp).reversed())
                        .filter(profile -> guild.isMember(UserSnowflake.fromId(profile.getUser())))
                        .toList();

                final List<Levelling> top10 = Lists.partition(sorted, 10).getFirst();
                try {
                    createAndSend(event, constructLevelLeaderboard(guild, top10));
                } catch (final IOException | NullPointerException | URISyntaxException exception) {
                    event.getHook()
                            .sendMessage("❌ There was an error while creating the leaderboard! Please try again later.")
                            .mentionRepliedUser(false)
                            .queue();
                    Constants.LOGGER.error("Unable to process leaderboard!", exception);
                }
            }
            case "economy" -> {
                final List<Economy> accounts = Database.getDatabase().economy
                        .find(Filters.eq("guild", guild.getIdLong()))
                        .into(new ArrayList<>());
                if (accounts.isEmpty()) {
                    event.getHook().sendMessage("❌ This server has no economy accounts!").mentionRepliedUser(false)
                            .queue();
                    return;
                }

                final List<Economy> sorted = accounts.stream()
                        .sorted(Comparator.comparing(EconomyManager::getBalance).reversed())
                        .filter(account -> guild.isMember(UserSnowflake.fromId(account.getUser())))
                        .toList();

                final List<Economy> top10 = Lists.partition(sorted, 10).getFirst();
                try {
                    createAndSend(event, constructEconomyLeaderboard(guild, top10));
                } catch (final IOException | NullPointerException | URISyntaxException exception) {
                    event.getHook()
                            .sendMessage("❌ There was an error while creating the leaderboard! Please try again later.")
                            .mentionRepliedUser(false)
                            .queue();
                    Constants.LOGGER.error("Unable to process leaderboard!", exception);
                }
            }
            case null, default -> event.getHook()
                    .sendMessage("❌ You must specify a valid subcommand!")
                    .mentionRepliedUser(false)
                    .queue();
        }
    }

    private static void createAndSend(SlashCommandInteractionEvent event, BufferedImage bufferedImage) {
        try {
            final var bao = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", bao);
            event.getHook()
                    .sendFiles(FileUpload.fromData(bao.toByteArray(), "leaderboard.png"))
                    .mentionRepliedUser(false)
                    .queue();
        } catch (final IOException | NullPointerException exception) {
            event.getHook()
                    .sendMessage("❌ There was an error while creating the leaderboard! Please try again later.")
                    .mentionRepliedUser(false)
                    .queue();
            Constants.LOGGER.error("Unable to process leaderboard!", exception);
        }
    }

    public static BufferedImage constructLevelLeaderboard(Guild guild, List<Levelling> profiles) throws IOException, NullPointerException, URISyntaxException {
        Pair<BufferedImage, Graphics2D> imageWithGraphics = constructTemplate();
        BufferedImage buffer = imageWithGraphics.getLeft();
        Graphics2D graphics = imageWithGraphics.getRight();
        final FontMetrics metrics = graphics.getFontMetrics();

        drawGuildInfo(guild, graphics, metrics);

        int maxUsernameLength = 0;
        int maxXPWidth = 0;
        int maxLevelWidth = 0;

        for (Levelling profile : profiles) {
            String username = getUsername(guild, profile.getUser());
            String truncatedUsername = StringUtils.truncateString(username, 20);
            String formattedXP = StringUtils.numberFormat(profile.getXp(), 0);
            if(formattedXP.endsWith(".0k")) {
                formattedXP = formattedXP.replace(".0k", "k");
            }

            String formattedLevel = StringUtils.numberFormat(profile.getLevel());
            if(formattedLevel.endsWith(".0k")) {
                formattedLevel = formattedLevel.replace(".0k", "k");
            }

            maxUsernameLength = Math.max(maxUsernameLength, truncatedUsername.length());
            maxXPWidth = Math.max(maxXPWidth, formattedXP.length());
            maxLevelWidth = Math.max(maxLevelWidth, formattedLevel.length());
        }

        for (int indexedRank = 0; indexedRank < 10; indexedRank++) {
            if (indexedRank >= profiles.size()) {
                break;
            }

            final Levelling profile = profiles.get(indexedRank);
            final long userId = profile.getUser();
            final int level = profile.getLevel();
            final int xp = profile.getXp();
            String username = getUsername(guild, userId);
            drawUser(guild, graphics, metrics, indexedRank, userId);

            Font font = FONT;
            FontMetrics metrics1 = graphics.getFontMetrics(font);

            String truncatedUsername = StringUtils.truncateString(username, 20);
            String formattedXP = StringUtils.numberFormat(xp, 0).replace(".0", "");
            String formattedLevel = StringUtils.numberFormat(level).replace(".0", "");

            String str = String.format(
                    "%-" + maxUsernameLength + "s | XP: %" + maxXPWidth + "s | Level: %" + maxLevelWidth + "s",
                    truncatedUsername, formattedXP, formattedLevel);

            while (metrics1.stringWidth(str) > 1850) {
                font = font.deriveFont(font.getSize() - 1f);
                metrics1 = graphics.getFontMetrics(font);
            }

            graphics.setFont(font);
            graphics.drawString(str, 420, START_Y + metrics.getHeight() + (SPACING + PART_SIZE) * indexedRank);
            graphics.setFont(FONT);
        }

        graphics.dispose();
        return buffer;
    }

    public static BufferedImage constructEconomyLeaderboard(Guild guild, List<Economy> accounts) throws IOException, NullPointerException, URISyntaxException {
        Pair<BufferedImage, Graphics2D> imageWithGraphics = constructTemplate();
        BufferedImage buffer = imageWithGraphics.getLeft();
        Graphics2D graphics = imageWithGraphics.getRight();
        final FontMetrics metrics = graphics.getFontMetrics();

        drawGuildInfo(guild, graphics, metrics);

        int maxUsernameLength = 0;
        int maxBalanceWidth = 0;

        for (Economy account : accounts) {
            String username = getUsername(guild, account.getUser());
            String truncatedUsername = StringUtils.truncateString(username, 20);
            String formattedBalance = StringUtils.numberFormat(EconomyManager.getBalance(account));

            maxUsernameLength = Math.max(maxUsernameLength, truncatedUsername.length());
            maxBalanceWidth = Math.max(maxBalanceWidth, formattedBalance.length());
        }

        for (int indexedRank = 0; indexedRank < 10; indexedRank++) {
            if (indexedRank >= accounts.size()) {
                break;
            }

            final Economy account = accounts.get(indexedRank);
            final long userId = account.getUser();
            final long balance = EconomyManager.getBalance(account);
            String username = getUsername(guild, userId);
            drawUser(guild, graphics, metrics, indexedRank, userId);

            GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
            if (guildData == null) {
                guildData = new GuildData(guild.getIdLong());
                Database.getDatabase().guildData.insertOne(guildData);
            }

            Font font = FONT;
            FontMetrics metrics1 = graphics.getFontMetrics(font);

            String truncatedUsername = StringUtils.truncateString(username, 20);
            String formattedBalance = StringUtils.numberFormat(balance);
            String str = String.format(
                    "%-" + maxUsernameLength + "s | Balance: %s%s",
                    truncatedUsername, guildData.getEconomyCurrency(), formattedBalance);

            while (metrics1.stringWidth(str) > 1850) {
                font = font.deriveFont(font.getSize() - 1f);
                metrics1 = graphics.getFontMetrics(font);
            }

            graphics.setFont(font);
            graphics.drawString(str, 420, START_Y + metrics.getHeight() + (SPACING + PART_SIZE) * indexedRank);
            graphics.setFont(FONT);
        }

        graphics.dispose();
        return buffer;
    }

    private static String getUsername(Guild guild, long userId) {
        final Member member = guild.getMemberById(userId);
        User user = member == null ? guild.getJDA().getUserById(userId) : member.getUser();
        return user == null ? "Unknown" : user.getEffectiveName();
    }

    private static void drawUser(Guild guild, Graphics2D graphics, FontMetrics metrics, int indexedRank, long userId) throws IOException, URISyntaxException {
        final int rank = indexedRank + 1;

        final Member member = guild.getMemberById(userId);
        User user = member == null ? guild.getJDA().getUserById(userId) : member.getUser();
        InputStream avatarStream;
        if (member == null) {
            if (user != null) {
                avatarStream = user.getEffectiveAvatar().download(512).join();
            } else {
                avatarStream = new URI(DISCORD_ICON_URL).toURL().openStream();
            }
        } else {
            avatarStream = member.getEffectiveAvatar().download(512).join();
        }

        final BufferedImage avatarImage = ImageIO.read(avatarStream);
        BotUtils.resize(avatarImage, PART_SIZE);
        graphics.drawImage(avatarImage, START_X, START_Y + (SPACING + PART_SIZE) * indexedRank, PART_SIZE, PART_SIZE, null);

        switch (rank) {
            case 1 -> graphics.setColor(GOLD_COLOR);
            case 2 -> graphics.setColor(SILVER_COLOR);
            case 3 -> graphics.setColor(BRONZE_COLOR);
            default -> graphics.setColor(Color.LIGHT_GRAY);
        }

        graphics.drawString("#" + rank, 240, START_Y + metrics.getHeight() + (SPACING + PART_SIZE) * indexedRank);

        {
            GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
            if (guildData == null) {
                guildData = new GuildData(guild.getIdLong());
                Database.getDatabase().guildData.insertOne(guildData);
            }

            long patronRoleId = guildData.getPatronRole();
            Role patronRole = guild.getRoleById(patronRoleId);

            boolean isPatron = patronRole != null && member != null && member.getRoles().contains(patronRole);
            boolean isBooster = member != null && member.isBoosting();
            boolean isOwner = member != null && member.isOwner();

            if (isPatron || isBooster || isOwner) {
                UserConfig userConfig = Database.getDatabase().userConfig.find(
                        Filters.and(
                                Filters.eq("user", userId),
                                Filters.eq("guild", guild.getIdLong())
                        )).first();

                if (userConfig != null && userConfig.getLeaderboardColor() != null) {
                    graphics.setColor(Color.decode(userConfig.getLeaderboardColor()));
                } else {
                    graphics.setColor(Color.WHITE);
                }
            } else {
                graphics.setColor(Color.WHITE);
            }
        }
    }

    private static Pair<BufferedImage, Graphics2D> constructTemplate() throws IOException {
        final BufferedImage template = getTemplate();

        final var buffer = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = buffer.createGraphics();
        graphics.drawImage(template, 0, 0, template.getWidth(), template.getHeight(), null);
        graphics.setFont(FONT);

        return Pair.of(buffer, graphics);
    }

    private static void drawGuildInfo(Guild guild, Graphics2D graphics, FontMetrics metrics) throws IOException, NullPointerException, URISyntaxException {
        InputStream guildIconStream = guild.getIcon().download().join();
        if (guildIconStream == null) {
            guildIconStream = new URI(DISCORD_ICON_URL).toURL().openStream();
        }

        final BufferedImage guildIcon = BotUtils.resize(ImageIO.read(guildIconStream), 420);
        graphics.drawImage(guildIcon, 125, 125, guildIcon.getWidth(), guildIcon.getHeight(), null);

        final String guildName = guild.getName();
        final int guildLength = metrics.stringWidth(guildName);
        graphics.drawString(guildName, 600, 300);
        graphics.setStroke(new BasicStroke(10));
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawLine(600, 300 + metrics.getHeight() / 2 - 20, 600 + guildLength,
                300 + metrics.getHeight() / 2 - 20);
    }

    private static BufferedImage getTemplate() throws IOException, NullPointerException {
        final InputStream stream = TurtyBot.loadResource("leaderboard.png");
        if (stream == null)
            throw new NullPointerException("Unable to load resource: 'leaderboard.png'");

        return ImageIO.read(stream);
    }
}
