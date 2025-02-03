package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
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
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class ShopCommand extends EconomyCommand {
    @Override
    public List<SubcommandData> createSubcommandData() {
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
    protected void runSlash(SlashCommandInteractionEvent event, Guild guild, GuildData config) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("❌ You must provide a subcommand!").queue();
            return;
        }

        switch (subcommand) { // TODO: Finish this
            case "view" -> {
                User user = event.getOption("user", null, OptionMapping::getAsUser);
                if (user != null) {
                    Member member = guild.getMember(user);
                    if (member == null) {
                        event.getHook().editOriginal("❌ That user is not in this server!").queue();
                        return;
                    }

                    Economy account = EconomyManager.getOrCreateAccount(guild, user);
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
                            .footer(user.getEffectiveName(), member.getEffectiveAvatarUrl())
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
                    var baos = new ByteArrayOutputStream();
                    ImageIO.write(generateShopImage(), "png", baos);
                    var upload = FileUpload.fromData(baos.toByteArray(), "shop.png");
                    event.getHook().sendFiles(upload).queue();
                } catch (IOException exception) {
                    event.getHook().editOriginal("❌ An error occurred while generating the shop image!").queue();
                    Constants.LOGGER.error("❌ An error occurred while generating the shop image!", exception);
                }
            }
        }
    }

    private static BufferedImage generateShopImage() throws IOException {
        PublicShop shop = EconomyManager.getPublicShop();

        final InputStream stream = TurtyBot.loadResource("economy/shop.png");
        if (stream == null)
            throw new IOException("Could not find shop image!");

        BufferedImage image = ImageIO.read(stream);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.RED);
        g2d.fillRect(0, image.getHeight() / 2 - 5, image.getWidth(), 10);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(image.getWidth() / 2 - 5, 0, 10, image.getHeight());

        g2d.setFont(g2d.getFont().deriveFont(500f));
        g2d.setColor(Color.BLACK);
        g2d.drawString("New", image.getWidth() / 2 - g2d.getFontMetrics().stringWidth("New") / 2, 400);

        g2d.setFont(g2d.getFont().deriveFont(250f));

        List<ShopItem> newItems = shop.getFeaturedItems();

        int totalWidth = 0;
        for (ShopItem item : newItems) {
            String name = item.getName();
            totalWidth += Math.max(g2d.getFontMetrics().stringWidth(name), 750) + 50;
        }

        int x = (image.getWidth() - totalWidth) / 2 + 50;
        for (ShopItem item : newItems) {
            String name = item.getName();
            BufferedImage img = ImageIO.read(
                    Objects.requireNonNull(TurtyBot.class.getResourceAsStream(item.getImage())));

            g2d.setColor(Color.BLACK);
            g2d.fillRect(x - 5,
                    545,
                    Math.max(g2d.getFontMetrics().stringWidth(name) + 5, 755),
                    805 + g2d.getFontMetrics().getHeight());

            g2d.drawImage(img, x, 550, 750, 750, null);

            g2d.setColor(Color.WHITE);
            g2d.drawString(name, x + 375 - g2d.getFontMetrics().stringWidth(name) / 2, 1500);

            x += Math.max(g2d.getFontMetrics().stringWidth(name), 750) + 50;
        }

        g2d.dispose();
        return image;
    }
}