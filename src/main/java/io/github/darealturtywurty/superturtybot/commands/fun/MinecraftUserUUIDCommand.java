package io.github.darealturtywurty.superturtybot.commands.fun;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.commons.io.IOUtils;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class MinecraftUserUUIDCommand extends CoreCommand {
    public MinecraftUserUUIDCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "username", "The username used to get the UUID from", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Gets the UUID from a Minecraft User's Name.";
    }

    @Override
    public String getHowToUse() {
        return "/mc-uuid [username]";
    }

    @Override
    public String getName() {
        return "mc-uuid";
    }

    @Override
    public String getRichName() {
        return "Minecraft User UUID";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String username = URLEncoder.encode(event.getOption("username").getAsString().trim(),
            StandardCharsets.UTF_8);
        try {
            final URLConnection connection = new URL("https://minecraft-api.com/api/uuid/" + username).openConnection();
            final String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            if (response.contains("not found"))
                throw new IllegalArgumentException(response);

            event.deferReply()
                .addEmbeds(new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.BLUE)
                    .setDescription(
                        "The UUID for `" + event.getOption("username").getAsString() + "` is: `" + response + "`")
                    .build())
                .mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            event.deferReply(true)
                .setContent("There has been an issue trying to gather this information from our database! "
                    + "This has been reported to the bot owner!")
                .mentionRepliedUser(false).queue();
        } catch (final IllegalArgumentException exception) {
            event.deferReply(true).setContent("This player does not exist!").mentionRepliedUser(false).queue();
        }
    }
}
