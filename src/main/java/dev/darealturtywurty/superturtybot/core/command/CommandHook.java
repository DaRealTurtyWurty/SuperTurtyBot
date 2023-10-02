package dev.darealturtywurty.superturtybot.core.command;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.commands.core.*;
import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigCommand;
import dev.darealturtywurty.superturtybot.commands.core.config.UserConfigCommand;
import dev.darealturtywurty.superturtybot.commands.economy.*;
import dev.darealturtywurty.superturtybot.commands.fun.*;
import dev.darealturtywurty.superturtybot.commands.image.*;
import dev.darealturtywurty.superturtybot.commands.levelling.LeaderboardCommand;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCommand;
import dev.darealturtywurty.superturtybot.commands.minigames.*;
import dev.darealturtywurty.superturtybot.commands.moderation.*;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.ClearWarningsCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.RemoveWarnCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarnCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarningsCommand;
import dev.darealturtywurty.superturtybot.commands.music.*;
import dev.darealturtywurty.superturtybot.commands.nsfw.GuessSexPositionCommand;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.commands.util.*;
import dev.darealturtywurty.superturtybot.commands.util.suggestion.SuggestCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.AutoModerator;
import dev.darealturtywurty.superturtybot.modules.ChangelogFetcher;
import dev.darealturtywurty.superturtybot.modules.counting.RegisterCountingCommand;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.weblisteners.social.*;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CommandHook extends ListenerAdapter {
    public static final CommandHook INSTANCE = new CommandHook();

    private static final String STARTUP_MESSAGE = "Initiating... Startup... Sequence.. Hello! I'm TurtyBot. I have a bunch of commands you can use, and I'm always adding more! You can see all of my commands by typing `/commands` in any channel that you and I can access.";
    private static final Set<CommandCategory> CATEGORIES = new HashSet<>();

    private static final CompletableFuture<Boolean> CHECKING_FOR_DEV_GUILD = new CompletableFuture<>();
    private static boolean CHECKED_FOR_DEV_GUILD = false;
    private static boolean IS_DEV_MODE = false;

    static {
        CHECKING_FOR_DEV_GUILD.thenAccept(ignored -> CHECKED_FOR_DEV_GUILD = true);
    }

    private final Set<CoreCommand> commands = new HashSet<>();

    private CommandHook() {
    }

    public static Set<CommandCategory> getCategories() {
        return CATEGORIES;
    }

    public static boolean isDevMode() {
        return IS_DEV_MODE;
    }

    public static boolean isCheckingForDevGuild() {
        return !CHECKED_FOR_DEV_GUILD;
    }

    public static CompletableFuture<Boolean> getCheckingForDevGuild() {
        return CHECKING_FOR_DEV_GUILD;
    }

    public Set<CoreCommand> getCommands() {
        return Set.of(this.commands.toArray(new CoreCommand[0]));
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        JDA jda = event.getJDA();

        if(this.commands.isEmpty()) {
            // Add all commands
            this.commands.addAll(createCommands());
            this.commands.forEach(jda::addEventListener);

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

        GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong())).first();
        if (config == null) {
            config = new GuildConfig(guild.getIdLong());
            Database.getDatabase().guildConfig.insertOne(config);
        }

        if(config.isShouldSendStartupMessage()) {
            sendStartupMessage(generalChannel);
        }

        if(this.commands.isEmpty())
            return;

        List<CoreCommand> guildCommands = this.commands.stream().filter(CoreCommand::isServerOnly).toList();
        registerCommands(guild, guildCommands);
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

        if (!RedditListener.isInitialized()) {
            RedditListener.initialize(jda);
        }

        if(!EconomyManager.isRunning()) {
            EconomyManager.start(jda);
        }

        // TODO: Fix this mf
//        if (!TwitterListener.isInitialized()) {
//            TwitterListener.initialize(jda);
//        }

        Guild devGuild = jda.getGuildById(1096109606452867243L);
        if (devGuild != null) {
            IS_DEV_MODE = true;
        }

        CHECKING_FOR_DEV_GUILD.complete(isDevMode());

        if (!isDevMode()) {
            AutoModerator.INSTANCE.initialize();
        }
    }

    private static void sendStartupMessage(@Nullable TextChannel channel) {
        if (channel == null) return;

        String changelog = ChangelogFetcher.INSTANCE.appendChangelog(STARTUP_MESSAGE);
        channel.sendMessage(changelog).queue();
    }

    private static void printCommandList(JDA jda, Set<CoreCommand> cmds) {
        final List<TextChannel> channels = jda.getTextChannelsByName("command-list", true);
        if (channels.isEmpty()) return;

        final TextChannel cmdList = channels.get(0);
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

            if(cmd.types.normal()) {
                prefixes.incrementAndGet();
            }

            if(cmd.types.messageCtx()) {
                messageCtx.incrementAndGet();
            }

            if(cmd.types.userCtx()) {
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

            List<SubcommandGroupData> subcommandGroupData = cmd.createSubcommandGroups();
            if (!subcommandGroupData.isEmpty()) {
                data.addSubcommandGroups(subcommandGroupData);
            } else {
                final List<SubcommandData> subcommands = cmd.createSubcommands();
                if (!subcommands.isEmpty()) {
                    data.addSubcommands(subcommands);
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
        final Set<CoreCommand> cmds = new HashSet<>();
        // Core
        cmds.add(new PingCommand());
        cmds.add(new HelpCommand());
        cmds.add(new CommandListCommand());
        cmds.add(new TagCommand());
        cmds.add(new ShutdownCommand());
        cmds.add(new RestartCommand());
        cmds.add(new GuildConfigCommand());
        cmds.add(new UserConfigCommand());
        cmds.add(new OptCommand());
        cmds.add(new UptimeCommand());
        //cmds.add(new SystemStatsCommand());
        //cmds.add(new TestCommand());

        // Utility
        cmds.add(new BotInfoCommand());
        cmds.add(new UserInfoCommand());
        cmds.add(new ServerInfoCommand());
        cmds.add(new PollCommand());
        cmds.add(new StrawpollCommand());
        cmds.add(new StrawpollResultsCommand());
        cmds.add(new RolesCommand());
        cmds.add(new GithubRepositoryCommand());
        cmds.add(new CurseforgeCommand());
        cmds.add(new SuggestCommand());
        cmds.add(new HighlightCommand());
        cmds.add(new RoleSelectionCommand());
        cmds.add(new TopicCommand());
        cmds.add(new WouldYouRatherCommand());
        cmds.add(new NotifierCommand());
        cmds.add(new PeriodicTableCommand());
        cmds.add(new FactCommand());
        cmds.add(new QuoteCommand());
        cmds.add(new LatestCommand());
        cmds.add(new AnalyzeLogCommand());
        cmds.add(new EmbedCommand());
        cmds.add(new ReminderCommand());
        cmds.add(new RainbowSixStatusCommand());
        cmds.add(new WeatherCommand());
        cmds.add(new SteamGamesCommand());
        cmds.add(new SteamIDCommand());
        cmds.add(new GetRobloxUserAvatarCommand());
        cmds.add(new GetRobloxUserNameCommand());
        cmds.add(new GetRobloxUserFriendListCommand());
        cmds.add(new GetRobloxUserFavoriteGameCommand());

        // Moderation
        cmds.add(new BanCommand());
        cmds.add(new UnbanCommand());
        cmds.add(new TimeoutCommand());
        cmds.add(new RemoveTimeoutCommand());
        cmds.add(new KickCommand());
        cmds.add(new PurgeCommand());
        cmds.add(new WarnCommand());
        cmds.add(new RemoveWarnCommand());
        cmds.add(new ClearWarningsCommand());
        cmds.add(new WarningsCommand());
        cmds.add(new SlowmodeCommand());
        cmds.add(new RegisterCountingCommand());
        cmds.add(new ReportCommand());
        cmds.add(new ReportsCommand());

        // NSFW
        cmds.add(new NSFWCommand());
        cmds.add(new GuessSexPositionCommand());

        // Music
        cmds.add(new JoinCommand());
        cmds.add(new LeaveCommand());
        cmds.add(new PlayCommand());
        cmds.add(new NowPlayingCommand());
        cmds.add(new QueueCommand());
        cmds.add(new RemoveCommand());
        cmds.add(new SkipCommand());
        cmds.add(new PauseCommand());
        cmds.add(new ResumeCommand());
        cmds.add(new VolumeCommand());
        cmds.add(new ShuffleCommand());
        cmds.add(new ClearCommand());
        cmds.add(new SearchCommand());
        cmds.add(new LyricsCommand());
        cmds.add(new RemoveDuplicatesCommand());
        cmds.add(new LeaveCleanupCommand());
        cmds.add(new LoopCommand());
        cmds.add(new MusicRestartCommand());
        cmds.add(new SeekCommand());
        cmds.add(new MoveCommand());
        cmds.add(new VoteSkipCommand());
        cmds.add(new SaveSongCommand());
        //cmds.add(new FilterCommand());
        //cmds.add(new FilterConfigCommand());
        // TODO: Come back to filters at some point

        // Image
        cmds.add(new HttpCatCommand());
        cmds.add(new MemeCommand());
        cmds.add(new ProgrammingMemeCommand());
        cmds.add(new InspiroBotCommand());
        cmds.add(new HttpDogCommand());
        cmds.add(new ImageCommand());
        cmds.add(new DeepfryCommand());
        cmds.add(new CatSaysCommand());
        cmds.add(new FlagifyCommand());
        cmds.add(new LGBTifyCommand());

        // Fun
        cmds.add(new AdviceCommand());
        cmds.add(new CoinFlipCommand());
        cmds.add(new EightBallCommand());
        cmds.add(new InternetRulesCommand());
        cmds.add(new ReverseTextCommand());
        cmds.add(new UpsideDownTextCommand());
        cmds.add(new UrbanDictionaryCommand());
        cmds.add(new MinecraftUsernameCommand());
        cmds.add(new MinecraftUserUUIDCommand());
        cmds.add(new MinecraftUserSkinCommand());

        // Levelling
        cmds.add(new RankCommand());
        cmds.add(new LeaderboardCommand());
        //cmds.add(new XPInventoryCommand());

        // Minigames
        cmds.add(new TriviaCommand());
        cmds.add(new GuessCombinedFlagsCommand());
        cmds.add(new GuessSongCommand());
        cmds.add(new GuessRegionBorderCommand());
        cmds.add(new HigherLowerCommand());
        cmds.add(new WordleCommand());

        // Economy
        cmds.add(new BalanceCommand());
        cmds.add(new CrimeCommand());
        cmds.add(new DepositCommand());
        cmds.add(new JobCommand());
        cmds.add(new RewardCommand());
        cmds.add(new RobCommand());
        cmds.add(new SexWorkCommand());
        cmds.add(new ShopCommand());
        cmds.add(new WithdrawCommand());
        cmds.add(new SlotsCommand());
        cmds.add(new SetMoneyCommand());
        cmds.add(new CrashCommand());
        cmds.add(new LoanCommand());
        cmds.add(new DonateCommand());

        return cmds;
    }
}
