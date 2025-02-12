package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.discord.BotUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StrawpollResultsCommand extends CoreCommand {
    private static final int WIDTH = 1080, HEIGHT = 720;

    public StrawpollResultsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "id", "The ID of the strawpoll (found at https://strawpoll.com/________)",
                true),
            new OptionData(OptionType.BOOLEAN, "is3d", "Whether or not this should provide a 3D chart instead of 2D",
                false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Retrieves the results of a strawpoll.";
    }

    @Override
    public String getHowToUse() {
        return "/strawpollresults [id]\n/strawpollresults [id] [is3D]";
    }

    @Override
    public String getName() {
        return "strawpollresults";
    }

    @Override
    public String getRichName() {
        return "Strawpoll Results";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.MINUTES, 1L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String id = event.getOption("id", OptionMapping::getAsString);
        if (id == null) {
            event.deferReply(true)
                    .setContent("❌ You must provide a strawpoll ID!")
                    .mentionRepliedUser(false)
                    .queue();
            return;
        }
        final boolean is3D = event.getOption("is3d", false, OptionMapping::getAsBoolean);

        try {
            final InputStream stream = handle(id, is3D);
            event.deferReply().setFiles(FileUpload.fromData(stream, "chart.png")).mentionRepliedUser(false).queue();
        } catch (final IOException | URISyntaxException | IllegalStateException exception) {
            event.deferReply(true)
                .setContent("There has been an error with this command. Please report the following to the bot owner:\n"
                    + exception.getMessage() + "\n" + ExceptionUtils.getMessage(exception))
                .mentionRepliedUser(true).queue();
            Constants.LOGGER.error("Error getting strawpoll results!", exception);
        }
    }

    private InputStream handle(String id, boolean is3D) throws IOException, URISyntaxException, IllegalStateException {
        final URLConnection connection = new URI(StrawpollCommand.STRAWPOLL_URL + "/" + id).toURL().openConnection();
        connection.addRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        final InputStream input = connection.getInputStream();
        final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(input)));
        input.close();

        JsonObject jsonObject = Constants.GSON.fromJson(result, JsonObject.class);
        if(!jsonObject.has("content")) {
            throw new IllegalStateException("Invalid strawpoll ID!");
        }

        final var response = jsonObject.get("content").getAsJsonObject()
            .get("poll").getAsJsonObject();
        final var answers = response.getAsJsonArray("poll_answers");
        final Map<String, Double> resultMap = new HashMap<>();
        for (final JsonElement element : answers) {
            final JsonObject answer = element.getAsJsonObject();
            resultMap.put(answer.get("answer").getAsString(), answer.get("votes").getAsDouble());
        }

        final PieDataset<String> dataset = createDataset(resultMap);
        final JFreeChart chart = createChart(dataset, response.get("title").getAsString(), is3D);
        final BufferedImage image = drawChart(chart);
        return BotUtils.toInputStream(image);
    }

    @SuppressWarnings("deprecation")
    public static JFreeChart createChart(final PieDataset<String> dataset, final String title, final boolean is3D) {
        JFreeChart chart;
        if (is3D) {
            chart = ChartFactory.createPieChart3D(title, dataset, false, true, Locale.getDefault());
        } else {
            chart = ChartFactory.createPieChart(title, dataset, false, true, Locale.getDefault());
        }
        return chart;
    }

    public static PieDataset<String> createDataset(final Map<String, Double> data) {
        final var dataset = new DefaultPieDataset<String>();
        data.forEach(dataset::setValue);
        return dataset;
    }

    public static BufferedImage drawChart(final JFreeChart chart) {
        final var bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = bufferedImage.createGraphics();
        chart.draw(graphics, new Rectangle2D.Double(0, 0, WIDTH, HEIGHT));
        graphics.dispose();
        return bufferedImage;
    }
}
