package dev.darealturtywurty.superturtybot.commands.economy;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class EconomyResetCommand extends EconomyCommand {
    public EconomyResetCommand() {
        super(new Types(false, true, false, false));
    }

    @Override
    public String getDescription() {
        return "Reset the economy for this server (server owner only).";
    }

    @Override
    public String getName() {
        return "economyreset";
    }

    @Override
    public String getRichName() {
        return "Economy Reset";
    }

    @Override
    public String getAccess() {
        return "Server Owner";
    }

    @Override
    protected void runNormalMessage(MessageReceivedEvent event, Guild guild, GuildData config) {
        if (guild.getOwnerIdLong() != event.getAuthor().getIdLong()) {
            reply(event, "❌ Only the server owner can reset the economy.", false);
            return;
        }

        event.getMessage().reply("⚠️ This will delete all economy accounts for this server. Are you sure?")
                .setComponents(ActionRow.of(
                        Button.danger("economyreset:confirm", "Confirm Reset"),
                        Button.secondary("economyreset:cancel", "Cancel")))
                .mentionRepliedUser(false)
                .queue(message -> TurtyBot.EVENT_WAITER.builder(ButtonInteractionEvent.class)
                        .timeout(2, TimeUnit.MINUTES)
                        .timeoutAction(() -> message.editMessage("❌ Economy reset timed out.").setComponents().queue())
                        .failure(() -> message.editMessage("❌ An error occurred while resetting the economy.").setComponents().queue())
                        .condition(buttonEvent -> buttonEvent.isFromGuild()
                                && Objects.requireNonNull(buttonEvent.getGuild()).getIdLong() == guild.getIdLong()
                                && buttonEvent.getUser().getIdLong() == event.getAuthor().getIdLong()
                                && buttonEvent.getMessageIdLong() == message.getIdLong()
                                && buttonEvent.getComponentId().startsWith("economyreset:"))
                        .success(buttonEvent -> {
                            buttonEvent.deferEdit().queue();
                            if (buttonEvent.getComponentId().equals("economyreset:cancel")) {
                                message.editMessage("❌ Economy reset cancelled.").setComponents().queue();
                                return;
                            }

                            Database.getDatabase().economy.deleteMany(Filters.eq("guild", guild.getIdLong()));
                            config.setEndOfDayIncomeTax(new HashMap<>());
                            Database.getDatabase().guildData.updateOne(Filters.eq("guild", guild.getIdLong()),
                                    Updates.set("endOfDayIncomeTax", config.getEndOfDayIncomeTax()));

                            message.editMessage("✅ Economy has been reset for this server.").setComponents().queue();
                        })
                        .build());
    }
}
