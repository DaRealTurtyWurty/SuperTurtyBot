package io.github.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class HttpDogCommand extends AbstractImageCommand {
    private static final Map<Integer, String> STATUS_CODES = new HashMap<>() {
        private static final long serialVersionUID = 1169231675070322026L;
        {
            put(100, "Continue");
            put(101, "Switching Protocols");
            put(102, "Processing");
            put(200, "OK");
            put(201, "Created");
            put(202, "Accepted");
            put(203, "Non-Authoritative Information");
            put(204, "No Content");
            put(206, "Partial Content");
            put(207, "Multi-Status");
            put(300, "Multiple Choices");
            put(301, "Moved Permanently");
            put(302, "Found");
            put(303, "See Other");
            put(304, "Not Modified");
            put(305, "Use Proxy");
            put(307, "Temporary Redirect");
            put(308, "Permanent Redirect");
            put(400, "Bad Request");
            put(401, "Unauthorized");
            put(402, "Payment Required");
            put(403, "Forbidden");
            put(404, "Not Found");
            put(405, "Method Not Allowed");
            put(406, "Not Acceptable");
            put(407, "Proxy Authentication Required");
            put(408, "Request Timeout");
            put(409, "Conflict");
            put(410, "Gone");
            put(411, "Length Required");
            put(412, "Precondition Failed");
            put(413, "Payload Too Large");
            put(414, "Request-URI Too Long");
            put(415, "Unsupported Media Type");
            put(416, "Request Range Not Satisfiable");
            put(417, "Expectation Failed");
            put(418, "I'm a teapot");
            put(420, "Enhance Your Calm");
            put(421, "Misdirected Response");
            put(422, "Unprocessable Entity");
            put(423, "Locked");
            put(424, "Failed Dependency");
            put(425, "Too Early");
            put(426, "Upgrade Required");
            put(429, "Too Many Requests");
            put(431, "Request Header Fields Too Large");
            put(444, "No Response");
            put(450, "Blocked by Windows Parental Controls");
            put(451, "Unavailable For Legal Reasons");
            put(497, "HTTP Request Sent to HTTPS Port");
            put(498, "Token expired/invalid");
            put(499, "Client Closed Request");
            put(500, "Internal Server Error");
            put(501, "Not Implemented");
            put(502, "Bad Gateway");
            put(503, "Service Unavailable");
            put(504, "Gateway Timeout");
            put(506, "Variant Also Negotiates");
            put(507, "Insufficient Storage");
            put(508, "Loop Detected");
            put(509, "Bandwidth Limit Exceeded");
            put(510, "Not Extended");
            put(511, "Network Authentication Required");
            put(521, "Web Server Is Down");
            put(523, "Origin Is Unreachable");
            put(525, "SSL Handshake Failed");
            put(599, "Network Connect Timeout Error");
        }
    };

    public HttpDogCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.INTEGER, "status_code", "The HTTP status code", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }

    @Override
    public String getDescription() {
        return "Gets a dog image for the corresponding http status code.";
    }

    @Override
    public String getHowToUse() {
        return "/httpdog [statusCode]";
    }

    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.FUN;
    }

    @Override
    public String getName() {
        return "httpdog";
    }

    @Override
    public String getRichName() {
        return "HTTP Dog";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        int statusCode = event.getOption("status_code").getAsInt();
        if (!STATUS_CODES.containsKey(statusCode)) {
            statusCode = 404;
        }

        try {
            final URLConnection connection = new URL("https://http.dog/" + statusCode + ".jpg").openConnection();
            event.deferReply().addFile(connection.getInputStream(), statusCode + ".jpg").mentionRepliedUser(false)
                .queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("�?� There was an issue getting this HTTP Dog!").mentionRepliedUser(false)
                .queue();
        }
    }
}
