package dev.darealturtywurty.superturtybot.commands.image;

import java.util.function.BiConsumer;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ImageCommandType implements Registerable {
    private String name;
    private final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> runner;
    
    public ImageCommandType(BiConsumer<SlashCommandInteractionEvent, ImageCommandType> runner) {
        this.runner = runner;
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    public BiConsumer<SlashCommandInteractionEvent, ImageCommandType> getRunner() {
        return this.runner;
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }
}
