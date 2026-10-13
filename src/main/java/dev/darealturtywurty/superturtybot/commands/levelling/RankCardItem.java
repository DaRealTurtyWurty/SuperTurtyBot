package dev.darealturtywurty.superturtybot.commands.levelling;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.registry.Registerable;
import dev.darealturtywurty.superturtybot.database.pojos.RankCard;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.text.WordUtils;

public class RankCardItem implements Registerable {
    public final String data;
    public final Type type;
    public final Rarity rarity;
    public final BufferedImage thumbnail;

    private String name;

    public RankCardItem(String data, Type type, Rarity rarity) {
        this(data, type, rarity, thumbnail(data, type));
    }

    public RankCardItem(String data, Type type, Rarity rarity, BufferedImage thumbnail) {
        this.data = data;
        this.type = type;
        this.rarity = rarity;
        this.thumbnail = thumbnail;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Registerable setName(String name) {
        if (this.name == null) {
            this.name = name;
        }

        return this;
    }

    public String getDisplayName() {
        return WordUtils.capitalizeFully(this.data.replace('_', ' '));
    }

    public String getResourcePath() {
        return "levels/" + this.type.directory + "/" + this.data + ".jpg";
    }

    private static BufferedImage thumbnail(String name, Type type) {
        try (final InputStream stream = TurtyBot.loadResource("levels/" + type.directory + "/" + name + ".jpg")) {
            if (stream == null) {
                Constants.LOGGER.warn("Could not find thumbnail for: {}", name);
                return null;
            }

            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                Constants.LOGGER.warn("Could not decode rank card texture: {}/{}", type.directory, name);
                return null;
            }
            return RankCardTextures.preview(image, type);
        } catch (final IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    public enum Rarity {
        COMMON("common", 0.4f),
        UNCOMMON("uncommon", 0.3f),
        RARE("rare", 0.15f),
        EPIC("epic", 0.1f),
        LEGENDARY("legendary", 0.05f);

        public final String saveName;
        public final float chance;

        Rarity(String name, float chance) {
            this.saveName = name;
            this.chance = chance;
        }
    }

    public enum Type {
        BACKGROUND_IMAGE("bgImg", "background", "backgroundImage", "Background"),
        OUTLINE_IMAGE("outlineImg", "outline", "outlineImage", "Outline"),
        XP_OUTLINE_IMAGE("xpOutlineImg", "xpOutline", "xpOutlineImage", "XP outline"),
        XP_EMPTY_IMAGE("xpEmptyImg", "xpEmpty", "xpEmptyImage", "XP empty"),
        XP_FILL_IMAGE("xpFillImg", "xpFill", "xpFillImage", "XP fill"),
        AVATAR_OUTLINE_IMAGE("avatarOutlineImg", "avatarOutline", "avatarOutlineImage", "Avatar outline");

        public final String saveName;
        public final String directory;
        public final String cardField;
        public final String displayName;

        Type(String name, String directory, String cardField, String displayName) {
            this.saveName = name;
            this.directory = directory;
            this.cardField = cardField;
            this.displayName = displayName;
        }

        public String getImage(RankCard card) {
            return switch (this) {
                case BACKGROUND_IMAGE -> card.getBackgroundImage();
                case OUTLINE_IMAGE -> card.getOutlineImage();
                case XP_OUTLINE_IMAGE -> card.getXpOutlineImage();
                case XP_EMPTY_IMAGE -> card.getXpEmptyImage();
                case XP_FILL_IMAGE -> card.getXpFillImage();
                case AVATAR_OUTLINE_IMAGE -> card.getAvatarOutlineImage();
            };
        }

        public void setImage(RankCard card, String image) {
            switch (this) {
                case BACKGROUND_IMAGE -> card.setBackgroundImage(image);
                case OUTLINE_IMAGE -> card.setOutlineImage(image);
                case XP_OUTLINE_IMAGE -> card.setXpOutlineImage(image);
                case XP_EMPTY_IMAGE -> card.setXpEmptyImage(image);
                case XP_FILL_IMAGE -> card.setXpFillImage(image);
                case AVATAR_OUTLINE_IMAGE -> card.setAvatarOutlineImage(image);
            }
        }
    }
}
