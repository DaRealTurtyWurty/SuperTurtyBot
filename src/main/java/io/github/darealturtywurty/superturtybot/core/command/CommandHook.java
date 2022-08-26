package io.github.darealturtywurty.superturtybot.core.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.darealturtywurty.superturtybot.commands.core.CommandListCommand;
import io.github.darealturtywurty.superturtybot.commands.core.EvalCommand;
import io.github.darealturtywurty.superturtybot.commands.core.HelpCommand;
import io.github.darealturtywurty.superturtybot.commands.core.PingCommand;
import io.github.darealturtywurty.superturtybot.commands.core.RestartCommand;
import io.github.darealturtywurty.superturtybot.commands.core.ShutdownCommand;
import io.github.darealturtywurty.superturtybot.commands.core.TagCommand;
import io.github.darealturtywurty.superturtybot.commands.core.config.ServerConfigCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.AdviceCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.CoinFlipCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.EightBallCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.InternetRulesCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.MemeCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.MinecraftUserSkinCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.MinecraftUserUUIDCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.MinecraftUsernameCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.ProgrammingMemeCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.ReverseTextCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.UpsideDownTextCommand;
import io.github.darealturtywurty.superturtybot.commands.fun.UrbanDictionaryCommand;
import io.github.darealturtywurty.superturtybot.commands.image.HttpCatCommand;
import io.github.darealturtywurty.superturtybot.commands.image.HttpDogCommand;
import io.github.darealturtywurty.superturtybot.commands.image.ImageCommand;
import io.github.darealturtywurty.superturtybot.commands.image.InspiroBotCommand;
import io.github.darealturtywurty.superturtybot.commands.levelling.LeaderboardCommand;
import io.github.darealturtywurty.superturtybot.commands.levelling.RankCommand;
import io.github.darealturtywurty.superturtybot.commands.levelling.XPInventoryCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.BeanCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.KickCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.PurgeCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.RemoveTimeoutCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.SlowmodeCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.TimeoutCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.UnbanCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.warnings.ClearWarningsCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.warnings.RemoveWarnCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.warnings.WarnCommand;
import io.github.darealturtywurty.superturtybot.commands.moderation.warnings.WarningsCommand;
import io.github.darealturtywurty.superturtybot.commands.music.ClearCommand;
import io.github.darealturtywurty.superturtybot.commands.music.JoinCommand;
import io.github.darealturtywurty.superturtybot.commands.music.LeaveCommand;
import io.github.darealturtywurty.superturtybot.commands.music.LyricsCommand;
import io.github.darealturtywurty.superturtybot.commands.music.NowPlayingCommand;
import io.github.darealturtywurty.superturtybot.commands.music.PauseCommand;
import io.github.darealturtywurty.superturtybot.commands.music.PlayCommand;
import io.github.darealturtywurty.superturtybot.commands.music.QueueCommand;
import io.github.darealturtywurty.superturtybot.commands.music.RemoveCommand;
import io.github.darealturtywurty.superturtybot.commands.music.RemoveDuplicatesCommand;
import io.github.darealturtywurty.superturtybot.commands.music.ResumeCommand;
import io.github.darealturtywurty.superturtybot.commands.music.SearchCommand;
import io.github.darealturtywurty.superturtybot.commands.music.ShuffleCommand;
import io.github.darealturtywurty.superturtybot.commands.music.SkipCommand;
import io.github.darealturtywurty.superturtybot.commands.music.VolumeCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiAnalCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiAssCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiBoobjobCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiBoobsCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiFoxCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiKemonomimiCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiMidriffCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiNekoCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiTentacleCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiThighCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.HentaiYaoiCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.LoliCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.NSFWCommandList;
import io.github.darealturtywurty.superturtybot.commands.nsfw.OrgasmCommand;
import io.github.darealturtywurty.superturtybot.commands.nsfw.Rule34Command;
import io.github.darealturtywurty.superturtybot.commands.util.BotInfoCommand;
import io.github.darealturtywurty.superturtybot.commands.util.CurseforgeCommand;
import io.github.darealturtywurty.superturtybot.commands.util.GithubRepositoryCommand;
import io.github.darealturtywurty.superturtybot.commands.util.HighlightCommand;
import io.github.darealturtywurty.superturtybot.commands.util.MojangStatusCommand;
import io.github.darealturtywurty.superturtybot.commands.util.PollCommand;
import io.github.darealturtywurty.superturtybot.commands.util.ReactionRoleCommand;
import io.github.darealturtywurty.superturtybot.commands.util.RolesCommand;
import io.github.darealturtywurty.superturtybot.commands.util.ServerInfoCommand;
import io.github.darealturtywurty.superturtybot.commands.util.StrawpollCommand;
import io.github.darealturtywurty.superturtybot.commands.util.StrawpollResultsCommand;
import io.github.darealturtywurty.superturtybot.commands.util.UserInfoCommand;
import io.github.darealturtywurty.superturtybot.commands.util.suggestion.ApproveSuggestionCommand;
import io.github.darealturtywurty.superturtybot.commands.util.suggestion.ConsiderSuggestionCommand;
import io.github.darealturtywurty.superturtybot.commands.util.suggestion.DenySuggestionCommand;
import io.github.darealturtywurty.superturtybot.commands.util.suggestion.SuggestCommand;
import io.github.darealturtywurty.superturtybot.modules.counting.RegisterCountingCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

public class CommandHook extends ListenerAdapter {
    protected static final Set<CommandCategory> CATEGORIES = new HashSet<>();
    protected static final Map<Long, Set<CoreCommand>> JDA_COMMANDS = new HashMap<>();
    public static final CommandHook INSTANCE = new CommandHook();
    
    private Set<CoreCommand> commands = new HashSet<>();
    
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
        final TextChannel cmdList = jda.getTextChannelsByName("command-list", true).get(0);
        if (cmdList == null)
            return;
        
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
            success.sendMessage("\n\nThere are **" + slashes.get() + "** slash commands.\nThere are **" + prefixes.get()
                + "** prefix commands.").queue();
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
        cmds.add(new ReactionRoleCommand());
        
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
        // cmds.add(new AmongUsCommand());
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
        
        jda.getGuilds().forEach(guild -> {
            final CommandListUpdateAction updates = guild.updateCommands();
            cmds.forEach(cmd -> registerCommand(cmd, updates, guild));
            updates.queue();
        });
        
        cmds.forEach(jda::addEventListener);
        
        return cmds;
    }
}
