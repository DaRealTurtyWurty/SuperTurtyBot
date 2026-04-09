package dev.darealturtywurty.superturtybot.commands.moderation.ticket;

import dev.darealturtywurty.superturtybot.modules.modmail.ModmailManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.concurrent.CompletableFuture;

public class ModmailCreateSubcommand extends ModmailSubcommand {
    public ModmailCreateSubcommand() {
        super("create", "Open a new modmail ticket");
        addOption(OptionType.STRING, "message", "The message that opens your ticket", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = requireGuildMember(event);
        if (member == null)
            return;

        String message = event.getOption("message", "", OptionMapping::getAsString).trim();
        if (message.isBlank()) {
            reply(event, "❌ You must provide a message to open a ticket!", false, true);
            return;
        }

        if (ModmailManager.getConfiguredModeratorRoles(member.getGuild()).isEmpty()) {
            reply(event, ModmailManager.missingConfigurationMessage(), false, true);
            return;
        }

        event.deferReply(true).queue();
        CompletableFuture.supplyAsync(() -> ModmailManager.createTicket(member, message, ModmailManager.TICKET_SOURCE_SLASH_COMMAND))
                .thenAccept(result -> event.getHook()
                        .editOriginal("✅ Ticket created: " + result.channel().getAsMention())
                        .queue())
                .exceptionally(throwable -> {
                    event.getHook().editOriginal(resolveError(throwable)).queue();
                    return null;
                });
    }

    private static String resolveError(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        if (cause instanceof ModmailManager.ModmailException)
            return cause.getMessage();

        return "❌ Failed to create the ticket.";
    }
}
