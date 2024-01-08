package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String rawUsername = event.getOption("username", null, OptionMapping::getAsString);
        if (rawUsername == null) {
            reply(event, "You must provide a username!", false, true);
            return;
        }

        final String username = URLEncoder.encode(rawUsername.trim(), StandardCharsets.UTF_8);
        try {
            final URLConnection connection = new URI("https://minecraft-api.com/api/uuid/" + username).toURL().openConnection();
            final String response = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            if (response.contains("not found"))
                throw new IllegalArgumentException(response);

            event.deferReply()
                .addEmbeds(new EmbedBuilder().setTimestamp(Instant.now()).setColor(Color.BLUE)
                    .setDescription(
                        "The UUID for `" + rawUsername + "` is: `" + response + "`")
                    .build())
                .mentionRepliedUser(false).queue();
        } catch (final IOException | URISyntaxException exception) {
            event.deferReply(true)
                .setContent("There has been an issue trying to gather this information from our database! "
                    + "This has been reported to the bot owner!")
                .mentionRepliedUser(false).queue();
            Constants.LOGGER.error("Error getting UUID for " + username, exception);
        } catch (final IllegalArgumentException exception) {
            reply(event, "This player does not exist!", false, true);
        }
    }
}
