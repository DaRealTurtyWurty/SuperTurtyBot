package dev.darealturtywurty.superturtybot.commands.image;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

public class HttpDogCommand extends AbstractImageCommand {
    private static final Map<Integer, String> STATUS_CODES = new HashMap<>();
    static {
        STATUS_CODES.put(100, "Continue");
        STATUS_CODES.put(101, "Switching Protocols");
        STATUS_CODES.put(102, "Processing");
        STATUS_CODES.put(200, "OK");
        STATUS_CODES.put(201, "Created");
        STATUS_CODES.put(202, "Accepted");
        STATUS_CODES.put(203, "Non-Authoritative Information");
        STATUS_CODES.put(204, "No Content");
        STATUS_CODES.put(206, "Partial Content");
        STATUS_CODES.put(207, "Multi-Status");
        STATUS_CODES.put(300, "Multiple Choices");
        STATUS_CODES.put(301, "Moved Permanently");
        STATUS_CODES.put(302, "Found");
        STATUS_CODES.put(303, "See Other");
        STATUS_CODES.put(304, "Not Modified");
        STATUS_CODES.put(305, "Use Proxy");
        STATUS_CODES.put(307, "Temporary Redirect");
        STATUS_CODES.put(308, "Permanent Redirect");
        STATUS_CODES.put(400, "Bad Request");
        STATUS_CODES.put(401, "Unauthorized");
        STATUS_CODES.put(402, "Payment Required");
        STATUS_CODES.put(403, "Forbidden");
        STATUS_CODES.put(404, "Not Found");
        STATUS_CODES.put(405, "Method Not Allowed");
        STATUS_CODES.put(406, "Not Acceptable");
        STATUS_CODES.put(407, "Proxy Authentication Required");
        STATUS_CODES.put(408, "Request Timeout");
        STATUS_CODES.put(409, "Conflict");
        STATUS_CODES.put(410, "Gone");
        STATUS_CODES.put(411, "Length Required");
        STATUS_CODES.put(412, "Precondition Failed");
        STATUS_CODES.put(413, "Payload Too Large");
        STATUS_CODES.put(414, "Request-URI Too Long");
        STATUS_CODES.put(415, "Unsupported Media Type");
        STATUS_CODES.put(416, "Request Range Not Satisfiable");
        STATUS_CODES.put(417, "Expectation Failed");
        STATUS_CODES.put(418, "I'm a teapot");
        STATUS_CODES.put(420, "Enhance Your Calm");
        STATUS_CODES.put(421, "Misdirected Response");
        STATUS_CODES.put(422, "Unprocessable Entity");
        STATUS_CODES.put(423, "Locked");
        STATUS_CODES.put(424, "Failed Dependency");
        STATUS_CODES.put(425, "Too Early");
        STATUS_CODES.put(426, "Upgrade Required");
        STATUS_CODES.put(429, "Too Many Requests");
        STATUS_CODES.put(431, "Request Header Fields Too Large");
        STATUS_CODES.put(444, "No Response");
        STATUS_CODES.put(450, "Blocked by Windows Parental Controls");
        STATUS_CODES.put(451, "Unavailable For Legal Reasons");
        STATUS_CODES.put(497, "HTTP Request Sent to HTTPS Port");
        STATUS_CODES.put(498, "Token expired/invalid");
        STATUS_CODES.put(499, "Client Closed Request");
        STATUS_CODES.put(500, "Internal Server Error");
        STATUS_CODES.put(501, "Not Implemented");
        STATUS_CODES.put(502, "Bad Gateway");
        STATUS_CODES.put(503, "Service Unavailable");
        STATUS_CODES.put(504, "Gateway Timeout");
        STATUS_CODES.put(506, "Variant Also Negotiates");
        STATUS_CODES.put(507, "Insufficient Storage");
        STATUS_CODES.put(508, "Loop Detected");
        STATUS_CODES.put(509, "Bandwidth Limit Exceeded");
        STATUS_CODES.put(510, "Not Extended");
        STATUS_CODES.put(511, "Network Authentication Required");
        STATUS_CODES.put(521, "Web Server Is Down");
        STATUS_CODES.put(523, "Origin Is Unreachable");
        STATUS_CODES.put(525, "SSL Handshake Failed");
        STATUS_CODES.put(599, "Network Connect Timeout Error");
    }

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
            event.deferReply().setFiles(FileUpload.fromData(connection.getInputStream(), statusCode + ".jpg"))
                .mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("�?� There was an issue getting this HTTP Dog!").mentionRepliedUser(false)
                .queue();
        }
    }
}
