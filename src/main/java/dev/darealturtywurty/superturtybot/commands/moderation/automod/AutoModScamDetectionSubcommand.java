package dev.darealturtywurty.superturtybot.commands.moderation.automod;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AutoModScamDetectionSubcommand extends AutoModSubcommand {
    public AutoModScamDetectionSubcommand() {
        super("scam_detection", "Configures scam domain detection");
        addOption(new OptionData(OptionType.BOOLEAN, "enabled", "Whether scam detection should be enabled", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildData config = requireConfig(event);
        if (config == null || event.getGuild() == null)
            return;

        boolean enabled = event.getOption("enabled", false, OptionMapping::getAsBoolean);
        config.setScamDetectionEnabled(enabled);

        if (!saveConfig(event.getGuild(), config)) {
            reply(event, "❌ Failed to update scam detection settings!", false, true);
            return;
        }

        reply(event, "✅ Scam detection is now `%s`.".formatted(enabled), false, true);
    }
}
