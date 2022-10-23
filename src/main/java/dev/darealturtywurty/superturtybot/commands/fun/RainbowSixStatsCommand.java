package dev.darealturtywurty.superturtybot.commands.fun;

import java.text.DateFormat;
import java.util.List;

import de.jan.r6statsjava.R6Player;
import de.jan.r6statsjava.R6PlayerStats;
import de.jan.r6statsjava.R6QueueStats;
import de.jan.r6statsjava.R6Stats;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RainbowSixStatsCommand extends CoreCommand {
    public static final R6Stats R6STATS = new R6Stats(Environment.INSTANCE.r6StatsKey());

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
        return CommandCategory.FUN;
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
            final R6Player player = R6STATS.getR6PlayerStats(user, R6Stats.Platform.valueOf(platform.toUpperCase()));
            final R6PlayerStats stats = player.getGeneralStats();

            reply(event, createEmbed(player, stats));
        } catch (final Exception exception) {
            reply(event, "❌ Unable to find this player!", false, true);
        }
    }
    
    public static EmbedBuilder createEmbed(R6Player player, R6PlayerStats stats) {
        final var embed = new EmbedBuilder();
        embed.setTitle("Stats for: " + player.getUsername() + " (" + player.getPlatform() + ")");
        
        embed.setThumbnail(player.getAvatarURL256());

        embed.setAuthor(
            player.getUsername() + " | Last Updated: " + DateFormat.getDateInstance().format(player.getLastUpdated()),
            player.getAvatarURL146());
        
        // @formatter:off
        embed.setDescription("__**Overall Stats:**__"
                + "\nKills: " + stats.getKills()
                + "\nDeaths: " + stats.getDeaths()
                + "\nGames Played: " + stats.getGamesPlayed()
                + "\nWins: " + stats.getWins()
                + "\nLosses: " + stats.getLosses()
                + "\nDraws: " + stats.getDraws()
                + "\nAssists: " + stats.getAssists()
                + "\nBullets Hit: " + stats.getBulletsHit()
                + "\nHeadshots: " + stats.getHeadshots()
                + "\nMelee Kills: " + stats.getMeleeKills()
                + "\nPenetration Kills: " + stats.getPenetrationKills()
                + "\nBlind Kills: " + stats.getBlindKills()
                + "\nDBNOs(Down But Not Out): " + stats.getDbnos()
                + "\nKD: " + stats.getKd()
                + "\nWL: " + stats.getWl()
                + "\nBarricades Deployed: " + stats.getBarricadesDeployed()
                + "\nReinforcements Deployed: " + stats.getReinforcementsDeployed()
                + "\nGadgets Destroyed: " + stats.getGadgetsDestroyed()
                + "\nRappel Breaches: " + stats.getRappelBreaches()
                + "\nRevives: " + stats.getRevives()
                + "\nSuicides: " + stats.getSuicides()
                + "\nTime Played: " + stats.getPlayTime().toDuration()
                + "\nDistance Travelled: " + stats.getDistanceTravelled().toKilometers() + "km");
        // @formatter:on
        
        final R6QueueStats casualStats = player.getCasualStats();
        final R6QueueStats rankedStats = player.getRankedStats();
        final R6QueueStats otherStats = player.getOtherQueueStats();
        
        embed.addField("Progression Information:", "Level: " + player.getLevel() + "\nTotal XP: " + player.getTotalXP()
            + "\nAlpha Pack Chance: " + player.getLootboxProbability(), false);
        embed.addField("Casual Stats:", getQueueStats(casualStats), false);
        embed.addField("Ranked Stats:", getQueueStats(rankedStats), false);
        embed.addField("Unranked and Event Stats:", getQueueStats(otherStats), false);
        
        embed.setFooter("Ubisoft ID: " + player.getUbisoftID() + " | Uplay ID: " + player.getUplayID());
        return embed;
    }
    
    private static String getQueueStats(final R6QueueStats stats) {
        // @formatter:off
        return "Kills: " + stats.getKills()
                + "\nDeaths: " + stats.getDeaths()
                + "\nGames Played: " + stats.getGamesPlayed()
                + "\nWins: " + stats.getWins()
                + "\nLosses: " + stats.getLosses()
                + "\nDraws: " + stats.getDraws()
                + "\nKD: " + stats.getKd()
                + "\nWL: " + stats.getWl()
                + "\nTime Played: " + stats.getPlayTime().toDuration();
        // @formatter:on
    }
}
