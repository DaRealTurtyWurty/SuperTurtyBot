package dev.darealturtywurty.superturtybot.modules.economy;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class JobCommand extends EconomyCommand {

    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(new SubcommandData("work", "Work for your job"),
                new SubcommandData("register", "Register for a job").addOptions(
                        new OptionData(OptionType.STRING, "job", "The job you want to register for",
                                true).setAutoComplete(true)), new SubcommandData("quit", "Quit your job"),
                new SubcommandData("profile", "View your job profile"));
    }

    @Override
    public String getDescription() {
        return "Manage your job";
    }

    @Override
    public String getName() {
        return "job";
    }

    @Override
    public String getRichName() {
        return "Job";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if(!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }

        String subcommand = event.getSubcommandName();
        if(subcommand == null) {
            reply(event, "❌ You must specify a subcommand!", false, true);
            return;
        }

        Economy account = EconomyManager.fetchAccount(event.getGuild(), event.getUser());

        switch (subcommand) {
            case "work" -> {
                if(!EconomyManager.hasJob(account)) {
                    reply(event, "❌ You must have a job to work!", false, true);
                    return;
                }

                if(EconomyManager.canWork(account)) {
                    String timestamp = convertToTimestamp(account.getNextWork());
                    reply(event, "❌ You can start working " + timestamp + "!", false, true);
                    return;
                }

                EconomyManager.work(account);

            }
        }
    }

    private static String convertToTimestamp(long millis) {
        return "<t:%d:R>".formatted(millis);
    }
}
