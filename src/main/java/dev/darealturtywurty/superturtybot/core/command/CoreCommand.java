package dev.darealturtywurty.superturtybot.core.command;

import dev.darealturtywurty.superturtybot.Environment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class CoreCommand extends ListenerAdapter implements BotCommand {
    protected static final Map<Long, Pair<String, Long>> RATELIMITS = new ConcurrentHashMap<>();

    public final Types types;

    // -1 = Global
    private final Map<Long, String> commandIds = new ConcurrentHashMap<>();

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

    final boolean isNotServerOnly() {
        return !isServerOnly();
    }

    public void setCommandId(long guildId, String id) {
        if (this.commandIds.containsKey(guildId))
            throw new IllegalStateException("Command ID already set!");

        this.commandIds.put(guildId, id);
    }

    public void setCommandId(String id) {
        setCommandId(-1L, id);
    }

    public String getCommandId(long guildId) {
        return this.commandIds.get(guildId);
    }

    public String getCommandId() {
        return getCommandId(-1L);
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        super.onMessageContextInteraction(event);

        if (!event.getName().equalsIgnoreCase(getRichName()) || !this.types.messageCtx())
            return;

        if (validateRatelimit(event.getUser().getIdLong(),
                end -> event.reply("❌ You are being ratelimited! You can use the command again " + end + "!")
                        .setEphemeral(true).queue())) {
            runMessageCtx(event);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);

        final String content = event.getMessage().getContentRaw().toLowerCase();
        if (event.isWebhookMessage() || event.getAuthor().isBot()
            || !content.startsWith((Environment.INSTANCE.defaultPrefix().orElse("") + getName() + " ").toLowerCase())
                && !(Environment.INSTANCE.defaultPrefix().orElse("") + getName()).equals(content))
            return;

        if (!this.types.normal())
            return;

        if(validateRatelimit(event.getAuthor().getIdLong(),
                end -> reply(event, "❌ You are being ratelimited! You can use the command again " + end + "!"))) {
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
    }

    @Override
    public final void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);
        if (!event.getName().equalsIgnoreCase(getName()) || !this.types.slash())
            return;

        if (validateRatelimit(event.getUser().getIdLong(),
                end -> event.reply("❌ You are being ratelimited! You can use the command again " + end + "!")
                        .setEphemeral(true).queue())) {
            runSlash(event);
        }
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        super.onUserContextInteraction(event);
        if (!event.getName().equalsIgnoreCase(getRichName()) || !this.types.userCtx())
            return;

        if (validateRatelimit(event.getUser().getIdLong(),
                end -> event.reply("❌ You are being ratelimited! You can use the command again " + end + "!")
                        .setEphemeral(true).queue())) {
            runUserCtx(event);
        }
    }

    protected void runGuildMessage(MessageReceivedEvent event) {}

    protected void runMessageCtx(MessageContextInteractionEvent event) {}

    protected void runNormalMessage(MessageReceivedEvent event) {}

    protected void runPrivateMessage(MessageReceivedEvent event) {}

    protected void runSlash(SlashCommandInteractionEvent event) {}

    protected void runThreadMessage(MessageReceivedEvent event) {}

    protected void runUserCtx(UserContextInteractionEvent event) {}

    private boolean validateRatelimit(long user, Consumer<String> ratelimitResponse) {
        Pair<TimeUnit, Long> ratelimit = getRatelimit();
        if(ratelimit.getRight() > 0) {
            long length = TimeUnit.MILLISECONDS.convert(ratelimit.getRight(), ratelimit.getLeft());
            if(RATELIMITS.containsKey(user)) {
                Pair<String, Long> pair = RATELIMITS.get(user);
                if(pair.getLeft().equalsIgnoreCase(getName())) {
                    long endTime = pair.getRight();
                    long currentTime = System.currentTimeMillis();

                    if(currentTime >= endTime) {
                        RATELIMITS.put(user, Pair.of(getName(), System.currentTimeMillis() + length));
                        return true;
                    } else {
                        ratelimitResponse.accept(TimeFormat.RELATIVE.format(endTime));
                        return false;
                    }
                }
            } else {
                RATELIMITS.put(user, Pair.of(getName(), System.currentTimeMillis() + length));
                return true;
            }
        }

        return true;
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
