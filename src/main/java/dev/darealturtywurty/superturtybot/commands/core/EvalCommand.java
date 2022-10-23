package dev.darealturtywurty.superturtybot.commands.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.JSEvaluator;
import dev.darealturtywurty.superturtybot.core.util.ProxyFactory;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class EvalCommand extends CoreCommand {
    public EvalCommand() {
        super(new Types(true, false, true, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "javascript", "The JavaScript code to evaluate.", true));
    }

    @Override
    public String getAccess() {
        return "Bot Owner";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "Evaluates a javascript expression.";
    }

    @Override
    public String getHowToUse() {
        return "/eval [expression]";
    }

    @Override
    public String getName() {
        return "eval";
    }
    
    @Override
    public String getRichName() {
        return "Evaluate";
    }

    @Override
    protected void runMessageCtx(MessageContextInteractionEvent event) {
        if (event.getUser().getIdLong() != Environment.INSTANCE.ownerId()) {
            event.deferReply(true).setContent("❌ You do not have permission to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final String content = event.getTarget().getContentDisplay();
        final Context ctx = JSEvaluator.getContext();
        final Value jsFunc = ctx.eval("js", content);
        event.deferReply().setContent(jsFunc.asString()).mentionRepliedUser(false).queue();
        ctx.close();
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (event.getUser().getIdLong() != Environment.INSTANCE.ownerId()) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return;
        }

        final String content = event.getOption("javascript").getAsString();
        try {
            final Map<String, Object> bindings = new HashMap<>();
            bindings.put("channel", ProxyFactory.toProxy(event.getChannel()));
            bindings.put("guild", ProxyFactory.toProxy(event.getGuild()));
            bindings.put("channelType", ProxyFactory.toProxy(event.getChannelType()));
            bindings.put("member", ProxyFactory.toProxy(event.getMember()));
            bindings.put("commandId", event.getCommandIdLong());
            bindings.put("commandPath", event.getCommandPath());
            bindings.put("commandString", event.getCommandString());
            bindings.put("commandType", ProxyFactory.toProxy(event.getCommandType()));
            bindings.put("guildLocale", ProxyFactory.toProxy(event.getGuildLocale()));
            bindings.put("hook", ProxyFactory.toProxy(event.getHook()));
            bindings.put("id", event.getIdLong());
            bindings.put("interaction", ProxyFactory.toProxy(event.getInteraction()));
            bindings.put("name", event.getName());
            bindings.put("options", ProxyFactory.toProxy(event.getOptions()));
            bindings.put("subcommandGroup", event.getSubcommandGroup());
            bindings.put("subcommandName", event.getSubcommandName());
            bindings.put("type", ProxyFactory.toProxy(event.getType()));
            bindings.put("token", ProxyFactory.toProxy(event.getToken()));
            bindings.put("timeCreated", ProxyFactory.toProxy(event.getTimeCreated()));
            bindings.put("user", ProxyFactory.toProxy(event.getUser()));
            bindings.put("userLocale", ProxyFactory.toProxy(event.getUserLocale()));
            bindings.put("isFromGuild", ProxyFactory.function(args -> event.isFromGuild()));
            bindings.put("isAcknowledged", ProxyFactory.function(args -> event.isAcknowledged()));

            createContext(event, content, bindings);
        } catch (final Exception exception) {
            final String message = exception.getMessage();
            event.deferReply().setContent(message.length() > 1996 ? message.substring(0, 1996) + " ..." : message)
                .mentionRepliedUser(false).queue();
        }
    }
    
    private static void createContext(SlashCommandInteractionEvent event, final String content,
        final Map<String, Object> bindings) {
        try {
            final Context ctx = JSEvaluator.getContext(bindings);
            final Value jsFunc = ctx.eval("js", content);
            event.deferReply().setContent(jsFunc.asString()).mentionRepliedUser(false).queue();
            ctx.close();
        } catch (final ClassCastException exception) {
            if (exception.getMessage()
                .contains("You can ensure that the value can be converted using Value.isString()")) {
                final Context ctx = JSEvaluator.getContext(bindings);
                final Value jsFunc = ctx.eval("js", content + ".toString()");
                event.deferReply().setContent(jsFunc.asString()).mentionRepliedUser(false).queue();
                ctx.close();
            }
        }
    }
}
