package io.github.darealturtywurty.superturtybot.commands.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PingCommand extends CoreCommand {
    public PingCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "address", "The URL/IP address to ping", false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }
    
    @Override
    public String getDescription() {
        return "Gets the ping of the bot";
    }
    
    @Override
    public String getName() {
        return "ping";
    }
    
    @Override
    public String getRichName() {
        return "Ping";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String toPing = event.getOption("address", "", OptionMapping::getAsString).trim();
        if (toPing.isBlank()) {
            event.getJDA().getRestPing()
                .queue(ping -> event
                    .replyFormat("Rest Ping: %sms\nWebsocket Ping: %sms", ping, event.getJDA().getGatewayPing())
                    .mentionRepliedUser(false).queue());
            return;
        }

        event.deferReply().setContent("Pinging `" + toPing + "`...").mentionRepliedUser(false).queue();
        if ("192.168.0.1".equals(toPing.trim())) {
            event.getHook().editOriginal("hehe").queue();
            return;
        }

        try {
            final Process ping = ping(removeOptions(toPing));
            final String[] lines = processToLines(ping);
            String ip;
            String address;
            if (lines[1].contains("[")) {
                ip = lines[1].split("\\[")[1].split("\\]")[0].trim();
                address = removeOptions(toPing.trim());
            } else {
                ip = removeOptions(toPing.trim());
                try {
                    final Process getAddress = nslookup(ip);
                    final String[] lines2 = processToLines(getAddress);
                    final String name = lines2[3].replace("Name:", "").trim();
                    address = name;
                } catch (final IOException exception2) {
                    event.getHook().editOriginal("❌ There was an error pinging this IP!").queue();
                    return;
                }
            }
            
            final int ttl = Integer.parseInt(lines[2].split("TTL=")[1].trim());
            
            final String rtt = lines[10];
            final long minTime = Long.parseLong(rtt.split("\s+")[3].replace("ms,", "").trim());
            final long maxTime = Long.parseLong(rtt.split("\s+")[6].replace("ms,", "").trim());
            final long avrTime = Long.parseLong(rtt.split("\s+")[9].replace("ms", "").trim());
            
            final String result = "Pinged: **" + address + "**[" + ip + "]\n__Response Times__:\n**Minimum**: "
                + minTime + "ms\n**Maximum**: " + maxTime + "ms\n**Average**: " + avrTime + "ms\n__TTL__:\n" + ttl
                + "\n";

            event.getHook().editOriginal(result).queue();
        } catch (final IOException | ArrayIndexOutOfBoundsException exception) {
            event.getHook().editOriginal("❌ There was an error pinging this IP!").queue();
        }
    }
    
    private static Process nslookup(String ip) throws IOException {
        return runCommand("nslookup " + ip);
    }

    private static Process ping(final String toPing) throws IOException {
        return runCommand("ping " + toPing);
    }

    private static String processAsString(final Process process) throws IOException {
        return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    }

    private static String[] processToLines(final Process process) throws IOException {
        final String output = processAsString(process);
        return output.split("\n");
    }

    private static String removeOptions(String command) {
        return command.replace("-", "/").replace("/t", "").replace("/a", "").replace("/n", "").replace("/l", "")
            .replace("/f", "").replace("/I", "").replace("/v", "").replace("/r", "").replace("/s", "").replace("/j", "")
            .replace("/k", "").replace("/w", "").replace("/R", "").replace("/S", "").replace("/4", "").replace("/6", "")
            .replace("/?", "");
    }

    private static Process runCommand(final String command) throws IOException {
        return Runtime.getRuntime().exec(command);
    }
}
