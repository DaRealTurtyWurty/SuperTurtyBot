package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface PromotionMinigame {
    void start(SlashCommandInteractionEvent event, Economy account);
}
