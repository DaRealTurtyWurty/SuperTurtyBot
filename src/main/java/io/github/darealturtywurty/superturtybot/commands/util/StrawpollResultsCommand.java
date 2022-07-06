package io.github.darealturtywurty.superturtybot.commands.util;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

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
    public String getName() {
        return "strawpollresults";
    }
    
    @Override
    public String getRichName() {
        return "Strawpoll Results";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String id = event.getOption("id").getAsString();
        final boolean is3D = event.getOption("is3d", false, OptionMapping::getAsBoolean);
        
        try {
            final InputStream stream = handle(id, is3D);
            event.deferReply().addFile(stream, "chart.png").mentionRepliedUser(false).queue();
        } catch (final IOException | IllegalStateException exception) {
            event.deferReply(true)
                .setContent("There has been an error with this command. Please report the following to the bot owner:\n"
                    + exception.getMessage() + "\n" + ExceptionUtils.getMessage(exception))
                .mentionRepliedUser(true).queue();
        }
    }
    
    private InputStream handle(String id, boolean is3D) throws IOException, IllegalStateException {
        final URLConnection connection = new URL(StrawpollCommand.STRAWPOLL_URL + "/" + id).openConnection();
        connection.addRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        final InputStream input = connection.getInputStream();
        final String result = IOUtils.toString(new BufferedReader(new InputStreamReader(input)));
        input.close();
        
        final var response = Constants.GSON.fromJson(result, JsonObject.class).get("content").getAsJsonObject()
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
        return toInputStream(image);
    }
    
    @SuppressWarnings("deprecation")
    public static JFreeChart createChart(final PieDataset<String> dataset, final String title, final boolean is3D) {
        JFreeChart chart;
        if (is3D) {
            // TODO: Rewrite this to use the new library
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
    
    // TODO: Utility class
    @Nullable
    public static InputStream toInputStream(@NotNull BufferedImage image) {
        try {
            final var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return new ByteArrayInputStream(output.toByteArray());
        } catch (final IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
    
    // TODO: Utility class
    private static boolean readBoolean(String str) {
        final String mod = str.trim().toLowerCase();
        return switch (mod) {
            case "y", "ye", "yes", "yea", "yeah", "true", "1" -> true;
            case "n", "nope", "no", "nah", "ne", "false", "0" -> false;
            default -> false;
        };
    }
}
