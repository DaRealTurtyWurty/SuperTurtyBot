package dev.darealturtywurty.superturtybot.modules.economy.command;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.PublicShop;
import dev.darealturtywurty.superturtybot.modules.economy.ShopItem;
import net.dv8tion.jda.api.EmbedBuilder;
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

public class ShopCommand extends CoreCommand {
    public ShopCommand() {
        super(new Types(true, false, false, false));
    }

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
    public CommandCategory getCategory() {
        return CommandCategory.ECONOMY;
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

        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        switch (subcommand) {
            case "view" -> {
                User user = event.getOption("user", null, OptionMapping::getAsUser);
                if (user != null) {
                    Member member = guild.getMember(user);
                    if (member == null) {
                        reply(event, "❌ That user is not in this server!", false, true);
                        return;
                    }

                    Economy account = EconomyManager.fetchAccount(guild, user);
                    if (account == null) {
                        reply(event, "❌ That user does not have an account!", false, true);
                        return;
                    }

                    List<ShopItem> shop = account.getShopItems();
                    if (shop.isEmpty()) {
                        reply(event, "❌ That user does not have any items in their shop!", false, true);
                        return;
                    }

                    var embed = new EmbedBuilder();
                    embed.setTitle("Shop for " + user.getAsTag());
                    embed.setColor(member.getColorRaw());
                    embed.setFooter(user.getAsTag(), member.getEffectiveAvatarUrl());
                    embed.setTimestamp(Instant.now());

                    // TODO: Pagination
                    List<ShopItem> shortened = shop.subList(0, Math.min(shop.size(), 25));
                    for (ShopItem item : shortened) {
                        embed.addField(item.getEmoji() + " " + item.getName(),
                                "ID: " + item.getId() + "\nPrice: " + item.getPrice(), false);
                    }

                    reply(event, embed);
                    return;
                }

                PublicShop shop = EconomyManager.getPublicShop();
                if (shop.getDiscountItems().isEmpty() && shop.getFeaturedItems().isEmpty() && shop.getNewItems()
                        .isEmpty()) {
                    reply(event, "❌ The shop is currently empty, please come back later!", false, true);
                    return;
                }

                event.deferReply().queue();

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

    private static BufferedImage generateShopImage() {
        var image = new BufferedImage(700, 500, BufferedImage.TYPE_INT_ARGB);
        var g2d = image.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, 700, 500);

        PublicShop shop = EconomyManager.getPublicShop();

        g2d.setFont(g2d.getFont().deriveFont(30f));

        g2d.setColor(Color.RED);
        int index = 0;
        for (ShopItem item : shop.getNewItems()) {
            String text = item.getEmoji() + " " + item.getName();
            g2d.drawString(text, 10 + (index++ * g2d.getFontMetrics().stringWidth(text) + 5),
                    g2d.getFontMetrics().getHeight());
        }

        g2d.setColor(Color.GREEN);
        index = 0;
        for (ShopItem item : shop.getDiscountItems()) {
            String text = item.getEmoji() + " " + item.getName();
            g2d.drawString(text, 10 + (index++ * g2d.getFontMetrics().stringWidth(text) + 5),
                    g2d.getFontMetrics().getHeight() * 2);
        }

        g2d.setColor(Color.BLUE);
        index = 0;
        for (ShopItem item : shop.getFeaturedItems()) {
            String text = item.getEmoji() + " " + item.getName();
            g2d.drawString(text, 10 + (index++ * g2d.getFontMetrics().stringWidth(text) + 5),
                    g2d.getFontMetrics().getHeight() * 3);
        }

        g2d.dispose();

        return image;
    }
}
