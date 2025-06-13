package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.commands;

import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MinecraftVersions;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap.ClassMapping;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap.FieldMapping;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap.MethodMapping;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mojmap.MojmapMappings;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.PistonMetaVersion;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.piston.version.VersionPackage;
import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class SearchCommand extends SubcommandCommand {
    public SearchCommand() {
        super("search", "Search for a mapping");
        addOption(OptionType.STRING, "version", "The minecraft version", true, true);
        addOption(OptionType.STRING, "channel", "The mapping channel to search with", true, true);
        addOption(OptionType.STRING, "mapping", "The mapping to search for", true, true);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!"mappings".equals(event.getName()) && !Objects.equals(event.getSubcommandName(), getName()))
            return;

        if (event.getFocusedOption().getName().equals("version")) {
            List<String> choices = MinecraftVersions.getPistonVersions();
            String focusedValue = event.getFocusedOption().getValue();
            List<String> filteredChoices = choices.stream()
                    .filter(choice -> choice.toLowerCase().startsWith(focusedValue.toLowerCase()))
                    .toList();

            event.replyChoiceStrings(filteredChoices.stream()
                    .limit(25)
                    .toList()).queue();
            return;
        }


    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String versionString = event.getOption("version", null, OptionMapping::getAsString);
        if (versionString == null) {
            reply(event, "❌ Please provide a version", false, true);
            return;
        }

        String channel = event.getOption("channel", null, OptionMapping::getAsString);
        if (!"mojmap".equalsIgnoreCase(channel)) {
            reply(event, "❌ Only Mojmap is supported at the moment", false, true);
            return;
        }

        String mapping = event.getOption("mapping", null, OptionMapping::getAsString);
        if (mapping == null) {
            reply(event, "❌ Please provide a mapping", false, true);
            return;
        }

        event.deferReply().queue();

        PistonMetaVersion version = MinecraftVersions.getVersion(versionString);
        if (version == null) {
            event.getHook().editOriginalFormat("❌ Invalid version %s", versionString).queue();
            return;
        }

        VersionPackage versionPackage = MinecraftVersions.getVersionPackage(version);
        if (versionPackage == null) {
            event.getHook().editOriginalFormat("❌ Invalid version %s", versionString).queue();
            return;
        }

        MojmapMappings mappings = MojmapMappings.getMojmapMappings(versionPackage);
        if (mappings == null) {
            event.getHook().editOriginalFormat("❌ No mappings found for version %s", versionString).queue();
            return;
        }

        ClassMapping classMapping = mappings.findClassMapping(mapping);
        List<MethodMapping> methodMappings = mappings.findMethodMappings(mapping);
        List<FieldMapping> fieldMappings = mappings.findFieldMappings(mapping);

        var response = new StringBuilder();
        if (classMapping != null) {
            response.append("Class Mapping: \n");
            response.append("```\n");
            response.append(classMapping);
            response.append("```");
        }

        if (methodMappings != null && !methodMappings.isEmpty()) {
            response.append("Method Mappings: \n");
            response.append("```\n");
            for (MethodMapping methodMapping : methodMappings) {
                response.append(methodMapping.toString());
            }

            response.append("```");
        }

        if (fieldMappings != null && !fieldMappings.isEmpty()) {
            response.append("Field Mappings: \n");
            response.append("```\n");
            for (FieldMapping fieldMapping : fieldMappings) {
                response.append(fieldMapping.toString());
            }

            response.append("```");
        }

        if (response.isEmpty()) {
            event.getHook().editOriginal("❌ No mappings found").queue();
            return;
        }

        event.getHook().editOriginal(response.toString()).queue();
    }
}
