package dev.darealturtywurty.superturtybot.commands.util.minecraft;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MinecraftCommand extends CoreCommand {
    public MinecraftCommand() {
        super(new Types(true, false, false, false));
    }

    private static void handleUsername(SlashCommandInteractionEvent event) {
        String rawUUID = event.getOption("uuid", null, OptionMapping::getAsString);
        if (rawUUID == null) {
            event.getHook().sendMessage("❌ You must provide a UUID!").queue();
            return;
        }

        String encoded = URLEncoder.encode(rawUUID.trim(), StandardCharsets.UTF_8);
        String url = "https://minecraft-api.com/api/pseudo/%s".formatted(encoded);
        Request request = new Request.Builder().get().url(url).build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from server!").queue();
                return;
            }

            String responseString = body.string();
            if (responseString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from server!").queue();
                return;
            }

            if (responseString.contains("Player not found !")) {
                event.getHook().sendMessage("❌ UUID not found!").queue();
                return;
            }

            event.getHook().sendMessage("✅ The Username for `%s` is: `%s`".formatted(rawUUID, responseString)).queue();
        } catch (final IOException exception) {
            Constants.LOGGER.error("Error getting username for {}", rawUUID, exception);
            event.getHook().sendMessage("❌ An error occurred while getting the username!").queue();
        }
    }

    private static void handleUUID(SlashCommandInteractionEvent event) {
        String username = event.getOption("username", null, OptionMapping::getAsString);
        if (username == null) {
            event.getHook().sendMessage("❌ You must provide a username!").queue();
            return;
        }

        String encoded = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
        String url = "https://minecraft-api.com/api/uuid/%s".formatted(encoded);
        Request request = new Request.Builder().get().url(url).build();
        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                event.getHook().sendMessage("❌ Response was not successful!").queue();
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                event.getHook().sendMessage("❌ Invalid response from server!").queue();
                return;
            }

            String responseString = body.string();
            if (responseString.isBlank()) {
                event.getHook().sendMessage("❌ Empty response from server!").queue();
                return;
            }

            if (responseString.contains("Player not found !")) {
                event.getHook().sendMessage("❌ Username not found!").queue();
                return;
            }

            event.getHook().sendMessage("✅ The UUID for `%s` is: `%s`".formatted(username, responseString)).queue();
        } catch (SocketTimeoutException exception) {
            event.getHook().sendMessage("❌ The request timed out! Please try again later.").queue();
        } catch (final IOException exception) {
            Constants.LOGGER.error("Error getting UUID for {}", username, exception);
            event.getHook().sendMessage("❌ An error occurred while getting the UUID!").queue();
        }
    }

    private static void handleSkin(SlashCommandInteractionEvent event) {
        final String username = event.getOption("uuid", null, OptionMapping::getAsString);
        if (username == null) {
            event.getHook().sendMessage("❌ You must provide a UUID!").queue();
            return;
        }

        String encodedUsername = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
        try {
            int rotation = 10;
            String url = "https://minecraft-api.com/api/skins/%s/body/10.%d/10/json".formatted(encodedUsername, rotation);
            byte[] bytes = decodeSkin(url);

            final var embed = new EmbedBuilder()
                    .setTimestamp(Instant.now())
                    .setColor(Color.BLUE)
                    .setDescription("The skin for `" + username + "` is:")
                    .setImage("attachment://" + encodedUsername + ".png")
                    .build();

            event.getHook().sendFiles(FileUpload.fromData(bytes, encodedUsername + ".png"))
                    .addEmbeds(embed)
                    .flatMap(msg -> msg.editMessageEmbeds(embed)
                            .setComponents(createButtons(msg.getIdLong(), rotation)))
                    .queue(message -> createEventWaiter(event, message).build());
        } catch (final IllegalStateException exception) {
            Constants.LOGGER.error("Error getting skin for {}", username, exception);
            event.getHook().sendMessage("This player does not exist!").queue();
        }
    }

    private static ActionRow createButtons(long messageId, int rotation) {
        return ActionRow.of(Button.primary(messageId + "-rotate-counter-clockwise-" + rotation, "↩️"),
                Button.primary(messageId + "-dismiss", "🚮"),
                Button.primary(messageId + "-rotate-clockwise-" + rotation, "↪️"));
    }

    private static byte[] decodeSkin(String url) {
        try {
            URLConnection connection = new URI(url).toURL().openConnection();
            final String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            if (response.contains("not found"))
                throw new IllegalArgumentException(response);

            JsonObject json = Constants.GSON.fromJson(response, JsonObject.class);
            return Base64.getDecoder().decode(json.get("skin").getAsString());
        } catch (final IOException | URISyntaxException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static EventWaiter.Builder<ButtonInteractionEvent> createEventWaiter(SlashCommandInteractionEvent event, Message message) {
        return TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                .condition(btnEvent -> btnEvent.getMessageIdLong() == message.getIdLong()
                        && btnEvent.getUser().equals(event.getUser())
                        && btnEvent.getChannelIdLong() == event.getChannel().getIdLong()
                        && btnEvent.getComponentId().startsWith(message.getId() + "-"))
                .timeout(10, TimeUnit.MINUTES)
                .timeoutAction(() -> message.delete().queue())
                .failure(() -> message.delete().queue())
                .success(btnEvent -> {
                    String action = btnEvent.getComponentId().replace(message.getId() + "-", "").trim();
                    if (action.equals("dismiss")) {
                        message.delete().queue();
                        return;
                    }

                    int newRotation = Integer.parseInt(action.substring(action.lastIndexOf("-") + 1));
                    action = action.substring(0, action.lastIndexOf("-"));

                    switch (action) {
                        case "rotate-counter-clockwise" -> onRotate(btnEvent, message, newRotation - 45);
                        case "rotate-clockwise" -> onRotate(btnEvent, message, newRotation + 45);
                    }

                    createEventWaiter(event, message).build();
                });
    }

    private static void onRotate(ButtonInteractionEvent btnEvent, Message message, int rotation) {
        if (rotation <= 0) {
            rotation = 355;
        } else if (rotation >= 360) {
            rotation = 5;
        }

        final MessageEmbed oldEmbed = message.getEmbeds().getFirst();
        final EmbedBuilder newEmbed = new EmbedBuilder(oldEmbed);

        MessageEmbed.ImageInfo image = oldEmbed.getImage();
        if (image == null) {
            btnEvent.deferEdit().queue();
            return;
        }

        String newUrl = image.getProxyUrl();
        if (newUrl == null) {
            btnEvent.deferEdit().queue();
            return;
        }

        final String filename = newUrl.substring(newUrl.lastIndexOf("/")).replace("/", "");
        final String newUsername = filename.split(".png")[0];
        byte[] newBytes = decodeSkin("https://minecraft-api.com/api/skins/%s/body/10.%d/10/json"
                .formatted(newUsername, rotation));

        newEmbed.setImage("attachment://" + newUsername + ".png");
        btnEvent.deferEdit().setAttachments(FileUpload.fromData(newBytes, newUsername + ".png"))
                .setEmbeds(newEmbed.build())
                .setComponents(createButtons(message.getIdLong(), rotation))
                .queue();
    }

    @Override
    public List<SubcommandData> createSubcommandData() {
        return List.of(
                new SubcommandData("username", "Gets the Minecraft Username from a UUID.")
                        .addOption(OptionType.STRING, "uuid", "The UUID used to get this user's name from", true),
                new SubcommandData("uuid", "Gets the Minecraft UUID from a username.")
                        .addOption(OptionType.STRING, "username", "The username of which to get the UUID for", true),
                new SubcommandData("skin", "Gets the Minecraft Skin from a UUID.")
                        .addOption(OptionType.STRING, "uuid", "The UUID used to get this user's skin from", true)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Get a Minecraft player's UUID, username, or skin!";
    }

    @Override
    public String getName() {
        return "minecraft";
    }

    @Override
    public String getRichName() {
        return "Minecraft";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 10L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            reply(event, "❌ You need to specify a subcommand!", false, true);
            return;
        }

        event.deferReply().mentionRepliedUser(false).queue();

        switch (subcommand) {
            case "username" -> handleUsername(event);
            case "uuid" -> handleUUID(event);
            case "skin" -> handleSkin(event);
            default -> event.getHook().sendMessage("❌ You need to specify a valid subcommand!").queue();
        }
    }
}
