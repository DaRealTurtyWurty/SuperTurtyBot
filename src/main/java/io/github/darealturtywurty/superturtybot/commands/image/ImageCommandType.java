package io.github.darealturtywurty.superturtybot.commands.image;

import java.util.function.BiConsumer;

import io.github.darealturtywurty.superturtybot.commands.image.AbstractImageCommand.ImageCategory;
import io.github.darealturtywurty.superturtybot.registry.Registerable;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ImageCommandType implements Registerable {
    private String name;
    private final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> runner;
    private final ImageCategory category;

    public ImageCommandType(BiConsumer<SlashCommandInteractionEvent, ImageCommandType> runner, ImageCategory category) {
        this.runner = runner;
        this.category = category;
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
