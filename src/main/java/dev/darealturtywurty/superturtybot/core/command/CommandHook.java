package dev.darealturtywurty.superturtybot.core.command;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.core.*;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigCommand;
import dev.darealturtywurty.superturtybot.commands.core.config.UserConfigCommand;
import dev.darealturtywurty.superturtybot.commands.core.suggestion.SuggestCommand;
import dev.darealturtywurty.superturtybot.commands.economy.*;
import dev.darealturtywurty.superturtybot.commands.fun.*;
import dev.darealturtywurty.superturtybot.commands.fun.relationship.RelationshipCommand;
import dev.darealturtywurty.superturtybot.commands.image.*;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCommand;
import dev.darealturtywurty.superturtybot.commands.levelling.SetXPCommand;
import dev.darealturtywurty.superturtybot.commands.minigames.*;
import dev.darealturtywurty.superturtybot.commands.moderation.*;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.ClearWarningsCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.RemoveWarnCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarnCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarningsCommand;
import dev.darealturtywurty.superturtybot.commands.music.*;
import dev.darealturtywurty.superturtybot.commands.nsfw.GuessSexPositionCommand;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWSmashOrPassCommand;
import dev.darealturtywurty.superturtybot.commands.test.TestCommand;
import dev.darealturtywurty.superturtybot.commands.util.*;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.MinecraftCommand;
import dev.darealturtywurty.superturtybot.commands.util.roblox.RobloxCommand;
import dev.darealturtywurty.superturtybot.commands.util.steam.SteamCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.BirthdayManager;
import dev.darealturtywurty.superturtybot.modules.ChangelogFetcher;
import dev.darealturtywurty.superturtybot.modules.counting.RegisterCountingCommand;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.weblisteners.social.SteamListener;
import dev.darealturtywurty.superturtybot.weblisteners.social.TwitchListener;
import dev.darealturtywurty.superturtybot.weblisteners.social.YouTubeListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CommandHook extends ListenerAdapter {
    public static final CommandHook INSTANCE = new CommandHook();

    private static final String STARTUP_MESSAGE = "Initiating... Startup... Sequence.. Hello! I'm TurtyBot. I have a bunch of commands you can use, and I'm always adding more! You can see all of my commands by typing `/commands` in any channel that you and I can access.";
    private static final Set<CommandCategory> CATEGORIES = new HashSet<>();
    private static boolean IS_DEV_MODE = false;

    private final Set<CoreCommand> commands = new HashSet<>();

    private CommandHook() {
    }

    public static Set<CommandCategory> getCategories() {
        return CATEGORIES;
    }

    public static boolean isDevMode() {
        return IS_DEV_MODE;
    }

    private static void init(JDA jda) {
        if (!YouTubeListener.isRunning()) {
            YouTubeListener.runExecutor(jda);
        }

        if (!TwitchListener.isInitialized()) {
            TwitchListener.initialize(jda);
        }

        if (!SteamListener.isRunning()) {
            SteamListener.runExecutor(jda);
        }

        // TODO: When this is fully implemented, uncomment this
//        if (!RedditListener.isInitialized()) {
//            RedditListener.initialize(jda);
//        }

        if (!EconomyManager.isRunning()) {
            EconomyManager.start(jda);
        }

        // TODO: Fix this mf
//        if (!TwitterListener.isInitialized()) {
//            TwitterListener.initialize(jda);
//        }

        if (!BirthdayManager.isRunning()) {
            BirthdayManager.start(jda);
        }

        Guild devGuild = jda.getGuildById(1096109606452867243L);
        if (devGuild != null) {
            IS_DEV_MODE = true;
        }

//        if (!isDevMode()) {
//            AutoModerator.INSTANCE.initialize();
//        }
    }

    private static void sendStartupMessage(@Nullable TextChannel channel) {
        if (channel == null) return;

        String changelog = ChangelogFetcher.INSTANCE.appendChangelog(STARTUP_MESSAGE);
        channel.sendMessage(changelog).queue();
    }

    private static void printCommandList(JDA jda, Set<CoreCommand> cmds) {
        final List<TextChannel> channels = jda.getTextChannelsByName("command-list", true);
        if (channels.isEmpty()) return;

        final TextChannel cmdList = channels.getFirst();
        if (cmdList == null) return;

        final var builder = new StringBuilder();
        final var previous = new AtomicReference<CoreCommand>();

        final var slashes = new AtomicInteger();
        final var prefixes = new AtomicInteger();
        final var messageCtx = new AtomicInteger();
        final var userCtx = new AtomicInteger();

        final var guild = new AtomicInteger();
        final var global = new AtomicInteger();
        cmds.stream().sorted((cmd0, cmd1) -> cmd0.getCategory().getName().compareToIgnoreCase(cmd1.getCategory().getName())).forEach(cmd -> {
            if (previous.get() != null && !previous.get().getCategory().equals(cmd.getCategory())) {
                builder.append("\n**").append(cmd.getCategory().getName()).append("**\n");
            } else if (previous.get() == null) {
                builder.append("**").append(cmd.getCategory().getName()).append("**\n");
            }

            builder.append("`").append(cmd.types.slash() ? "/" : ".").append(cmd.getName()).append("`\n");
            previous.set(cmd);

            if (cmd.types.slash()) {
                slashes.incrementAndGet();
            }

            if (cmd.types.normal()) {
                prefixes.incrementAndGet();
            }

            if (cmd.types.messageCtx()) {
                messageCtx.incrementAndGet();
            }

            if (cmd.types.userCtx()) {
                userCtx.incrementAndGet();
            }

            if (cmd.isServerOnly()) {
                guild.incrementAndGet();
            } else {
                global.incrementAndGet();
            }
        });

        cmdList.createCopy().setPosition(cmdList.getPosition()).queue(success -> {
            success.sendMessage(builder.toString()).queue();
            success.sendMessage("\n\nThere are **%s** slash commands.\nThere are **%d** prefix commands.\nThere are **%d** message context commands.\nThere are **%d** user context commands.\nThere are **%d** guild commands.\nThere are **%d** global commands.".formatted(
                            slashes.get(), prefixes.get(), messageCtx.get(), userCtx.get(), guild.get(), global.get()))
                    .queue();
            cmdList.delete().queue();
        });
    }

    private static void registerCommands(JDA jda, Collection<CoreCommand> commands) {
        CommandListUpdateAction updates = jda.updateCommands();
        commands.forEach(cmd -> registerCommand(cmd, updates));
        updates.queue(registered ->
                registered.forEach(command ->
                        commands.stream()
                                .filter(registeredCommand -> registeredCommand.getName().equals(command.getName()))
                                .findFirst()
                                .ifPresent(registeredCommand -> registeredCommand.setCommandId(command.getId()))));
    }

    private static void registerCommands(Guild guild, Collection<CoreCommand> commands) {
        CommandListUpdateAction updates = guild.updateCommands();
        commands.forEach(cmd -> registerCommand(cmd, updates));
        updates.queue(registered -> {
            for (Command command : registered) {
                commands.stream()
                        .filter(registeredCommand -> registeredCommand.getName().equals(command.getName()))
                        .findFirst()
                        .ifPresent(registeredCommand -> registeredCommand.setCommandId(guild.getIdLong(), command.getId()));
            }
        });
    }

    protected static void registerCommand(CoreCommand cmd, CommandListUpdateAction updates) {
        if (cmd.types.slash()) {
            final SlashCommandData data = Commands.slash(cmd.getName(), cmd.getDescription().substring(0, Math.min(cmd.getDescription().length(), 100)));
            final List<OptionData> options = cmd.createOptions();
            if (!options.isEmpty()) {
                data.addOptions(options);
            }

            List<SubcommandGroupData> subcommandGroupData = cmd.createSubcommandGroupData();
            if (!subcommandGroupData.isEmpty()) {
                data.addSubcommandGroups(subcommandGroupData);
            } else {
                final List<SubcommandData> subcommandData = cmd.createSubcommandData();
                if (!subcommandData.isEmpty()) {
                    data.addSubcommands(subcommandData);
                }

                final List<SubcommandCommand> subcommands = cmd.getSubcommands();
                if (!subcommands.isEmpty()) {
                    subcommands.forEach(subcommand -> {
                        var subcommandDatum = new SubcommandData(subcommand.getName(), subcommand.getDescription());
                        subcommandDatum.addOptions(subcommand.getOptions());
                        data.addSubcommands(subcommandDatum);
                    });
                }
            }

            updates.addCommands(data);
        }

        if (cmd.types.messageCtx()) {
            updates.addCommands(Commands.message(cmd.getRichName()));
        }

        if (cmd.types.userCtx()) {
            updates.addCommands(Commands.user(cmd.getRichName()));
        }
    }

    private static Set<CoreCommand> createCommands() {
        final Set<CoreCommand> commands = new HashSet<>();
        // Core
        commands.add(new PingCommand());
        commands.add(new HelpCommand());
        commands.add(new CommandListCommand());
        commands.add(new TagCommand());
        commands.add(new ShutdownCommand());
        commands.add(new RestartCommand());
        commands.add(new GuildConfigCommand());
        commands.add(new UserConfigCommand());
        commands.add(new OptCommand());
        commands.add(new UptimeCommand());
        //commands.add(new SystemStatsCommand());
        //commands.add(new SubmitCommand());
        //commands.add(new SubmissionCommand());
        commands.add(new EvalCommand());
        commands.add(new SpeakCommand());
        commands.add(new SpeakVoiceCommand());
        commands.add(new AnnounceCommand());
        commands.add(new TestCommand());

        // Utility
        commands.add(new BotInfoCommand());
        commands.add(new UserInfoCommand());
        commands.add(new ServerInfoCommand());
        commands.add(new PollCommand());
        commands.add(new StrawpollCommand());
        commands.add(new StrawpollResultsCommand());
        commands.add(new RolesCommand());
        commands.add(new GithubRepositoryCommand());
        commands.add(new CurseforgeCommand());
        commands.add(new SuggestCommand());
        commands.add(new HighlightCommand());
        commands.add(new RoleSelectionCommand());
        commands.add(new TopicCommand());
        commands.add(new WouldYouRatherCommand());
        commands.add(new NotifierCommand());
        commands.add(new PeriodicTableCommand());
        commands.add(new FactCommand());
        commands.add(new QuoteCommand());
        commands.add(new LatestCommand());
        commands.add(new AnalyzeLogCommand());
        commands.add(new EmbedCommand());
        commands.add(new ReminderCommand());
        commands.add(new RainbowSixStatusCommand());
        commands.add(new WeatherCommand());
        commands.add(new SteamCommand());
        commands.add(new RobloxCommand());
        commands.add(new MinecraftCommand());
        commands.add(new WikipediaCommand());
        //commands.add(new ConvertCommand());
        commands.add(new BirthdayCommand());
        commands.add(new LatexCommand());
        commands.add(new AddRoleToThreadCommand());

        // Moderation
        commands.add(new BanCommand());
        commands.add(new UnbanCommand());
        commands.add(new TimeoutCommand());
        commands.add(new RemoveTimeoutCommand());
        commands.add(new KickCommand());
        commands.add(new PurgeCommand());
        commands.add(new WarnCommand());
        commands.add(new RemoveWarnCommand());
        commands.add(new ClearWarningsCommand());
        commands.add(new WarningsCommand());
        commands.add(new SlowmodeCommand());
        commands.add(new RegisterCountingCommand());
        commands.add(new ReportCommand());
        commands.add(new ReportsCommand());

        // NSFW
        commands.add(new NSFWCommand());
        commands.add(new GuessSexPositionCommand());
        commands.add(new NSFWSmashOrPassCommand());

        // Music
        commands.add(new JoinCommand());
        commands.add(new LeaveCommand());
        commands.add(new PlayCommand());
        commands.add(new NowPlayingCommand());
        commands.add(new QueueCommand());
        commands.add(new RemoveCommand());
        commands.add(new SkipCommand());
        commands.add(new PauseCommand());
        commands.add(new ResumeCommand());
        commands.add(new VolumeCommand());
        commands.add(new ShuffleCommand());
        commands.add(new ClearCommand());
        commands.add(new SearchCommand());
        commands.add(new LyricsCommand());
        commands.add(new RemoveDuplicatesCommand());
        commands.add(new LeaveCleanupCommand());
        commands.add(new LoopCommand());
        commands.add(new MusicRestartCommand());
        commands.add(new SeekCommand());
        commands.add(new MoveCommand());
        commands.add(new VoteSkipCommand());
        commands.add(new SaveSongCommand());

        // Image
        commands.add(new HttpCatCommand());
        commands.add(new MemeCommand());
        commands.add(new ProgrammingMemeCommand());
        commands.add(new InspiroBotCommand());
        commands.add(new HttpDogCommand());
        commands.add(new ImageCommand());
        commands.add(new DeepfryCommand());
        commands.add(new CatSaysCommand());
        commands.add(new FlagifyCommand());
        commands.add(new LGBTifyCommand());

        // Fun
        commands.add(new AdviceCommand());
        commands.add(new CoinFlipCommand());
        commands.add(new EightBallCommand());
        commands.add(new InternetRulesCommand());
        commands.add(new ReverseTextCommand());
        commands.add(new UpsideDownTextCommand());
        commands.add(new UrbanDictionaryCommand());
        commands.add(new PetPetGifCommand());
        commands.add(new LoveCommand());
        commands.add(new SmashOrPassCommand());
        commands.add(new RelationshipCommand());

        // Levelling
        commands.add(new RankCommand());
        commands.add(new LeaderboardCommand());
        //commands.add(new XPInventoryCommand());
        commands.add(new SetXPCommand());

        // Minigames
        commands.add(new TriviaCommand());
        commands.add(new GuessCommand());
        commands.add(new HigherLowerCommand());
        commands.add(new WordleCommand());
        commands.add(new HangmanCommand());
        commands.add(new TicTacToeCommand());
        commands.add(new WordSearchCommand());
        commands.add(new Connect4Command());
        commands.add(new CheckersCommand());
//        commands.add(new ChessCommand());
//        commands.add(new MinesweeperCommand());
//        commands.add(new CrosswordCommand());
//        commands.add(new SudokuCommand());
//        commands.add(new RussianRouletteCommand());
//        commands.add(new WordScrambleCommand());
//        commands.add(new BattleshipCommand());
//        commands.add(new BlackjackCommand());
//        commands.add(new PokerCommand());
//        commands.add(new UnoCommand());
//        commands.add(new LudoCommand());
//        commands.add(new MonopolyCommand());
//        commands.add(new 2048Command());

        // Economy
        commands.add(new BalanceCommand());
        commands.add(new CrimeCommand());
        commands.add(new DepositCommand());
        commands.add(new JobCommand());
        commands.add(new RewardCommand());
        commands.add(new RobCommand());
        commands.add(new SexWorkCommand());
        commands.add(new ShopCommand());
        commands.add(new WithdrawCommand());
        commands.add(new SlotsCommand());
        commands.add(new SetMoneyCommand());
        commands.add(new CrashCommand());
        commands.add(new LoanCommand());
        commands.add(new DonateCommand());
        // commands.add(new PropertyCommand());

        return commands;
    }

    public Set<CoreCommand> getCommands() {
        return Set.of(this.commands.toArray(new CoreCommand[0]));
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        JDA jda = event.getJDA();

        if (this.commands.isEmpty()) {
            // Add all commands
            this.commands.addAll(createCommands());
            this.commands.forEach(command -> {
                jda.addEventListener(command);
                command.getSubcommands().forEach(jda::addEventListener);
            });

            // Register all global commands
            List<CoreCommand> globalCommands = this.commands.stream().filter(CoreCommand::isNotServerOnly).toList();
            registerCommands(jda, globalCommands);

            // Register all guild commands
            List<CoreCommand> guildCommands = this.commands.stream().filter(CoreCommand::isServerOnly).toList();
            for (Guild guild : event.getJDA().getGuilds()) {
                registerCommands(guild, guildCommands);
            }

            // Print command list
            printCommandList(jda, this.commands);
        }

        // Initialize all listeners
        init(jda);
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        Guild guild = event.getGuild();

        TextChannel generalChannel = guild.getTextChannels()
                .stream()
                .filter(channel -> channel.getName().equals("general"))
                .findFirst()
                .orElseGet(guild::getSystemChannel);

        GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(config);
        }

        if (config.isShouldSendStartupMessage()) {
            sendStartupMessage(generalChannel);
        }

        if (this.commands.isEmpty())
            return;

        List<CoreCommand> guildCommands = this.commands.stream().filter(CoreCommand::isServerOnly).toList();
        registerCommands(guild, guildCommands);
    }
}
