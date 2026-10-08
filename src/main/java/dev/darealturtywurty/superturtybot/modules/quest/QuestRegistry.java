package dev.darealturtywurty.superturtybot.modules.quest;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CompletedMatchesConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CompletedMatchesRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CollectablesEarnedConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CollectablesEarnedRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CommandUsageConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CommandUsageRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CommunityActivityConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CommunityActivityRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CommunityExplorerConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CommunityExplorerRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CountingContributorConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.CountingContributorRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.EconomyActionsConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.EconomyActionsRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyAnswersConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyAnswersRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyFirstTryConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyFirstTryRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyGameSizeConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyGameSizeRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyStreakConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.GeographyStreakRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.Game2048Config;
import dev.darealturtywurty.superturtybot.modules.quest.impl.Game2048Rule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.MarketplaceSalesConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.MarketplaceSalesRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.MinigameCompletionsConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.MinigameCompletionsRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.MultiplayerVarietyConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.MultiplayerVarietyRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.PollVotesConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.PollVotesRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.ReminderCompletionConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.ReminderCompletionRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.TriviaStreakConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.TriviaStreakRule;
import dev.darealturtywurty.superturtybot.modules.quest.impl.WordleCompletionStreakConfig;
import dev.darealturtywurty.superturtybot.modules.quest.impl.WordleCompletionStreakRule;

import java.util.Map;
import java.util.Set;

public final class QuestRegistry {
    private static final String[] TIER_DISPLAY_NAMES = {
        "Novice",
        "Enthusiast",
        "Expert",
        "Master",
        "Grandmaster",
        "Legend",
        "Godlike"
    };

    protected static void registerQuests() {
        registerTriviaQuests();
        registerMultiplayerMatchesQuests();
        registerWordleQuests();
        registerCollectableQuests();
        registerMarketplaceQuests();
        registerCommunityQuests();
        registerCountingQuests();
        registerGeographyQuests();
        registerPuzzleQuests();
        registerMultiplayerVarietyQuests();
        register2048Quests();
        registerEconomyQuests();
        registerUtilityExplorationQuests();
    }

    private static void registerUtilityExplorationQuests() {
        registerCommandUsageQuest(
            "knowledge_seeker",
            "Knowledge Seeker",
            "Use Wikipedia, Periodic Table, and Fact during the week.",
            new CommandUsageConfig(Set.of("wikipedia", "periodic-table", "fact"), null, 3, 0),
            150);
        registerCommandUsageQuest(
            "weather_watcher",
            "Weather Watcher",
            "Check the weather on three different days.",
            new CommandUsageConfig(Set.of("weather"), null, 0, 3),
            150);
        registerCommandUsageQuest(
            "developer_toolkit",
            "Developer Toolkit",
            "Use GitHub, CurseForge, and one /latest lookup.",
            new CommandUsageConfig(Set.of("github", "curseforge", "latest"), null, 3, 0),
            200);
        registerCommandUsageQuest(
            "creative_toolkit",
            "Creative Toolkit",
            "Use three different media tools such as LaTeX, PetPet, Deepfry, or Cat Says.",
            new CommandUsageConfig(Set.of("latex", "petpetgif", "deepfry", "catsays"), null, 3, 0),
            200);
        registerCommandUsageQuest(
            "information_explorer",
            "Information Explorer",
            "Use five different utility commands.",
            new CommandUsageConfig(Set.of(), "Utility", 5, 0),
            250);
    }

