package dev.darealturtywurty.superturtybot.core.command;

import dev.darealturtywurty.superturtybot.commands.core.*;
import dev.darealturtywurty.superturtybot.commands.core.config.ServerConfigCommand;
import dev.darealturtywurty.superturtybot.commands.fun.*;
import dev.darealturtywurty.superturtybot.commands.image.HttpCatCommand;
import dev.darealturtywurty.superturtybot.commands.image.HttpDogCommand;
import dev.darealturtywurty.superturtybot.commands.image.ImageCommand;
import dev.darealturtywurty.superturtybot.commands.image.InspiroBotCommand;
import dev.darealturtywurty.superturtybot.commands.levelling.LeaderboardCommand;
import dev.darealturtywurty.superturtybot.commands.levelling.RankCommand;
import dev.darealturtywurty.superturtybot.commands.levelling.XPInventoryCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.*;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.ClearWarningsCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.RemoveWarnCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarnCommand;
import dev.darealturtywurty.superturtybot.commands.moderation.warnings.WarningsCommand;
import dev.darealturtywurty.superturtybot.commands.music.*;
import dev.darealturtywurty.superturtybot.commands.nsfw.*;
import dev.darealturtywurty.superturtybot.commands.util.*;
import dev.darealturtywurty.superturtybot.commands.util.suggestion.ApproveSuggestionCommand;
import dev.darealturtywurty.superturtybot.commands.util.suggestion.ConsiderSuggestionCommand;
import dev.darealturtywurty.superturtybot.commands.util.suggestion.DenySuggestionCommand;
import dev.darealturtywurty.superturtybot.commands.util.suggestion.SuggestCommand;
import dev.darealturtywurty.superturtybot.modules.counting.RegisterCountingCommand;
import dev.darealturtywurty.superturtybot.modules.economy.*;
import dev.darealturtywurty.superturtybot.weblisteners.social.RedditListener;
import dev.darealturtywurty.superturtybot.weblisteners.social.SteamListener;
import dev.darealturtywurty.superturtybot.weblisteners.social.TwitchListener;
import dev.darealturtywurty.superturtybot.weblisteners.social.YouTubeListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CommandHook extends ListenerAdapter {
    protected static final Set<CommandCategory> CATEGORIES = new HashSet<>();
    protected static final Map<Long, Set<CoreCommand>> JDA_COMMANDS = new HashMap<>();
    public static final CommandHook INSTANCE = new CommandHook();

    private final Set<CoreCommand> commands = new HashSet<>();

    private CommandHook() {
    }

    public Set<CoreCommand> getCommands() {
        return Set.of(this.commands.toArray(new CoreCommand[0]));
    }

    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
        this.commands.clear();
        this.commands.addAll(registerCommands(event.getJDA()));

        printCommandList(event.getJDA(), this.commands);

        if (!YouTubeListener.isRunning()) {
            YouTubeListener.runExecutor(event.getJDA());
        }

        if (!TwitchListener.isInitialized()) {
            TwitchListener.initialize(event.getJDA());
        }

        if (!SteamListener.isRunning()) {
            SteamListener.runExecutor(event.getJDA());
        }

        if (!RedditListener.isInitialized()) {
            RedditListener.initialize(event.getJDA());
        }

        // TODO: Fix this mf
        // TwitterListener.setup();

        if (!EconomyManager.isRunning()) {
            EconomyManager.start(event.getJDA());
        }
    }

    protected static void registerCommand(CoreCommand cmd, CommandListUpdateAction updates, Guild guild) {
        if (cmd.types.slash()) {
            final SlashCommandData data = Commands.slash(cmd.getName(), cmd.getDescription());
            final List<OptionData> options = cmd.createOptions();
            if (!options.isEmpty()) {
                data.addOptions(options);
            }

            final List<SubcommandData> subcommands = cmd.createSubcommands();
            if (!subcommands.isEmpty()) {
                data.addSubcommands(subcommands);
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

    private static void printCommandList(JDA jda, Set<CoreCommand> cmds) {
        final List<TextChannel> channels = jda.getTextChannelsByName("command-list", true);
        if (channels.isEmpty()) return;

        final TextChannel cmdList = channels.get(0);
        if (cmdList == null) return;

        final var builder = new StringBuilder();
        final var previous = new AtomicReference<CoreCommand>();
        final var slashes = new AtomicInteger();
        final var prefixes = new AtomicInteger();
        cmds.stream()
            .sorted((cmd0, cmd1) -> cmd0.getCategory().getName().compareToIgnoreCase(cmd1.getCategory().getName()))
            .forEach(cmd -> {
                if (previous.get() != null && !previous.get().getCategory().equals(cmd.getCategory())) {
                    builder.append("\n**" + cmd.getCategory().getName() + "**\n");
                } else if (previous.get() == null) {
                    builder.append("**" + cmd.getCategory().getName() + "**\n");
                }

                builder.append("`" + (cmd.types.slash() ? "/" : ".") + cmd.getName() + "`\n");
                previous.set(cmd);

                if (cmd.types.slash()) {
                    slashes.incrementAndGet();
                } else {
                    prefixes.incrementAndGet();
                }
            });

        cmdList.createCopy().setPosition(cmdList.getPosition()).queue(success -> {
            success.sendMessage(builder.toString()).queue();
            success.sendMessage(
                           "\n\nThere are **" + slashes.get() + "** slash commands.\nThere are **" + prefixes.get() + "** prefix commands.")
                   .queue();
            cmdList.delete().queue();
        });
    }

    private static Set<CoreCommand> registerCommands(JDA jda) {
        final Set<CoreCommand> cmds = new HashSet<>();
        // Core
        cmds.add(new PingCommand());
        cmds.add(new HelpCommand());
        cmds.add(new CommandListCommand());
        cmds.add(new TagCommand());
        cmds.add(new EvalCommand());
        cmds.add(new ShutdownCommand());
        cmds.add(new RestartCommand());
        cmds.add(new ServerConfigCommand());
        // cmds.add(new UserConfigCommand());

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
        cmds.add(new ApproveSuggestionCommand());
        cmds.add(new DenySuggestionCommand());
        cmds.add(new ConsiderSuggestionCommand());
        cmds.add(new MojangStatusCommand());
        cmds.add(new HighlightCommand());
        cmds.add(new RoleSelectionCommand());
        cmds.add(new TopicCommand());
        cmds.add(new WouldYouRatherCommand());
        cmds.add(new NotifierCommand());
        cmds.add(new PeriodicTableCommand());
        cmds.add(new FactCommand());

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
        cmds.add(new BeanCommand());
        cmds.add(new RegisterCountingCommand());
        cmds.add(new ReportCommand());
        cmds.add(new ReportsCommand());

        // NSFW
        NSFWCommandList.addAll(cmds);
        cmds.add(new HentaiCommand());
        cmds.add(new HentaiAnalCommand());
        cmds.add(new HentaiAssCommand());
        cmds.add(new HentaiBoobjobCommand());
        cmds.add(new HentaiBoobsCommand());
        cmds.add(new HentaiFoxCommand());
        cmds.add(new HentaiKemonomimiCommand());
        cmds.add(new HentaiMidriffCommand());
        cmds.add(new HentaiNekoCommand());
        cmds.add(new HentaiTentacleCommand());
        cmds.add(new HentaiThighCommand());
        cmds.add(new HentaiYaoiCommand());
        cmds.add(new LoliCommand());
        cmds.add(new OrgasmCommand());
        cmds.add(new Rule34Command());

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

        // Image
        cmds.add(new HttpCatCommand());
        cmds.add(new MemeCommand());
        cmds.add(new ProgrammingMemeCommand());
        cmds.add(new InspiroBotCommand());
        cmds.add(new HttpDogCommand());
        cmds.add(new ImageCommand());

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
        cmds.add(new XPInventoryCommand());

        // Economy
        cmds.add(new BalanceCommand());
        cmds.add(new RobCommand());
        cmds.add(new RewardCommand());
        cmds.add(new WithdrawCommand());
        cmds.add(new DepositCommand());
        cmds.add(new CrimeCommand());
        cmds.add(new SexWorkCommand());
        cmds.add(new JobCommand());

        jda.getGuilds().forEach(guild -> {
            final CommandListUpdateAction updates = guild.updateCommands();
            cmds.forEach(cmd -> registerCommand(cmd, updates, guild));
            updates.queue();
        });

        cmds.forEach(jda::addEventListener);

        return cmds;
    }
}
