package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.commands.moderation.automod.AutoModImageSpamSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.automod.AutoModInviteGuardSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.automod.AutoModScamDetectionSubcommand;
import dev.darealturtywurty.superturtybot.commands.moderation.automod.AutoModStatusSubcommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class AutoModCommand extends CoreCommand {
    public AutoModCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new AutoModStatusSubcommand(),
                new AutoModInviteGuardSubcommand(),
                new AutoModScamDetectionSubcommand(),
                new AutoModImageSpamSubcommand()
        );
    }

    @Override
    public String getAccess() {
        return "Moderators (Manage Server Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Configures the bot's automatic moderation systems";
    }

    @Override
    public String getHowToUse() {
        return """
                /automod status
                /automod invite_guard <enabled> [clear_whitelist] [channel_1] ... [channel_5]
                /automod scam_detection <enabled>
                /automod image_spam <enabled> [window_seconds] [min_images] [new_member_threshold_hours]""";
    }

    @Override
    public String getName() {
        return "automod";
    }

    @Override
    public String getRichName() {
        return "Auto Moderation";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, "❌ You must provide a valid subcommand!", false, true);
    }
}
