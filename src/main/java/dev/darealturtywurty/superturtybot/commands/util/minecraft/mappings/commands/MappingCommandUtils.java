package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.commands;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingChannel;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public final class MappingCommandUtils {
    private MappingCommandUtils() {
    }

    public static OptionData channelOption(String name, String description) {
        var option = new OptionData(OptionType.STRING, name, description, true);
        for (MappingChannel channel : MappingChannel.values()) {
            option.addChoice(channel.displayName(), channel.name().toLowerCase(Locale.ROOT));
        }

        return option;
    }

    public static void sendResponse(InteractionHook hook, String header, String body) {
        if (header.length() + body.length() + 10 <= 2000 && !body.contains("```")) {
            hook.editOriginal(header + "\n```\n" + body + "```").setAllowedMentions(List.of()).queue();
        } else {
            hook.editOriginal(header)
                .setAttachments(FileUpload.fromData(body.getBytes(StandardCharsets.UTF_8), "mappings.txt"))
                .setAllowedMentions(List.of())
                .queue();
        }
    }

    public static void reportError(InteractionHook hook, Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) {
            error = error.getCause();
        }

        if (error instanceof IllegalArgumentException) {
            hook.editOriginal("❌ " + error.getMessage()).setAllowedMentions(List.of()).queue();
        } else {
            Constants.LOGGER.error("Failed to look up Minecraft mappings", error);
            hook.editOriginal("❌ Could not load mappings right now. Please try again shortly.").queue();
        }
    }
}
