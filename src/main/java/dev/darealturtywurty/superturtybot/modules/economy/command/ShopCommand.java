package dev.darealturtywurty.superturtybot.modules.economy.command;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.PublicShop;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class ShopCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("view", "View the shop").addOption(OptionType.USER, "user",
                        "The user to view the shop of", false),
                new SubcommandData("buy", "Buy an item from the shop").addOption(OptionType.INTEGER, "item_id",
                        "The ID of the item you want to buy", true),
                new SubcommandData("sell", "Sell an item to the shop").addOption(OptionType.INTEGER, "item_id",
                        "The ID of the item you want to sell", true).addOption(OptionType.INTEGER, "amount",
                        "The amount of money that you want to sell this item for", true));
    }

    @Override
    public String getDescription() {
        return "Access the shop to view, buy, and sell items!";
    }

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public String getRichName() {
        return "Shop";
    }

    @Override
    public String getHowToUse() {
        return "/shop view <user>\n/shop buy <item_id>\n/shop sell <item_id> <amount>";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        event.deferReply().queue();

        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().editOriginal("❌ You must be in a server to use this command!").queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        switch (subcommand) {
            case "view" -> {
                User user = event.getOption("user", null, OptionMapping::getAsUser);
                if (user != null) {
                    Member member = guild.getMember(user);
                    if (member == null) {
                        event.getHook().editOriginal("❌ That user is not in this server!").queue();
                        return;
                    }

                    Economy account = EconomyManager.getAccount(guild, user);
                    List<ShopItem> shop = account.getShopItems();
                    if (shop.isEmpty()) {
                        event.getHook().editOriginal("❌ That user does not have any items in their shop!").queue();
                        return;
                    }

                    var contents = new PaginatedEmbed.ContentsBuilder();
                    for (ShopItem item : shop) {
                        contents.field(item.getImage() + " " + item.getName(),
                                "ID: " + item.getId() + "\nPrice: " + item.getPrice());
                    }

                    PaginatedEmbed paginatedEmbed = new PaginatedEmbed.Builder(10, contents)
                            .timestamp(Instant.now())
                            .title("Shop for " + user.getName())
                            .color(member.getColorRaw())
                            .footer(user.getName(), member.getEffectiveAvatarUrl())
                            .authorOnly(event.getUser().getIdLong())
                            .build(event.getJDA());

                    paginatedEmbed.send(event.getHook());
                    return;
                }

                PublicShop shop = EconomyManager.getPublicShop();
                if (shop.getDiscountItems().isEmpty() && shop.getFeaturedItems().isEmpty() && shop.getNewItems()
                        .isEmpty()) {
                    event.getHook().editOriginal("❌ The shop is currently empty, please come back later!").queue();
                    return;
                }

                try {
                    var boas = new ByteArrayOutputStream();
                    ImageIO.write(generateShopImage(), "png", boas);
                    var upload = FileUpload.fromData(boas.toByteArray(), "shop.png");
                    event.getHook().sendFiles(upload).queue();
                } catch (IOException exception) {
                    exception.printStackTrace();
                    event.getHook().editOriginal("❌ An error occurred while generating the shop image!").queue();
                }
            }
        }
    }

    private static BufferedImage generateShopImage() throws IOException {
        PublicShop shop = EconomyManager.getPublicShop();

        var image = ImageIO.read(Objects.requireNonNull(TurtyBot.class.getResourceAsStream("/shop.png")));
        var g2d = image.createGraphics();

        g2d.setFont(g2d.getFont().deriveFont(60f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("New", image.getWidth() / 2, g2d.getFontMetrics().getHeight());

        g2d.setFont(g2d.getFont().deriveFont(30f));
        g2d.setColor(Color.RED);
        int index = 0;
        for (ShopItem item : shop.getNewItems()) {
            String name = item.getName();
            BufferedImage img = ImageIO.read(
                    Objects.requireNonNull(TurtyBot.class.getResourceAsStream(item.getImage())));

            g2d.drawImage(img, 10 + (index++ * 50), g2d.getFontMetrics().getHeight() + 5, 50, 50, null);
            g2d.drawString(name, 10 + (index++ * 50), g2d.getFontMetrics().getHeight() * 2 + 55);
        }

        g2d.setFont(g2d.getFont().deriveFont(60f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Discounted", image.getWidth() / 2, g2d.getFontMetrics().getHeight() + 150);

        g2d.setFont(g2d.getFont().deriveFont(30f));
        g2d.setColor(Color.GREEN);
        index = 0;
        for (ShopItem item : shop.getDiscountItems()) {
            String name = item.getName();
            BufferedImage img = ImageIO.read(Objects.requireNonNull(TurtyBot.class.getResourceAsStream(item.getImage())));

            g2d.drawImage(img, 10 + (index++ * 50), g2d.getFontMetrics().getHeight() + 155, 50, 50, null);
            g2d.drawString(name, 10 + (index++ * 50), g2d.getFontMetrics().getHeight() * 2 + 205);
        }

        g2d.setFont(g2d.getFont().deriveFont(60f));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Featured", image.getWidth() / 2, g2d.getFontMetrics().getHeight() + 300);

        g2d.setFont(g2d.getFont().deriveFont(30f));
        g2d.setColor(Color.BLUE);
        index = 0;
        for (ShopItem item : shop.getFeaturedItems()) {
            String name = item.getName();
            BufferedImage img = ImageIO.read(Objects.requireNonNull(TurtyBot.class.getResourceAsStream(item.getImage())));

            g2d.drawImage(img, 10 + (index++ * 50), g2d.getFontMetrics().getHeight() + 305, 50, 50, null);
            g2d.drawString(name, 10 + (index++ * 50), g2d.getFontMetrics().getHeight() * 2 + 355);
        }

        g2d.dispose();

        return image;
    }
}