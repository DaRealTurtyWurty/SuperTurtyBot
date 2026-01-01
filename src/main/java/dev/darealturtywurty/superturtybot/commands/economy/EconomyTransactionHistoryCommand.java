package dev.darealturtywurty.superturtybot.commands.economy;

import dev.darealturtywurty.superturtybot.core.command.SubcommandCommand;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.MoneyTransaction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EconomyTransactionHistoryCommand extends SubcommandCommand {
    private static final TextAnchor[] ANCHORS = new TextAnchor[]
            {TextAnchor.TOP_CENTER, TextAnchor.TOP_LEFT, TextAnchor.TOP_RIGHT,
                    TextAnchor.BOTTOM_CENTER, TextAnchor.BOTTOM_LEFT, TextAnchor.BOTTOM_RIGHT,
                    TextAnchor.CENTER, TextAnchor.CENTER_LEFT, TextAnchor.CENTER_RIGHT};

    public EconomyTransactionHistoryCommand() {
        super("economy", "Shows the economy transaction history of a user.");
        addOption(OptionType.USER, "user", "The user to get the transaction history of.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ This command can only be used in servers!", false, true);
            return;
        }

        event.deferReply().queue();

        Optional<Economy> optAccount = EconomyManager.getAccount(guild, user);
        if (optAccount.isEmpty()) {
            event.getHook().sendMessage("❌ That user does not have an account!").mentionRepliedUser(false).queue();
            return;
        }

        Economy account = optAccount.get();
        if(account.isImprisoned()) {
            event.getHook().editOriginalFormat("❌ You are currently imprisoned and cannot view your transaction history! You will be released %s.",
                    TimeFormat.RELATIVE.format(account.getImprisonedUntil())).queue();
            return;
        }

        if (account.getTransactions().size() < 2) {
            event.getHook().sendMessage("❌ That user has no transactions!").mentionRepliedUser(false).queue();
            return;
        }

        long startOfYear = getStartOfCurrentYear();
        long now = System.currentTimeMillis();
        List<MoneyTransaction> transactions = account.getTransactions()
                .stream()
                .filter(transaction -> transaction.timestamp() >= startOfYear && transaction.timestamp() <= now)
                .sorted(Comparator.comparingLong(MoneyTransaction::timestamp))
                .toList();

        ByteArrayOutputStream baos = createScatterPlot(event, account, transactions);
        if(baos == null)
            return;

        GuildData config = GuildData.getOrCreateGuildData(guild);

        StringBuilder transactionHistory = new StringBuilder();
        for (int i = 0; i < transactions.size(); i++) {
            MoneyTransaction transaction = transactions.get(i);
            transactionHistory.append("**Transaction ").append(i + 1).append(":**\n")
                    .append("Amount: ").append(StringUtils.numberFormat(transaction.amount(), config)).append("\n")
                    .append("Type: ").append(transaction.type()).append("\n")
                    .append("Timestamp: ").append(TimeFormat.RELATIVE.format(transaction.timestamp())).append("\n");
        }

        try (FileUpload upload = FileUpload.fromData(baos.toByteArray(), "transaction_history.png")) {
            var embed = new EmbedBuilder()
                    .setTitle("Transaction History for " + user.getEffectiveName())
                    .setDescription(user.getAsMention() + " has performed " + account.getTransactions().size() + " transactions.")
                    .setColor(0x00FF00)
                    .setImage("attachment://transaction_history.png")
                    .setTimestamp(event.getTimeCreated().toInstant())
                    .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                    .build();
            event.getHook().sendMessageEmbeds(embed).addFiles(upload).setContent(transactionHistory.toString()).mentionRepliedUser(false).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("❌ An error occurred while uploading the chart!").mentionRepliedUser(false).queue();
        }
    }

    private static ByteArrayOutputStream createScatterPlot(SlashCommandInteractionEvent event, Economy account, List<MoneyTransaction> transactions) {
        XYSeries series = new XYSeries("Transactions");

        BigInteger balance = EconomyManager.getBalance(account);

        for (MoneyTransaction transaction : transactions) {
            long timestamp = transaction.timestamp();
            BigInteger transactionAmount = transaction.amount();

            series.add(timestamp, balance);
            balance = balance.subtract(transactionAmount);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
                "Transaction History",
                "Time",
                "Balance",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(0, true);
        plot.setRenderer(renderer);

        DateAxis domainAxis = new DateAxis("Time");
        domainAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"));
        plot.setDomainAxis(domainAxis);

        NumberAxis rangeAxis = new NumberAxis("Balance");
        plot.setRangeAxis(rangeAxis);

        BufferedImage image = chart.createBufferedImage(1920, 1080);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            event.getHook().sendMessage("❌ An error occurred while creating the chart!").mentionRepliedUser(false).queue();
            return null;
        }

        return baos;
    }

    private static ByteArrayOutputStream createStepChart(SlashCommandInteractionEvent event, Economy account, List<MoneyTransaction> transactions) {
        TimeSeries series = new TimeSeries("Transactions");

        BigInteger balance = EconomyManager.getBalance(account);

        BigInteger[] balances = new BigInteger[transactions.size()];
        for (int i = transactions.size() - 1; i >= 0; i--) {
            MoneyTransaction transaction = transactions.get(i);
            balances[i] = balance;
            balance = balance.subtract(transaction.amount());
        }

        for (int i = 0; i < transactions.size(); i++) {
            series.addOrUpdate(new FixedMillisecond(transactions.get(i).timestamp()), balances[i]);
        }

        series.addOrUpdate(new FixedMillisecond(System.currentTimeMillis()), EconomyManager.getBalance(account));

        var dataset = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYStepChart(
                "Transaction History",
                "Time",
                "Balance",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();

        GuildData config = GuildData.getOrCreateGuildData(account.getGuild());

        for (int index = 0; index < transactions.size(); index++) {
            MoneyTransaction transaction = transactions.get(index);
            double x = transaction.timestamp();
            double y = balances[index].doubleValue();

            String typeName = MoneyTransaction.getTypeName(transaction.type());
            String label = typeName + "\n" + StringUtils.numberFormat(transaction.amount(), config);

            var annotation = new MultiLineXYTextAnnotation(label, x, y);
            annotation.setFont(new Font("SansSerif", Font.PLAIN, 12));
            annotation.setTextAnchor(ANCHORS[index % ANCHORS.length]);
            plot.addAnnotation(annotation);
        }

        DateAxis axis = new DateAxis("Time");
        axis.setDateFormatOverride(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"));
        plot.setDomainAxis(axis);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        Range range = dataset.getRangeBounds(false);
        double minValue = range.getLowerBound();
        double maxValue = range.getUpperBound();
        double padding = (maxValue - minValue) * 0.1;
        rangeAxis.setRange(minValue - padding, maxValue + padding);

        BufferedImage image = chart.createBufferedImage(1920, 1080);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            event.getHook().sendMessage("❌ An error occurred while creating the chart!").mentionRepliedUser(false).queue();
            return null;
        }

        return baos;
    }

    private static long getStartOfCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static class MultiLineXYTextAnnotation extends XYTextAnnotation {
        private final String[] lines;

        public MultiLineXYTextAnnotation(String text, double x, double y) {
            super(text, x, y);
            this.lines = text.split("\n");
        }

        @Override
        public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea, ValueAxis domainAxis, ValueAxis rangeAxis, int rendererIndex, PlotRenderingInfo info) {
            PlotOrientation orientation = plot.getOrientation();
            RectangleEdge domainEdge = Plot.resolveDomainAxisLocation(
                    plot.getDomainAxisLocation(), orientation);
            RectangleEdge rangeEdge = Plot.resolveRangeAxisLocation(
                    plot.getRangeAxisLocation(), orientation);

            float anchorX = (float) domainAxis.valueToJava2D(getX(), dataArea, domainEdge);
            float anchorY = (float) rangeAxis.valueToJava2D(getY(), dataArea, rangeEdge);

            if (orientation == PlotOrientation.HORIZONTAL) {
                float tempAnchor = anchorX;
                anchorX = anchorY;
                anchorY = tempAnchor;
            }

            g2.setFont(getFont());

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                Shape hotspot = TextUtils.calculateRotatedStringBounds(
                        line, g2, anchorX, anchorY + (lineIndex * g2.getFontMetrics().getHeight()), getTextAnchor(),
                        getRotationAngle(), getRotationAnchor());
                if (getBackgroundPaint() != null) {
                    g2.setPaint(getBackgroundPaint());
                    g2.fill(hotspot);
                }

                g2.setPaint(getPaint());
                TextUtils.drawRotatedString(line, g2, anchorX, anchorY + (lineIndex * g2.getFontMetrics().getHeight()),
                        getTextAnchor(), getRotationAngle(), getRotationAnchor());

                if (isOutlineVisible()) {
                    g2.setStroke(getOutlineStroke());
                    g2.setPaint(getOutlinePaint());
                    g2.draw(hotspot);
                }

                String toolTip = getToolTipText();
                String url = getURL();
                if (toolTip != null || url != null) {
                    addEntity(info, hotspot, rendererIndex, toolTip, url);
                }
            }
        }
    }
}
