package io.github.darealturtywurty.superturtybot.core.command;

import io.github.darealturtywurty.superturtybot.Environment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public abstract class CoreCommand extends ListenerAdapter implements BotCommand {
    public final Types types;

    protected CoreCommand(Types types) {
        this.types = types;
    }

    public String getAccess() {
        return "Everyone";
    }

    public String getHowToUse() {
        return (this.types.slash() ? "/" : ".") + getName();
    }
    
    public boolean isServerOnly() {
        return false;
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        super.onMessageContextInteraction(event);

        if (!event.getName().equalsIgnoreCase(getRichName()) || !this.types.messageCtx()
            || event.isFromGuild() && event.getGuild().getIdLong() == 621352915034177566L)
            return;

        runMessageCtx(event);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);

        // TODO: Check for guild prefix if it is from a guild
        final String content = event.getMessage().getContentRaw().toLowerCase();
        if (event.isWebhookMessage() || event.getAuthor().isBot()
            || !content.startsWith((Environment.INSTANCE.defaultPrefix() + getName() + " ").toLowerCase())
                && !(Environment.INSTANCE.defaultPrefix() + getName()).equals(content))
            return;

        if (!this.types.normal() || event.isFromGuild() && event.getGuild().getIdLong() == 621352915034177566L)
            return;

        runNormalMessage(event);

        if (event.isFromGuild()) {
            if (event.isFromThread()) {
                runThreadMessage(event);
                return;
            }

            runGuildMessage(event);
            return;
        }

        runPrivateMessage(event);
    }

    @Override
    public final void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);
        if (!event.getName().equalsIgnoreCase(getName()) || !this.types.slash()
            || event.isFromGuild() && event.getGuild().getIdLong() == 621352915034177566L
                && event.getUser().getIdLong() != Environment.INSTANCE.ownerId())
            return;

        runSlash(event);
    }

    @Override
    public void onUserContextInteraction(UserContextInteractionEvent event) {
        super.onUserContextInteraction(event);
        if (!event.getName().equalsIgnoreCase(getRichName()) || !this.types.userCtx()
            || event.isFromGuild() && event.getGuild().getIdLong() == 621352915034177566L)
            return;

        runUserCtx(event);
    }

    protected void runGuildMessage(MessageReceivedEvent event) {

    }

    protected void runMessageCtx(MessageContextInteractionEvent event) {

    }

    protected void runNormalMessage(MessageReceivedEvent event) {

    }

    protected void runPrivateMessage(MessageReceivedEvent event) {

    }

    protected void runSlash(SlashCommandInteractionEvent event) {

    }

    protected void runThreadMessage(MessageReceivedEvent event) {

    }

    protected void runUserCtx(UserContextInteractionEvent event) {

    }

    protected static void reply(MessageReceivedEvent event, EmbedBuilder embed) {
        reply(event, embed, false);
    }

    protected static void reply(MessageReceivedEvent event, EmbedBuilder embed, boolean mention) {
        event.getMessage().replyEmbeds(embed.build()).mentionRepliedUser(mention).queue();
    }

    protected static void reply(MessageReceivedEvent event, String message) {
        reply(event, message, false);
    }

    protected static void reply(MessageReceivedEvent event, String message, boolean mention) {
        event.getMessage().reply(message).mentionRepliedUser(mention).queue();
    }

    protected static void reply(SlashCommandInteractionEvent event, EmbedBuilder embed) {
        reply(event, embed, false);
    }

    protected static void reply(SlashCommandInteractionEvent event, EmbedBuilder embed, boolean mention) {
        reply(event, embed, mention, false);
    }

    protected static void reply(SlashCommandInteractionEvent event, EmbedBuilder embed, boolean mention,
        boolean defer) {
        event.deferReply(defer).addEmbeds(embed.build()).mentionRepliedUser(mention).queue();
    }

    protected static void reply(SlashCommandInteractionEvent event, String message) {
        reply(event, message, false);
    }

    protected static void reply(SlashCommandInteractionEvent event, String message, boolean mention) {
        reply(event, message, mention, false);
    }

    protected static void reply(SlashCommandInteractionEvent event, String message, boolean mention, boolean defer) {
        event.deferReply(defer).setContent(message).mentionRepliedUser(mention).queue();
    }

    public record Types(boolean slash, boolean normal, boolean messageCtx, boolean userCtx) {
    }
}
