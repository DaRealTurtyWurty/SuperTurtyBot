package io.github.darealturtywurty.superturtybot.commands.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RainbowSixStatsCommand extends CoreCommand {
    public RainbowSixStatsCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "user", "The name of the user to get statistics for", true),
            new OptionData(OptionType.STRING, "platform", "The platform in which this account can be found", false)
                .addChoice("xbox", "xbox").addChoice("pc", "pc").addChoice("playstation", "playstation"));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Gathers statistics about a rainbow six siege account";
    }

    @Override
    public String getHowToUse() {
        return "/r6stats [username]\n/r6stats [username] [xbox|pc|playstation]";
    }

    @Override
    public String getName() {
        return "r6stats";
    }

    @Override
    public String getRichName() {
        return "Rainbow Six Siege Stats";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        final String user = event.getOption("user").getAsString();
        String platform = event.getOption("platform", "pc", OptionMapping::getAsString).toLowerCase().trim();
        if (!"pc".equals(platform) && !"xbox".equals(platform) && !"playstation".equals(platform)) {
            platform = "pc";
        }

        try {
            final URLConnection connection = new URL("https://api2.r6stats.com/public-api/stats/"
                + URLEncoder.encode(user, StandardCharsets.UTF_8) + "/" + platform + "/generic").openConnection();
            connection.addRequestProperty("Authorization", "Bearer " + Environment.INSTANCE.r6StatsKey());
            connection.connect();

            final JsonObject result = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            event.deferReply().addEmbeds(createEmbed(result).build()).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
        }
    }

    public static EmbedBuilder createEmbed(final JsonObject result) {
        final var embed = new EmbedBuilder();
        embed.setTitle(
            "Stats for: " + result.get("username").getAsString() + " (" + result.get("platform").getAsString() + ")");

        embed.setThumbnail(result.get("avatar_url_256").getAsString());

        final JsonObject alias = result.get("aliases").getAsJsonArray().get(0).getAsJsonObject();
        try {
            embed.setAuthor(alias.get("username").getAsString() + " | Last Seen At: "
                + DateFormat.getDateInstance().parse(alias.get("last_seen_at").getAsString()));
        } catch (final ParseException exception) {
            embed.setAuthor(alias.get("username").getAsString());
        }

        final JsonObject stats = result.getAsJsonObject("stats");
        final JsonObject overallStats = stats.getAsJsonObject("general");
        // @formatter:off
        embed.setDescription("__**Overall Stats:**__"
                + "\nKills: " + overallStats.get("kills").getAsInt()
                + "\nDeaths: " + overallStats.get("deaths").getAsInt()
                + "\nGames Played: " + overallStats.get("games_played").getAsInt()
                + "\nWins: " + overallStats.get("wins").getAsInt()
                + "\nLosses: " + overallStats.get("losses").getAsInt()
                + "\nDraws: " + overallStats.get("draws").getAsInt()
                + "\nAssists: " + overallStats.get("assists").getAsInt()
                + "\nBullets Hit: " + overallStats.get("bullets_hit").getAsInt()
                + "\nHeadshots: " + overallStats.get("headshots").getAsInt()
                + "\nMelee Kills: " + overallStats.get("melee_kills").getAsInt()
                + "\nPenetration Kills: " + overallStats.get("penetration_kills").getAsInt()
                + "\nBlind Kills: " + overallStats.get("blind_kills").getAsInt()
                + "\nDBNOs(Down But Not Out): " + overallStats.get("dbnos").getAsInt()
                + "\nKD: " + overallStats.get("kd").getAsDouble()
                + "\nWL: " + overallStats.get("wl").getAsDouble()
                + "\nBarricades Deployed: " + overallStats.get("barricades_deployed").getAsInt()
                + "\nReinforcements Deployed: " + overallStats.get("reinforcements_deployed").getAsInt()
                + "\nGadgets Destroyed: " + overallStats.get("gadgets_destroyed").getAsInt()
                + "\nRappel Breaches: " + overallStats.get("rappel_breaches").getAsInt()
                + "\nRevives: " + overallStats.get("revives").getAsInt()
                + "\nSuicides: " + overallStats.get("suicides").getAsInt()
                + "\nTime Played: " + millisecondsFormatted(overallStats.get("playtime").getAsLong() * 1000L)
                + "\nDistance Travelled: " + overallStats.get("distance_travelled").getAsLong() + "m");

        // @formatter:on
        final JsonObject progression = result.getAsJsonObject("progression");
        embed.addField("Progression Information:",
            "Level: " + progression.get("level").getAsInt() + "\nTotal XP: " + progression.get("total_xp").getAsInt()
                + "\nAlpha Pack Chance: " + progression.get("lootbox_probability").getAsInt(),
            false);

        final JsonObject queueStats = stats.getAsJsonObject("queue");
        final JsonObject casualStats = queueStats.getAsJsonObject("casual");
        final JsonObject rankedStats = queueStats.getAsJsonObject("ranked");
        final JsonObject otherStats = queueStats.getAsJsonObject("other");
        embed.addField("Casual Stats:", getQueueStats(casualStats), false);
        embed.addField("Ranked Stats:", getQueueStats(rankedStats), false);
        embed.addField("Unranked and Event Stats:", getQueueStats(otherStats), false);

        embed.setFooter("Ubisoft ID: " + result.get("ubisoft_id").getAsString() + " | Uplay ID: "
            + result.get("uplay_id").getAsString());
        return embed;
    }

    private static String getQueueStats(final JsonObject queue) {
        // @formatter:off
        return "Kills: " + queue.get("kills").getAsInt()
                + "\nDeaths: " + queue.get("deaths").getAsInt()
                + "\nGames Played: " + queue.get("games_played").getAsInt()
                + "\nWins: " + queue.get("wins").getAsInt()
                + "\nLosses: " + queue.get("losses").getAsInt()
                + "\nDraws: " + queue.get("draws").getAsInt()
                + "\nKD: " + queue.get("kd").getAsDouble()
                + "\nWL: " + queue.get("wl").getAsDouble()
                + "\nTime Played: " + millisecondsFormatted(queue.get("playtime").getAsLong() * 1000L);
        // @formatter:on
    }

    // TODO: Utility class
    private static String millisecondsFormatted(final long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        final long years = days / 365;
        days %= 365;
        final long months = days / 30;
        days %= 30;
        final long weeks = days / 7;
        days %= 7;
        final long hours = TimeUnit.MILLISECONDS.toHours(millis)
            - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
            - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
            - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        final long milliseconds = TimeUnit.MILLISECONDS.toMillis(millis)
            - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(millis));
        return String.format("%dyrs %dm %dw %ddays %dhrs %dmins %dsecs %dms", years, months, weeks, days, hours,
            minutes, seconds, milliseconds);
    }
}
