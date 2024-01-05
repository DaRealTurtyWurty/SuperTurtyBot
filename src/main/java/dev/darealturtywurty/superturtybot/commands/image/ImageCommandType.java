package dev.darealturtywurty.superturtybot.commands.image;

import dev.darealturtywurty.superturtybot.registry.Registerable;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.function.BiConsumer;

public class ImageCommandType implements Registerable {
    private String name;
    @Getter
    private final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> runner;
    
    public ImageCommandType(BiConsumer<SlashCommandInteractionEvent, ImageCommandType> runner) {
        this.runner = runner;
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Registerable setName(String name) {
        this.name = name;
        return this;
    }
}
