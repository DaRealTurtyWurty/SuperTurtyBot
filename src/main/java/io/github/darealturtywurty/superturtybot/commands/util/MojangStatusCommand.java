package io.github.darealturtywurty.superturtybot.commands.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class MojangStatusCommand extends CoreCommand {
    public MojangStatusCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Retrives the status of the mojang services";
    }
    
    @Override
    public String getName() {
        return "mojangstatus";
    }
    
    @Override
    public String getRichName() {
        return "Mojang Status";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();
        final EmbedBuilder embed = getStatusEmbed();
        if (embed == null) {
            event.deferReply(true)
                .setContent("There was an issue with this command! Please notify the bot owner of this issue!")
                .mentionRepliedUser(true).queue();
            return;
        }
        
        embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
            event.isFromGuild() ? event.getMember().getEffectiveAvatarUrl() : event.getUser().getEffectiveAvatarUrl());
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
    
    @Nullable
    private static EmbedBuilder getStatusEmbed() {
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setTitle("Mojang Status");
        final Map<String, String> siteStatus = new HashMap<>();
        try {
            final URLConnection connection = new URL("https://mojan.ga/api/check").openConnection();
            final String result = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            final JsonArray jsonResult = Constants.GSON.fromJson(result, JsonArray.class);
            for (final JsonElement siteElement : jsonResult) {
                final JsonObject siteObj = siteElement.getAsJsonObject();
                final String site = siteObj.keySet().stream().findFirst().get();
                final String status = siteObj.get(site).getAsString();
                siteStatus.put(site, status);
            }
            
            connection.getInputStream().close();
        } catch (final IOException exception) {
            Constants.LOGGER.error(
                "There seems to have been an error with the Mojang Status Api! (https://mojan.ga/api/check)",
                exception);
            return null;
        }
        
        siteStatus.entrySet().stream().sorted((entry1, entry2) -> {
            if ("green".equalsIgnoreCase(entry1.getValue()) && "green".equalsIgnoreCase(entry2.getValue())
                || "yellow".equalsIgnoreCase(entry1.getValue()) && "yellow".equalsIgnoreCase(entry2.getValue()))
                return entry1.getKey().compareTo(entry2.getKey());
            
            if ("red".equalsIgnoreCase(entry1.getValue()) && "red".equalsIgnoreCase(entry2.getValue()))
                return entry1.getKey().compareTo(entry2.getKey());
            
            if ("green".equalsIgnoreCase(entry1.getValue()) && "yellow".equalsIgnoreCase(entry2.getValue())
                || "red".equalsIgnoreCase(entry2.getValue())
                || "yellow".equalsIgnoreCase(entry1.getValue()) && "red".equalsIgnoreCase(entry2.getValue()))
                return -1;
            
            return 1;
        }).forEachOrdered(entry -> embed.addField(entry.getKey(), entry.getValue().replace("green", "🟩 No issues 🟩")
            .replace("yellow", "🟨 Issues reported 🟨").replace("red", "🟥 Service unavailable 🟥"), false));
        
        return embed;
    }
}
