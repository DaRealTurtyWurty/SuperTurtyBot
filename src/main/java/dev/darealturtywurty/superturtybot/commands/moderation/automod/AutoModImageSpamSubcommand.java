package dev.darealturtywurty.superturtybot.commands.moderation.automod;

import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class AutoModImageSpamSubcommand extends AutoModSubcommand {
    public AutoModImageSpamSubcommand() {
        super("image_spam", "Configures the new-member image spam autoban");
        addOption(new OptionData(OptionType.BOOLEAN, "enabled", "Whether image spam autoban should be enabled", true));
        addOption(new OptionData(OptionType.INTEGER, "window_seconds", "How long the detection window lasts", false));
        addOption(new OptionData(OptionType.INTEGER, "min_images", "Minimum images per qualifying message", false));
        addOption(new OptionData(OptionType.INTEGER, "new_member_threshold_hours",
                "Only members newer than this many hours are checked", false));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        GuildData config = requireConfig(event);
        if (config == null || event.getGuild() == null)
            return;

        boolean enabled = event.getOption("enabled", false, OptionMapping::getAsBoolean);
        Integer windowSeconds = event.getOption("window_seconds", OptionMapping::getAsInt);
        Integer minImages = event.getOption("min_images", OptionMapping::getAsInt);
        Integer thresholdHours = event.getOption("new_member_threshold_hours", OptionMapping::getAsInt);

        if (windowSeconds != null && windowSeconds < 1) {
            reply(event, "❌ `window_seconds` must be at least 1.", false, true);
            return;
        }

        if (minImages != null && minImages < 1) {
            reply(event, "❌ `min_images` must be at least 1.", false, true);
            return;
        }

        if (thresholdHours != null && thresholdHours < 1) {
            reply(event, "❌ `new_member_threshold_hours` must be at least 1.", false, true);
            return;
        }

        config.setImageSpamAutoBanEnabled(enabled);
        if (windowSeconds != null) {
            config.setImageSpamWindowSeconds(windowSeconds);
        }

        if (minImages != null) {
            config.setImageSpamMinImages(minImages);
        }

        if (thresholdHours != null) {
            config.setImageSpamNewMemberThresholdHours(thresholdHours);
        }

        if (!saveConfig(event.getGuild(), config)) {
            reply(event, "❌ Failed to update image spam settings!", false, true);
            return;
        }

        reply(event, """
                ✅ Updated image spam automod.
                Enabled: `%s`
                Window: `%d` seconds
                Min images: `%d`
                New member threshold: `%d` hours""".formatted(
                enabled,
                config.getImageSpamWindowSeconds(),
                config.getImageSpamMinImages(),
                config.getImageSpamNewMemberThresholdHours()), false, true);
    }
}