    private static void registerCommandUsageQuest(
        String id,
        String displayName,
        String description,
        CommandUsageConfig config,
        int xp
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                config,
                new CommandUsageRule(),
                QuestReward.xp(xp),
                (_, _) -> true).addConflictGroups("utility-exploration"));
    }

    private static void registerEconomyQuests() {
        registerEconomyQuest(
            "clocked_in",
            "Clocked In",
            "Complete three job shifts across at least two days.",
            new EconomyActionsConfig(Set.of(QuestManager.ECONOMY_JOB_SHIFT), 3, 2, "shifts"),
            250,
            "job-shifts");
        registerEconomyQuest(
            "career_climber",
            "Career Climber",
            "Earn a job promotion.",
            new EconomyActionsConfig(Set.of(QuestManager.ECONOMY_JOB_PROMOTION), 1, 1),
            400);
        registerEconomyQuest(
            "responsible_borrower",
            "Responsible Borrower",
            "Fully repay a loan.",
            new EconomyActionsConfig(Set.of(QuestManager.ECONOMY_LOAN_REPAID), 1, 1),
            500);
        registerEconomyQuest(
            "property_ladder",
            "Property Ladder",
            "Purchase or upgrade a property.",
            new EconomyActionsConfig(Set.of(
                QuestManager.ECONOMY_PROPERTY_PURCHASED,
                QuestManager.ECONOMY_PROPERTY_UPGRADED), 1, 1),
            400);
        registerEconomyQuest(
            "rental_host",
            "Rental Host",
            "Have another user rent one of your properties.",
            new EconomyActionsConfig(Set.of(QuestManager.ECONOMY_PROPERTY_RENTED_TO_USER), 1, 1),
            500);
        registerEconomyQuest(
            "heist_specialist",
            "Heist Specialist",
            "Successfully complete a heist.",
            new EconomyActionsConfig(Set.of(QuestManager.ECONOMY_HEIST_COMPLETED), 1, 1),
            500);
        registerEconomyQuest(
            "working_week",
            "Working Week",
            "Complete a job shift on five different days.",
            new EconomyActionsConfig(Set.of(QuestManager.ECONOMY_JOB_SHIFT), 5, 5, "shifts"),
            750,
            "job-shifts");
    }

    private static void registerEconomyQuest(
        String id,
        String displayName,
        String description,
        EconomyActionsConfig config,
        int xp,
        String... conflictGroups
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                config,
                new EconomyActionsRule(),
                QuestReward.xp(xp),
                (guild, _) -> GuildData.getOrCreateGuildData(guild).isEconomyEnabled())
                .addConflictGroups(conflictGroups));
    }

    private static void registerPuzzleQuests() {
        registerMinigameQuest(
            "puzzle_sampler",
            "Puzzle Sampler",
            "Complete one Hangman, Word Search, and Crossword game.",
            Map.of("hangman", 1, "word_search", 1, "crossword", 1),
            400,
            "hangman-completions",
            "word-search-completions",
            "crossword-completions");
        registerMinigameQuest(
            "wordsmith",
            "Wordsmith",
            "Successfully complete three Hangman games.",
            Map.of("hangman", 3),
            250,
            "hangman-completions");
        registerMinigameQuest(
            "search_party",
            "Search Party",
            "Complete two Word Search games.",
            Map.of("word_search", 2),
            250,
            "word-search-completions");
        registerMinigameQuest(
            "crossword_conqueror",
            "Crossword Conqueror",
            "Complete a Crossword game.",
            Map.of("crossword", 1),
            300,
            "crossword-completions");
    }

    private static void registerMinigameQuest(
        String id,
        String displayName,
        String description,
        Map<String, Integer> requirements,
        int xp,
        String... conflictGroups
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                new MinigameCompletionsConfig(requirements),
                new MinigameCompletionsRule(),
                QuestReward.xp(xp),
                (_, _) -> true).addConflictGroups(conflictGroups));
    }

    private static void registerMultiplayerVarietyQuests() {
        registerMultiplayerVarietyQuest(
            "board_game_tour",
            "Board Game Tour",
            "Finish matches in three different multiplayer games.",
            new MultiplayerVarietyConfig(3, 0, 0, 0),
            350);
        registerMultiplayerVarietyQuest(
            "friendly_competition",
            "Friendly Competition",
            "Finish multiplayer matches against three different users.",
            new MultiplayerVarietyConfig(0, 3, 0, 0),
            300);
        registerMultiplayerVarietyQuest(
            "rematch",
            "Rematch",
            "Finish two matches against the same opponent.",
            new MultiplayerVarietyConfig(0, 0, 2, 0),
            150);
        registerMultiplayerVarietyQuest(
            "versatile_player",
            "Versatile Player",
            "Win two different multiplayer games.",
            new MultiplayerVarietyConfig(0, 0, 0, 2),
            400);
    }

    private static void registerMultiplayerVarietyQuest(
        String id,
        String displayName,
        String description,
        MultiplayerVarietyConfig config,
        int xp
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                config,
                new MultiplayerVarietyRule(),
                QuestReward.xp(xp),
                (_, _) -> true).addConflictGroups("multiplayer"));
    }

    private static void register2048Quests() {
        register2048Quest(
            "2048_apprentice",
            "2048 Apprentice",
            "Finish a game of 2048 with a 512 tile or better.",
            new Game2048Config(512, true),
            250);
        register2048Quest(
            "2048_expert",
            "2048 Expert",
            "Reach the 2048 tile.",
            new Game2048Config(2048, false),
            750);
    }

    private static void register2048Quest(
        String id,
        String displayName,
        String description,
        Game2048Config config,
        int xp
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                config,
                new Game2048Rule(),
                QuestReward.xp(xp),
                (_, _) -> true).addConflictGroups("2048-tile"));
    }

    private static void registerGeographyQuests() {
        registerGeographyQuest(
            "geography_rookie",
            "Geography Rookie",
            "Answer 3 geography game questions correctly.",
            new GeographyAnswersConfig(3, 0),
            150,
            "geography-correct-count");
        registerGeographyQuest(
            "world_traveller",
            "World Traveller",
            "Answer correctly in 3 different geography game modes.",
            new GeographyAnswersConfig(0, 3),
            300);
        registerGeographyQuest(
            "geography_expert",
            "Geography Expert",
            "Answer 10 geography game questions correctly.",
            new GeographyAnswersConfig(10, 0),
            500,
            "geography-correct-count");

        QuestManager.QUESTS.register(
            "flag_fusion_master",
            new Quest<>(
                "flag_fusion_master",
                "Flag Fusion Master",
                "Correctly identify all flags in a 16-region combined flags game.",
                new GeographyGameSizeConfig("combined_flags", 16),
                new GeographyGameSizeRule(),
                QuestReward.xp(1_000),
                (_, _) -> true));
        QuestManager.QUESTS.register(
            "geoguesser_sharpshooter",
            new Quest<>(
                "geoguesser_sharpshooter",
                "GeoGuesser Sharpshooter",
                "Correctly identify a GeoGuesser location on your first guess.",
                new GeographyFirstTryConfig("geoguesser"),
                new GeographyFirstTryRule(),
                QuestReward.xp(500),
                (_, _) -> true));
        QuestManager.QUESTS.register(
            "border_instinct",
            new Quest<>(
                "border_instinct",
                "Border Instinct",
                "Correctly identify a region border on your first guess.",
                new GeographyFirstTryConfig("region_border"),
                new GeographyFirstTryRule(),
                QuestReward.xp(400),
                (_, _) -> true));
        QuestManager.QUESTS.register(
            "geography_hot_streak",
            new Quest<>(
                "geography_hot_streak",
                "Geography Hot Streak",
                "Answer 10 population or area comparisons correctly in a row.",
                new GeographyStreakConfig(10, Set.of(
                    "higher_lower_population",
                    "higher_lower_area")),
                new GeographyStreakRule(),
                QuestReward.xp(750),
                (_, _) -> true));
    }

    private static void registerGeographyQuest(
        String id,
        String displayName,
        String description,
        GeographyAnswersConfig config,
        int xp,
        String... conflictGroups
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                config,
                new GeographyAnswersRule(),
                QuestReward.xp(xp),
                (_, _) -> true).addConflictGroups(conflictGroups));
    }

    private static void registerCountingQuests() {
        QuestManager.QUESTS.register(
            "counting_contributor",
            new Quest<>(
                "counting_contributor",
                "Counting Contributor",
                "Make 10 valid counts across at least 2 days.",
                new CountingContributorConfig(10, 2),
                new CountingContributorRule(),
                QuestReward.xp(200),
                (guild, _) -> hasCountingChannel(guild.getIdLong())));
    }

    private static void registerCommunityQuests() {
        QuestManager.QUESTS.register(
            "community_explorer",
            new Quest<>(
                "community_explorer",
                "Community Explorer",
                "Complete 1 trivia question, 1 minigame, and 1 valid count.",
                new CommunityExplorerConfig(1, 1, 1),
                new CommunityExplorerRule(),
                QuestReward.xp(300),
                (guild, _) -> hasCountingChannel(guild.getIdLong())));

        QuestManager.QUESTS.register(
            "showcase_star",
            new Quest<>(
                "showcase_star",
                "Showcase Star",
                "Have one of your posts reach the server's starboard threshold.",
                new CommunityActivityConfig("showcase_threshold", 1, 1),
                new CommunityActivityRule(),
                QuestReward.xp(400),
                (guild, _) -> GuildData.getOrCreateGuildData(guild).isStarboardEnabled()));
        QuestManager.QUESTS.register(
            "suggestion_box",
            new Quest<>(
                "suggestion_box",
                "Suggestion Box",
                "Submit a suggestion that is marked considered or approved.",
                new CommunityActivityConfig("suggestion_accepted", 1, 1),
                new CommunityActivityRule(),
                QuestReward.xp(250),
                (guild, _) -> GuildData.getOrCreateGuildData(guild).getSuggestions() != 0L
                    || !guild.getTextChannelsByName("suggestions", false).isEmpty()));
        QuestManager.QUESTS.register(
            "pollster",
            new Quest<>(
                "pollster",
                "Pollster",
                "Create a poll that receives at least five votes from other users.",
                new PollVotesConfig(5),
                new PollVotesRule(),
                QuestReward.xp(200),
                (_, _) -> true));
        QuestManager.QUESTS.register(
            "conversation_starter",
            new Quest<>(
                "conversation_starter",
                "Conversation Starter",
                "Run a would you rather or topic at least 3 times across at least 2 days.",
                new CommunityActivityConfig("conversation_prompt", 3, 2),
                new CommunityActivityRule(),
                QuestReward.xp(150),
                (_, _) -> true));
        QuestManager.QUESTS.register(
            "remember_me",
            new Quest<>(
                "remember_me",
                "Remember Me",
                "Create a reminder and allow it to fire successfully.",
                new ReminderCompletionConfig(1),
                new ReminderCompletionRule(),
                QuestReward.xp(100),
                (_, _) -> true));
    }

    private static boolean hasCountingChannel(long guildId) {
        return Database.getDatabase().counting.find(Filters.eq("guild", guildId)).first() != null;
    }

    private static void registerMarketplaceQuests() {
        QuestManager.QUESTS.register(
            "marketplace_sale",
            new Quest<>(
                "marketplace_sale",
                "Market Trader",
                "List an item on the marketplace and successfully sell it.",
                new MarketplaceSalesConfig(1),
                new MarketplaceSalesRule(),
                QuestReward.xp(500),
                (guild, _) -> GuildData.getOrCreateGuildData(guild).isEconomyEnabled()));
    }

    private static void registerCollectableQuests() {
        registerCollectableQuest(
            "collectors_week",
            "Collector's Week",
            "Earn 2 collectables from the bot's question events.",
            new CollectablesEarnedConfig(2),
            100,
            "collectable-count");
        registerCollectableQuest(
            "collection_variety",
            "Collection Variety",
            "Earn collectables from two different collections.",
            new CollectablesEarnedConfig(0, 2, 0, null, -1, 0),
            250);
        registerCollectableQuest(
            "collectors_hat_trick",
            "Collector's Hat Trick",
            "Earn three collectables from question events.",
            new CollectablesEarnedConfig(3),
            200,
            "collectable-count");
        registerCollectableQuest(
            "quick_draw",
            "Quick Draw",
            "Earn a collectable within one minute of its question appearing.",
            new CollectablesEarnedConfig(1, 0, 0, null, -1, 60_000),
            300);
        registerCollectableQuest(
            "rare_discovery",
            "Rare Discovery",
            "Earn a rare or higher collectable from a question event.",
            new CollectablesEarnedConfig(1, 0, 0, null, CollectableRarity.RARE.ordinal(), 0),
            350);
        registerCollectableQuest(
            "country_collector",
            "Country Collector",
            "Earn two country collectables.",
            new CollectablesEarnedConfig(2, 0, 0, "countries", -1, 0),
            200,
            "collectable-count");
        registerCollectableQuest(
            "dedicated_collector",
            "Dedicated Collector",
            "Earn collectables on three different days.",
            new CollectablesEarnedConfig(0, 0, 3, null, -1, 0),
            400);
    }

    private static void registerCollectableQuest(
        String id,
        String displayName,
        String description,
        CollectablesEarnedConfig config,
        int xp,
        String... conflictGroups
    ) {
        QuestManager.QUESTS.register(
            id,
            new Quest<>(
                id,
                displayName,
                description,
                config,
                new CollectablesEarnedRule(),
                QuestReward.xp(xp),
                (_, _) -> true).addConflictGroups(conflictGroups));
    }

    private static void registerWordleQuests() {
        for (int days = 1; days <= 7; days++) {
            int requiredDays = days;
            String dayLabel = requiredDays == 1 ? "day" : "days";
            QuestManager.QUESTS.register(
                "wordle_streak_" + requiredDays,
                new Quest<>(
                    "wordle_streak_" + requiredDays,
                    "Wordle " + TIER_DISPLAY_NAMES[requiredDays - 1],
                    "Complete Wordle on " + requiredDays + " consecutive " + dayLabel + ".",
                    new WordleCompletionStreakConfig(requiredDays),
                    new WordleCompletionStreakRule(),
                    QuestReward.xp(100 * requiredDays),
                    (_, _) -> true).addConflictGroups("wordle-streak"));
        }
    }

    private static void registerMultiplayerMatchesQuests() {
        String[] types = {"connect4", "checkers", "chess", "battleships"};
        for (var ref = new Object() {
            private int i = 3;
        };ref.i < 10; ref.i++) {
            QuestManager.QUESTS.register(
                "multiplayer_matches_" + ref.i,
                new Quest<>(
                    "multiplayer_matches_" + ref.i,
                    "Multiplayer " + TIER_DISPLAY_NAMES[Math.min(ref.i - 3, TIER_DISPLAY_NAMES.length - 1)],
                    "Play " + ref.i + " multiplayer matches.",
                    new CompletedMatchesConfig(ref.i, null),
                    new CompletedMatchesRule(),
                    QuestReward.xp(75 * (ref.i - 2)),
                    (_, _) -> true).addConflictGroups("multiplayer"));

            for (String type : types) {
                QuestManager.QUESTS.register(
                    "multiplayer_matches_" + type + "_" + ref.i,
                    new Quest<>(
                        "multiplayer_matches_" + type + "_" + ref.i,
                        "Multiplayer " + TIER_DISPLAY_NAMES[Math.min(ref.i - 3, TIER_DISPLAY_NAMES.length - 1)] + " ("
                            + type + ")",
                        "Play " + ref.i + " multiplayer matches of type " + type + ".",
                        new CompletedMatchesConfig(ref.i, type),
                        new CompletedMatchesRule(),
                        QuestReward.xp(100 * (ref.i - 2)),
                        (_, _) -> true).addConflictGroups("multiplayer"));
            }
        }
    }

    private static void registerTriviaQuests() {
        for (var ref = new Object() {
            private int i = 3;
        };ref.i < 10; ref.i++) {
            QuestManager.QUESTS.register(
                "trivia_correct_" + ref.i,
                new Quest<>(
                    "trivia_correct_" + ref.i,
                    "Trivia " + TIER_DISPLAY_NAMES[Math.min(ref.i - 3, TIER_DISPLAY_NAMES.length - 1)],
                    "Answer " + ref.i + " trivia questions correctly.",
                    new TriviaStreakConfig(ref.i, false),
                    new TriviaStreakRule(),
                    QuestReward.xp(75 * (ref.i - 2)),
                    (_, _) -> true).addConflictGroups("trivia-answers"));

            QuestManager.QUESTS.register(
                "trivia_streak_" + ref.i,
                new Quest<>(
                    "trivia_streak_" + ref.i,
                    ref.i + " Time Trivia Streaker",
                    "Answer " + ref.i + " trivia questions correctly in a row.",
                    new TriviaStreakConfig(ref.i, true),
                    new TriviaStreakRule(),
                    QuestReward.xp(150 * (ref.i - 2)),
                    (_, _) -> true).addConflictGroups("trivia-answers"));
        }
    }

    private QuestRegistry() {
    }
}
