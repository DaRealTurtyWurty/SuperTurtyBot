package io.github.darealturtywurty.superturtybot.commands.fun;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class MinecraftUserSkinCommand extends CoreCommand {
    public MinecraftUserSkinCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.STRING, "username", "The username of which to get the skin for", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Gets the Minecraft Skin from a username.";
    }

    @Override
    public String getHowToUse() {
        return "/mc-skin [username]";
    }

    @Override
    public String getName() {
        return "mc-skin";
    }

    @Override
    public String getRichName() {
        return "Minecraft Skin";
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        final Message message = event.getMessage();
        if (event.getComponentId().startsWith(message.getIdLong() + "-rotate-counter-clockwise")) {
            int rotation = Integer.parseInt(event.getComponentId().split("-")[4]) - 45;
            if (rotation <= 0) {
                rotation = 355;
            }
            if (rotation >= 360) {
                rotation = 5;
            }

            final MessageEmbed embed = message.getEmbeds().get(0);
            final EmbedBuilder newEmbed = new EmbedBuilder(embed);
            final String filename = embed.getImage().getProxyUrl()
                .substring(embed.getImage().getProxyUrl().lastIndexOf("/")).replace("/", "");
            final String username = filename.split(".png")[0];
            try {
                final byte[] bytes = decodeURL(new URL(
                    "https://minecraft-api.com/api/skins/" + username + "/body/" + "10." + rotation + "/10/json"));
                newEmbed.setImage("attachment://" + username + ".png");
                final var atomicRotation = new AtomicInteger(rotation);
                event.deferEdit().retainFiles(message.getAttachments()).setCheck(event::isAcknowledged)
                    .queue(hook -> hook.editOriginalEmbeds(newEmbed.build())
                        .setActionRows(createButtons(message.getIdLong(), atomicRotation.get()))
                        .addFile(bytes, username + ".png").queue());
            } catch (final IOException exception) {

            }
        } else if (event.getComponentId().startsWith(message.getIdLong() + "-dismiss")) {
            if (message.getInteraction() == null) {
                message.getMessageReference().resolve().queue(msg -> {
                    if (event.getUser().getIdLong() != msg.getAuthor().getIdLong()) {
                        event.deferEdit().queue();
                    } else {
                        message.delete().queue();
                        msg.delete().queue();
                    }
                }, error -> {
                    message.delete().queue();
                });

                return;
            }

            final User author = message.getInteraction().getUser();
            if (event.getUser().getIdLong() != author.getIdLong()) {
                event.deferEdit().queue();
                return;
            }

            message.delete().queue();
        } else if (event.getComponentId().startsWith(message.getIdLong() + "-rotate-clockwise")) {
            int rotation = Integer.parseInt(event.getComponentId().split("-")[3]) + 45;
            if (rotation <= 0) {
                rotation = 355;
            }
            if (rotation >= 360) {
                rotation = 5;
            }

            final MessageEmbed embed = message.getEmbeds().get(0);
            final EmbedBuilder newEmbed = new EmbedBuilder(embed);
            final String filename = embed.getImage().getProxyUrl()
                .substring(embed.getImage().getProxyUrl().lastIndexOf("/")).replace("/", "");
            final String username = filename.split(".png")[0];
            try {
                final byte[] bytes = decodeURL(new URL(
                    "https://minecraft-api.com/api/skins/" + username + "/body/" + "10." + rotation + "/10/json"));
                newEmbed.setImage("attachment://" + username + ".png");
                final var atomicRotation = new AtomicInteger(rotation);
                event.deferEdit().retainFiles(message.getAttachments()).setCheck(() -> event.isAcknowledged())
                    .queue(hook -> hook.editOriginalEmbeds(newEmbed.build())
                        .setActionRows(createButtons(message.getIdLong(), atomicRotation.get()))
                        .addFile(bytes, username + ".png").queue());
            } catch (final IOException exception) {

            }
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String username = URLEncoder.encode(event.getOption("username").getAsString().trim(),
            StandardCharsets.UTF_8);
        try {
            final int rotation = 10;
            final var url = new URL(
                "https://minecraft-api.com/api/skins/" + username + "/body/" + "10." + rotation + "/10/json");
            final byte[] bytes = decodeURL(url);

            final var embed = new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.BLUE)
                .setDescription("The skin for `" + event.getOption("username").getAsString() + "` is:")
                .setImage("attachment://" + username + ".png").build();
            event.deferReply().addFile(bytes, username + ".png").addEmbeds(embed).mentionRepliedUser(false)
                .flatMap(InteractionHook::retrieveOriginal).queue(msg -> msg.editMessageEmbeds(embed)
                    .setActionRows(createButtons(msg.getIdLong(), rotation)).queue());
        } catch (final IOException exception) {
            event.deferReply(true)
                .setContent("There has been an issue trying to gather this information from our database! "
                    + "This has been reported to the bot owner!")
                .mentionRepliedUser(false).queue();
        } catch (final IllegalArgumentException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("This player does not exist!").mentionRepliedUser(false).queue();
        }
    }

    private static ActionRow createButtons(long messageId, int rotation) {
        return ActionRow.of(Button.primary(messageId + "-rotate-counter-clockwise-" + rotation, "↩️"),
            Button.primary(messageId + "-dismiss", "🚮"),
            Button.primary(messageId + "-rotate-clockwise-" + rotation, "↪️"));
    }

    private static byte[] decodeURL(URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        final String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
        if (response.contains("not found"))
            throw new IllegalArgumentException(response);
        final String base64 = Constants.GSON.fromJson(response, JsonObject.class).get("skin").getAsString();
        return Base64.decodeBase64(base64);
    }
}
