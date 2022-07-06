package io.github.darealturtywurty.superturtybot.commands.fun;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class EightBallCommand extends CoreCommand {
    public EightBallCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List
            .of(new OptionData(OptionType.STRING, "question", "The question that you want to ask the 8 Ball", true));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }
    
    @Override
    public String getDescription() {
        return "This will fortune tell with an answer for your question.";
    }
    
    @Override
    public String getName() {
        return "eightball";
    }
    
    @Override
    public String getRichName() {
        return "8 Ball";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String question = event.getOption("question").getAsString();
        try {
            final URLConnection connection = new URL(
                "https://8ball.delegator.com/magic/JSON/" + URLEncoder.encode(question, StandardCharsets.UTF_8))
                    .openConnection();
            final JsonObject result = Constants.GSON
                .fromJson(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8), JsonObject.class)
                .getAsJsonObject("magic");
            
            final var embed = new EmbedBuilder();
            final Color color = switch (result.get("type").getAsString()) {
                case "affirmitive" -> Color.GREEN;
                case "neutral" -> Color.BLUE;
                case "contrary" -> Color.RED;
                default -> Color.BLACK;
            };
            
            embed.setColor(color);
            embed.setTimestamp(Instant.now());
            embed.setTitle("8 Ball 🎱");
            embed.setDescription("You asked: " + question + "\n My answer: "
                + URLDecoder.decode(result.get("answer").getAsString(), StandardCharsets.UTF_8).replace("€“", ""));
            embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                event.getUser().getEffectiveAvatarUrl());
            event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            event.deferReply(true)
                .setContent("There has been an issue processing this command! Please try again later.")
                .mentionRepliedUser(false).queue();
            Constants.LOGGER.error("There has been an issue with the 8 Ball command:\nException: {}\n{}",
                exception.getMessage(), ExceptionUtils.getStackTrace(exception));
        }
    }
}
