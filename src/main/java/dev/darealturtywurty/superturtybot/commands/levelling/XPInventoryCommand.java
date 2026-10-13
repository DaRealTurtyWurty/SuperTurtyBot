package dev.darealturtywurty.superturtybot.commands.levelling;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.BotUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import dev.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bson.conversions.Bson;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
import dev.darealturtywurty.superturtybot.database.pojos.RankCard;

public class XPInventoryCommand extends CoreCommand {
    private final Font usedFont;

    public XPInventoryCommand() {
        super(new Types(true, false, false, false));
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try (final InputStream stream = TurtyBot.loadResource("fonts/Code New Roman.otf")) {
            if (stream == null)
                throw new IllegalStateException("Unable to load font: fonts/Code New Roman.otf");

            this.usedFont = Font
                .createFont(Font.TRUETYPE_FONT, stream)
                .deriveFont(60f);
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException("Unable to load font", exception);
        }

        graphicsEnv.registerFont(this.usedFont);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.LEVELLING;
    }

    @Override
    public String getDescription() {
        return "Displays the items that you currently have in your xp(levelling) inventory";
    }

    @Override
    public String getName() {
        return "xpinventory";
    }

    @Override
    public String getRichName() {
        return "XP Inventory";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null || event.getMember() == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        if (!LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
            reply(event, "❌ Levelling has been disabled for this server!", false, true);
            return;
        }

        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("user", event.getUser().getIdLong()));
        Levelling profile = Database.getDatabase().levelling.find(filter).first();
        if (profile == null) {
            profile = new Levelling(event.getGuild().getIdLong(), event.getUser().getIdLong());
            Database.getDatabase().levelling.insertOne(profile);
        }

        final List<String> inventory = profile.getInventory();
        event.deferReply().mentionRepliedUser(false).queue();

        try {
            final BufferedImage image = createInventory(inventory, profile.getRankCard(), event.getMember());
            final var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            event.getHook().editOriginal(
                "Use `/rankcard` to equip an item, or leave its item option empty to reset a slot.\nDrops are earned from messages eligible for XP; items belong to this server.")
                .setFiles(FileUpload.fromData(output.toByteArray(), "inventory.png"))
                .mentionRepliedUser(false).queue();
        } catch (final IOException | URISyntaxException exception) {
            Constants.LOGGER.error("Error creating inventory!", exception);
            reply(event, "❌ There has been an error creating your inventory. This has been reported to the bot owner!",
                false, true);
        }
    }

    private BufferedImage createInventory(List<String> inventory, RankCard card, Member member)
        throws IOException, URISyntaxException {
        final BufferedImage buffer = createTemplate(this.usedFont);
        final Graphics2D graphics = buffer.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        final BufferedImage profilePic = BotUtils
            .resize(ImageIO.read(new URI(member.getUser().getEffectiveAvatarUrl()).toURL()), 100);
        graphics.drawImage(profilePic, 80, 68, profilePic.getWidth(), profilePic.getHeight(), null);

        graphics.setFont(this.usedFont.deriveFont(42f));
        graphics.setColor(Color.WHITE);
        final String name = member.getUser().getEffectiveName();
        graphics.drawString((name.length() > 22 ? name.substring(0, 22) + "..." : name) + "'s Levelling Inventory",
            80 + profilePic.getWidth() + 30,
            68 + profilePic.getHeight() / 2 + graphics.getFontMetrics().getHeight() / 4);

        drawItems(graphics, inventory, card, this.usedFont);

        graphics.dispose();
        return buffer;
    }

    static void drawItems(Graphics2D graphics, List<String> inventory, RankCard card, Font font) {
        final List<RankCardItem> items = inventory.stream().distinct()
            .map(RankCardItemRegistry.RANK_CARD_ITEMS::get).filter(Objects::nonNull)
            .sorted(Comparator.comparing(RankCardItem::getName)).toList();

        for (RankCardItem.Type type : RankCardItem.Type.values()) {
            List<RankCardItem> row = items.stream().filter(item -> item.type == type).toList();
            int y = 175 + type.ordinal() * 156;
            for (int index = 0; index < row.size(); index++) {
                RankCardItem item = row.get(index);
                int x = 202 + index * 156;
                if (item.thumbnail != null) {
                    graphics.drawImage(item.thumbnail, x + 3, y + 3, null);
                }
                graphics.setFont(font.deriveFont(16f));
                graphics.setColor(Color.WHITE);
                graphics.drawString(item.getDisplayName(), x + 3, y + 108);
                graphics.drawString(item.rarity.saveName, x + 3, y + 130);
                if (RankCardItemRegistry.resolveImage(type, type.getImage(card)) == item) {
                    graphics.setColor(new Color(136, 230, 120));
                    graphics.drawString("Equipped", x + 3, y + 149);
                }
            }
        }
        if (items.isEmpty()) {
            graphics.setFont(font.deriveFont(32f));
            graphics.setColor(Color.WHITE);
            graphics.drawString("No items yet. Keep chatting to earn rank card rewards!", 230, 250);
        }

    }

    static BufferedImage createTemplate(Font font) {
        int columns = 10;
        for (RankCardItem.Type type : RankCardItem.Type.values()) {
            int count = (int) RankCardItemRegistry.RANK_CARD_ITEMS.values().stream()
                .filter(item -> item.type == type).count();
            columns = Math.max(columns, count);
        }
        int extraWidth = (columns - 10) * 156;
        BufferedImage image = new BufferedImage(1820 + extraWidth, 1200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(44, 62, 78));
        graphics.fillRoundRect(0, 0, image.getWidth(), 1200, 140, 140);
        graphics.setColor(new Color(51, 73, 91));
        graphics.fillRoundRect(50, 50, image.getWidth() - 100, 1100, 100, 100);
        graphics.setStroke(new BasicStroke(5));
        graphics.setFont(font.deriveFont(22f));
        for (RankCardItem.Type type : RankCardItem.Type.values()) {
            int y = 170 + type.ordinal() * 156;
            graphics.setColor(new Color(73, 101, 124));
            graphics.drawRect(62, y, 1694 + extraWidth, 156);
            graphics.drawLine(197, y, 197, y + 156);
            for (int column = 1; column < columns; column++)
                graphics.drawLine(197 + column * 156, y, 197 + column * 156, y + 156);
            graphics.setColor(Color.WHITE);
            String label = type.displayName;
            String[] words = label.split(" ");
            for (int line = 0; line < words.length; line++)
                graphics.drawString(words[line], 78, y + 62 + line * 28);
        }
        graphics.dispose();
        return image;
    }
}
