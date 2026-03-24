package dev.darealturtywurty.superturtybot.commands.core.notifier;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class NotifierCommand extends CoreCommand {
    public NotifierCommand() {
        super(new Types(true, false, false, false));
        addSubcommands(
                new YoutubeNotifierSubcommand(),
                new TwitchNotifierSubcommand(),
                new SteamNotifierSubcommand(),
                new RedditNotifierSubcommand(),
                new MinecraftNotifierSubcommand(),
                new SiegeNotifierSubcommand(),
                new RocketLeagueNotifierSubcommand(),
                new LeagueNotifierSubcommand(),
                new ValorantNotifierSubcommand()
        );
    }

    @Override
    public String getAccess() {
        return "Moderators Only (Manage Server Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Sets up a notification system for the given social media channel/game/etc";
    }

    @Override
    public String getHowToUse() {
        return "/notifier [social] [whatToListenTo] [discordChannel] [whoToPing]";
    }

    @Override
    public String getName() {
        return "notifier";
    }

    @Override
    public String getRichName() {
        return "Notifier";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, "❌ You must provide a subcommand!", false, true);
    }
}
