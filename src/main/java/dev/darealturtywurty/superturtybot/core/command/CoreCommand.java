package dev.darealturtywurty.superturtybot.core.command;

import dev.darealturtywurty.superturtybot.Environment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// TODO: Rate-limits per subcommand
public abstract class CoreCommand extends ListenerAdapter implements BotCommand {
    protected static final Map<Long, Pair<String, Long>> RATE_LIMITS = new ConcurrentHashMap<>();

    public final Types types;

    protected final List<SubcommandCommand> subcommands = new ArrayList<>();

    // -1 = Global
    private final Map<Long, String> commandIds = new ConcurrentHashMap<>();

    protected CoreCommand(Types types) {
        this.types = types;
    }

    public static void reply(MessageReceivedEvent event, String message) {
        reply(event, message, false);
    }

    public static void reply(MessageReceivedEvent event, String message, boolean mention) {
        event.getMessage().reply(message).mentionRepliedUser(mention).queue();
    }

    public static void reply(GenericCommandInteractionEvent event, EmbedBuilder embed) {
        reply(event, embed, false);
    }

    public static void reply(GenericCommandInteractionEvent event, EmbedBuilder embed, boolean mention) {
        reply(event, embed, mention, false);
    }

    public static void reply(GenericCommandInteractionEvent event, EmbedBuilder embed, boolean mention, boolean ephemeral) {
        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(mention).setEphemeral(ephemeral).queue();
    }

    public static void reply(GenericCommandInteractionEvent event, String message) {
        reply(event, message, false);
    }

    public static void reply(GenericCommandInteractionEvent event, String message, boolean mention) {
        reply(event, message, mention, false);
    }

    public static void reply(GenericCommandInteractionEvent event, String message, boolean mention, boolean ephemeral) {
        event.deferReply().setContent(message).mentionRepliedUser(mention).setEphemeral(ephemeral).queue();
    }

    public static void reply(IReplyCallback event, String message, boolean mention) {
        event.reply(message).mentionRepliedUser(mention).queue();
    }

    public static void reply(IReplyCallback event, String message) {
        reply(event, message, false);
    }

    public static void reply(IReplyCallback event, EmbedBuilder embed, boolean mention) {
        event.replyEmbeds(embed.build()).mentionRepliedUser(mention).queue();
    }

    public static void reply(IReplyCallback event, EmbedBuilder embed) {
        reply(event, embed, false);
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
        this.commandIds.put(guildId, id);
    }

    public String getCommandId(long guildId) {
        return this.commandIds.get(guildId);
    }

    public String getCommandId() {
        return getCommandId(-1L);
    }

    public void setCommandId(String id) {
        setCommandId(-1L, id);
    }

    @Override
    public final List<SubcommandCommand> getSubcommands() {
        return this.subcommands;
    }

    public void addSubcommands(SubcommandCommand... subcommands) {
        Collections.addAll(this.subcommands, subcommands);
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        super.onMessageContextInteraction(event);

        if (!event.getName().equalsIgnoreCase(getRichName()) || !this.types.messageCtx())
            return;

        if (validateRatelimit(event.getUser().getIdLong(),
                end -> event.reply("❌ You are being rate-limited! You can use the command again " + end + "!")
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

        if (validateRatelimit(event.getAuthor().getIdLong(),
                end -> reply(event, "❌ You are being rate-limited! You can use the command again " + end + "!"))) {
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
                end -> event.reply("❌ You are being rate-limited! You can use the command again " + end + "!")
                        .setEphemeral(true).queue())) {
            String subcommand = event.getSubcommandName();
            if (subcommand == null) {
                runSlash(event);
                return;
            }

            this.subcommands.stream()
                    .filter(sub -> sub.getName().equalsIgnoreCase(subcommand))
                    .findFirst()
                    .ifPresentOrElse(
                            sub -> sub.execute(event),
                            () -> runSlash(event)
                    );
        }
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        super.onUserContextInteraction(event);
        if (!event.getName().equalsIgnoreCase(getRichName()) || !this.types.userCtx())
            return;

        if (validateRatelimit(event.getUser().getIdLong(),
                end -> event.reply("❌ You are being rate-limited! You can use the command again " + end + "!")
                        .setEphemeral(true).queue())) {
            runUserCtx(event);
        }
    }

    protected void runGuildMessage(MessageReceivedEvent event) {
        // NO-OP
    }

    protected void runMessageCtx(MessageContextInteractionEvent event) {
        // NO-OP
    }

    protected void runNormalMessage(MessageReceivedEvent event) {
        // NO-OP
    }

    protected void runPrivateMessage(MessageReceivedEvent event) {
        // NO-OP
    }

    protected void runSlash(SlashCommandInteractionEvent event) {
        // NO-OP
    }

    protected void runThreadMessage(MessageReceivedEvent event) {
        // NO-OP
    }

    protected void runUserCtx(UserContextInteractionEvent event) {
        // NO-OP
    }

    private boolean validateRatelimit(long user, Consumer<String> ratelimitResponse) {
        if(user == Environment.INSTANCE.ownerId().orElse(-1L))
            return true;

        Pair<TimeUnit, Long> ratelimit = getRatelimit();
        if (ratelimit.getRight() > 0) {
            long length = TimeUnit.MILLISECONDS.convert(ratelimit.getRight(), ratelimit.getLeft());
            if (RATE_LIMITS.containsKey(user)) {
                Pair<String, Long> pair = RATE_LIMITS.get(user);
                if (pair.getLeft().equalsIgnoreCase(getName())) {
                    long endTime = pair.getRight();
                    long currentTime = System.currentTimeMillis();

                    if (currentTime >= endTime) {
                        RATE_LIMITS.put(user, Pair.of(getName(), System.currentTimeMillis() + length));
                        return true;
                    } else {
                        ratelimitResponse.accept(TimeFormat.RELATIVE.format(endTime));
                        return false;
                    }
                }
            } else {
                RATE_LIMITS.put(user, Pair.of(getName(), System.currentTimeMillis() + length));
                return true;
            }
        }

        return true;
    }

    public record Types(boolean slash, boolean normal, boolean messageCtx, boolean userCtx) {
    }
}
