package io.github.darealturtywurty.superturtybot.commands.levelling;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.TurtyBot;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Levelling;
import io.github.darealturtywurty.superturtybot.registry.impl.RankCardItemRegistry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class XPInventoryCommand extends CoreCommand {
    private final Font usedFont;
    
    public XPInventoryCommand() {
        super(new Types(true, false, false, false));
        final var graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            this.usedFont = Font
                .createFont(Font.TRUETYPE_FONT, TurtyBot.class.getResourceAsStream("/fonts/Code New Roman.otf"))
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
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("You must be in a server to use this command!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        if (!LevellingManager.INSTANCE.areLevelsEnabled(event.getGuild())) {
            event.deferReply(true).setContent("Levelling has been disabled for this server!").mentionRepliedUser(false)
                .queue();
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
        
        try {
            final BufferedImage image = createInventory(inventory, event.getMember());
            final var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            event.deferReply().addFile(output.toByteArray(), "inventory.png").mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            Constants.LOGGER.error(ExceptionUtils.getStackTrace(exception));
            event.deferReply(true)
                .setContent("There has been an error creating your inventory. This has been reported to the bot owner!")
                .mentionRepliedUser(false).queue();
        }
    }

    private BufferedImage createInventory(List<String> inventory, Member member) throws IOException {
        final BufferedImage template = getTemplate();
        final var buffer = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = buffer.createGraphics();
        
        graphics.drawImage(template, 0, 0, template.getWidth(), template.getHeight(), null);
        
        final BufferedImage profilePic = RankCommand
            .resize(ImageIO.read(new URL(member.getUser().getEffectiveAvatarUrl())), 100);
        graphics.drawImage(profilePic, 80, 68, profilePic.getWidth(), profilePic.getHeight(), null);
        
        graphics.setFont(this.usedFont);
        final String name = member.getUser().getName();
        graphics.drawString((name.length() > 22 ? name.substring(0, 22) + "..." : name) + "'s Levelling Inventory",
            80 + profilePic.getWidth() + 30,
            68 + profilePic.getHeight() / 2 + graphics.getFontMetrics().getHeight() / 4);

        final List<RankCardItem> items = inventory.stream()
            .map(n -> RankCardItemRegistry.RANK_CARD_ITEMS.getRegistry().get(n)).toList();

        // TODO: sort items and render items

        graphics.dispose();
        return buffer;
    }

    private static BufferedImage getTemplate() throws IOException {
        return ImageIO.read(TurtyBot.class.getResourceAsStream("/levels/xp_inventory.png"));
    }
}
