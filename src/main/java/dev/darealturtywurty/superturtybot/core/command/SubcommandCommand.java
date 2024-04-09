package dev.darealturtywurty.superturtybot.core.command;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.*;

@Getter
public abstract class SubcommandCommand extends ListenerAdapter {
    private final String name;
    private final String description;
    private final List<OptionData> options = new ArrayList<>();

    public SubcommandCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public SubcommandCommand addOption(OptionData option) {
        this.options.add(option);
        return this;
    }

    public SubcommandCommand addOptions(Collection<OptionData> options) {
        this.options.addAll(options);
        return this;
    }

    public SubcommandCommand addOptions(OptionData... options) {
        Collections.addAll(this.options, options);
        return this;
    }

    public SubcommandCommand addOption(OptionType type, String name, String description, boolean required, boolean autocomplete) {
        return addOption(new OptionData(type, name, description, required, autocomplete));
    }

    public SubcommandCommand addOption(OptionType type, String name, String description, boolean required) {
        return addOption(type, name, description, required, false);
    }

    public SubcommandCommand addOption(OptionType type, String name, String description) {
        return addOption(type, name, description,false);
    }

    public abstract void execute(SlashCommandInteractionEvent event);

    protected static void reply(SlashCommandInteractionEvent event, String message, boolean mention, boolean isEphemeral) {
        CoreCommand.reply(event, message, mention, isEphemeral);
    }

    protected static void reply(SlashCommandInteractionEvent event, String message, boolean mention) {
        CoreCommand.reply(event, message, mention);
    }

    protected static void reply(SlashCommandInteractionEvent event, String message) {
        CoreCommand.reply(event, message);
    }

    protected static void reply(SlashCommandInteractionEvent event, EmbedBuilder embed, boolean mention, boolean isEphemeral) {
        CoreCommand.reply(event, embed, mention, isEphemeral);
    }

    protected static void reply(SlashCommandInteractionEvent event, EmbedBuilder embed, boolean mention) {
        CoreCommand.reply(event, embed, mention);
    }

    protected static void reply(SlashCommandInteractionEvent event, EmbedBuilder embed) {
        CoreCommand.reply(event, embed);
    }
}
