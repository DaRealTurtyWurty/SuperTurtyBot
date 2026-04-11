package dev.darealturtywurty.superturtybot.commands.moderation.ticket;

import dev.darealturtywurty.superturtybot.database.pojos.collections.ModmailTicket;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.modules.modmail.ModmailManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModmailCloseSubcommand extends ModmailSubcommand {
    public ModmailCloseSubcommand() {
        super("close", "Close the current ticket and archive its transcript");
        addOption(reasonOption(false, "Why this ticket is being closed"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!validateModerator(event))
            return;

        Member member = event.getMember();
        if (member == null || event.getGuild() == null || event.getChannelType() != ChannelType.TEXT) {
            reply(event, "❌ This subcommand must be run inside a ticket text channel.", false, true);
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        Optional<ModmailTicket> ticket = ModmailManager.getOpenTicketByChannel(event.getGuild().getIdLong(), channel.getIdLong());
        if (ticket.isEmpty()) {
            reply(event, "❌ This channel is not an active ticket.", false, true);
            return;
        }

        String reason = event.getOption("reason", "", OptionMapping::getAsString).trim();
        event.deferReply(true).queue();
        CompletableFuture.supplyAsync(() -> ModmailManager.closeTicket(ticket.get(), member, reason))
                .thenAccept(result -> event.getHook()
                        .editOriginal("✅ Ticket #" + result.ticket().getTicketNumber() + " closed. Archived " + result.archivedMessageCount() + " messages.")
                        .queue(
                                success -> ModmailManager.deleteTicketChannel(member.getGuild(), result.ticket()),
                                failure -> ModmailManager.deleteTicketChannel(member.getGuild(), result.ticket())
                        ))
                .exceptionally(throwable -> {
                    Constants.LOGGER.error("Failed to close modmail ticket in guild {} channel {}",
                            event.getGuild().getId(),
                            event.getChannel().getId(),
                            throwable);
                    event.getHook().editOriginal(resolveError(throwable)).queue();
                    return null;
                });
    }

    private static String resolveError(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ModmailManager.ModmailException)
            return cause.getMessage();

        String message = cause.getMessage();
        if (message != null && !message.isBlank())
            return "❌ Failed to close the ticket: " + message;

        return "❌ Failed to close the ticket.";
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current;
    }
}
