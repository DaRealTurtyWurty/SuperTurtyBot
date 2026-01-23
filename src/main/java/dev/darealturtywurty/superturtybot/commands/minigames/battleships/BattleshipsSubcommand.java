package dev.darealturtywurty.superturtybot.commands.minigames.battleships;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;

public abstract class BattleshipsSubcommand extends SubcommandCommand {
    private final boolean ephemeral;

    protected BattleshipsSubcommand(String name, String description, boolean ephemeral) {
        super(name, description);
        this.ephemeral = ephemeral;
    }

    @Override
    public final void execute(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            reply(event, "❌ This command can only be used in a server.", false, true);
            return;
        }

        reply(event, "⏳ Processing", false, this.ephemeral);
        executeSubcommand(event);
    }

    protected abstract void executeSubcommand(SlashCommandInteractionEvent event);

    protected static WebhookMessageEditAction<Message> replyBattleships(SlashCommandInteractionEvent event, String message, boolean mention) {
        return event.getHook().editOriginal(message).mentionRepliedUser(mention);
    }

    protected static WebhookMessageEditAction<Message> replyBattleships(SlashCommandInteractionEvent event, String message) {
        return replyBattleships(event, message, false);
    }

    protected static WebhookMessageEditAction<Message> replyBattleships(SlashCommandInteractionEvent event, EmbedBuilder embed, boolean mention) {
        return event.getHook().editOriginalEmbeds(embed.build()).mentionRepliedUser(mention);
    }

    protected static WebhookMessageEditAction<Message> replyBattleships(SlashCommandInteractionEvent event, EmbedBuilder embed) {
        return replyBattleships(event, embed, false);
    }

    protected static String resolveDisplayName(SlashCommandInteractionEvent event, long userId, String fallback) {
        if (event.getGuild() != null) {
            var member = event.getGuild().getMemberById(userId);
            if (member != null)
                return member.getEffectiveName();
        }

        User user = event.getJDA().getUserById(userId);
        if (user != null)
            return user.getName();

        return fallback;
    }
}
