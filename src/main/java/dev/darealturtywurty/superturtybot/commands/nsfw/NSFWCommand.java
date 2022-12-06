package dev.darealturtywurty.superturtybot.commands.nsfw;

import com.codepoetics.ambivalence.Either;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.RedditUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.function.Consumer;

public class NSFWCommand extends CoreCommand {
    private static final Set<NSFWCommandList.NSFWReddit> NSFW_REDDIT_COMMANDS = new HashSet<>();
    private static final Map<String, Consumer<SlashCommandInteractionEvent>> NSFW_OTHER_COMMANDS = new HashMap<>();

    static {
        NSFWCommandList.addAll(NSFW_REDDIT_COMMANDS);
        NSFWCommandList.addAll(NSFW_OTHER_COMMANDS);
    }

    public NSFWCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandGroupData> createSubcommandGroups() {
        return List.of(new SubcommandGroupData("real1", "Real NSFW commands (part 1)").addSubcommands(subcommand("ass"),
                        subcommand("porn"), subcommand("nsfw"), subcommand("gay"), subcommand("cock"), subcommand("pussy"),
                        subcommand("4k"), subcommand("anal"), subcommand("asian"), subcommand("bbc"), subcommand("bdsm"),
                        subcommand("boobs"), subcommand("cosplay"), subcommand("cum"), subcommand("feet"), subcommand("ebony"),
                        subcommand("gangbang"), subcommand("lesbian"), subcommand("interracial"), subcommand("gonewild"),
                        subcommand("pawg"), subcommand("public"), subcommand("teen"), subcommand("thigh"), subcommand("trap")),
                new SubcommandGroupData("real2", "Real NSFW commands (part 2)").addSubcommands(subcommand("boobjob"),
                        subcommand("petite"), subcommand("passion"), subcommand("hardcore"), subcommand("milf"),
                        subcommand("funny")),
                new SubcommandGroupData("fake", "Fake NSFW commands").addSubcommands(subcommand("furry"),
                        subcommand("hentaigif"), subcommand("yuri"), subcommand("oppai"), subcommand("r6s"),
                        subcommand("apex"), subcommand("overwatch"), subcommand("valorant"), subcommand("hentai"),
                        subcommand("anal"), subcommand("ass"), subcommand("boobjob"), subcommand("boobs"),
                        subcommand("fox"), subcommand("kemonomimi"), subcommand("midriff"), subcommand("neko"),
                        subcommand("tentacle"), subcommand("thigh"), subcommand("yaoi"), subcommand("loli")),
                new SubcommandGroupData("misc", "Miscellaneous NSFW commands").addSubcommands(subcommand("random"),
                        subcommand("orgasm"),
                        subcommand("rule34").addOption(OptionType.STRING, "search_term", "The rule34 search phrase",
                                true)));
    }

    private static SubcommandData subcommand(String name) {
        return new SubcommandData(name, name);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.NSFW;
    }

    @Override
    public String getDescription() {
        return "Runs the specified NSFW command.";
    }

    @Override
    public String getName() {
        return "nsfw";
    }

    @Override
    public String getRichName() {
        return "NSFW (Not Safe For Work - 18+)";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.deferReply(true).setContent("You must specify a subcommand!").queue();
            return;
        }

        if (!event.getChannel().asTextChannel().isNSFW()) {
            event.deferReply(true).setContent("This command can only be used in NSFW channels!").queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild != null) {
            GuildConfig config = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
                    .first();
            if (config == null) {
                event.deferReply(true).setContent("This server has not been configured yet!").queue();
                return;
            }

            List<Long> enabledChannels = GuildConfig.getChannels(config.getNsfwChannels());
            if (enabledChannels.isEmpty()) {
                event.deferReply(true).setContent("This server has no NSFW channels configured!").queue();
                return;
            }

            System.out.println(config.getNsfwChannels());
            System.out.println(event.getChannel().getIdLong());
            if (!enabledChannels.contains(event.getChannel().getIdLong())) {
                event.deferReply(true).setContent("This channel is not configured as an NSFW channel!").queue();
                return;
            }
        }

        String group = event.getSubcommandGroup();
        if (group == null) {
            event.deferReply(true).setContent("You must specify a subcommand group!").queue();
            return;
        }

        if (NSFW_REDDIT_COMMANDS.stream().anyMatch(reddit -> reddit.name().equals(subcommand))) {
            if (group.equals("fake")) {
                if (NSFW_OTHER_COMMANDS.containsKey(subcommand)) {
                    runNonReddit(event, subcommand);
                    return;
                }
            }

            runReddit(event, subcommand);
            return;
        }

        runNonReddit(event, subcommand);
    }

    private static void runNonReddit(SlashCommandInteractionEvent event, String subcommand) {
        final Consumer<SlashCommandInteractionEvent> command = NSFW_OTHER_COMMANDS.get(subcommand);
        if (command != null) {
            command.accept(event);
        } else {
            event.deferReply(true).setContent("You must specify a valid subcommand!").queue();
        }
    }

    private static void runReddit(SlashCommandInteractionEvent event, String subcommand) {
        NSFWCommandList.NSFWReddit reddit = NSFW_REDDIT_COMMANDS.stream().filter(cmd -> cmd.name().equals(subcommand))
                .findFirst().orElse(null);
        if (reddit != null) {
            event.deferReply().setContent("Loading...").queue();

            final Either<EmbedBuilder, Collection<String>> eitherEmbedOrImages = RedditUtils.constructEmbed(true,
                    reddit.subreddits());
            if (eitherEmbedOrImages == null) {
                event.getHook().editOriginal(
                        "There has been an error processing the command you tried to run. Please try again!").queue();
                return;
            }

            if (eitherEmbedOrImages.isRight()) {
                Collection<String> images = eitherEmbedOrImages.right().orElse(List.of());
                List<FileUpload> uploads = new ArrayList<>();

                int index = 0;
                for (String image : images) {
                    try {
                        URLConnection connection = new URL(image).openConnection();
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                        connection.connect();
                        uploads.add(
                                FileUpload.fromData(connection.getInputStream(), "image_%d.png".formatted(index++)));
                    } catch (IOException exception) {
                        event.getHook().editOriginal(
                                        "There has been an error processing the command you tried to run. Please try again!")
                                .queue();
                        exception.printStackTrace();
                        return;
                    }
                }

                event.getHook().editOriginal("Gallery 🖼️").flatMap(msg -> msg.replyFiles(uploads)).queue();
                return;
            }

            EmbedBuilder embed = eitherEmbedOrImages.left().orElse(null);
            final String mediaURL = embed.build().getTitle();
            if (mediaURL == null) {
                event.getHook().editOriginal(
                        "There has been an error processing the command you tried to run. Please try again!").queue();
                return;
            }

            if (mediaURL.contains("redgifs") || mediaURL.contains("xvideos") || mediaURL.contains(
                    "xhamster") || mediaURL.contains("xxx") || mediaURL.contains("porn") || mediaURL.contains(
                    "nsfw") || mediaURL.contains("gfycat") || mediaURL.contains("/watch.") || mediaURL.contains(
                    "reddit.com") || mediaURL.contains("twitter") || mediaURL.contains("hub") || mediaURL.contains(
                    "imgur") || mediaURL.contains("youtube")) {
                event.getHook().editOriginal(mediaURL).queue();
                return;
            }

            MessageEmbed builtEmbed = embed.build();
            event.getHook().editOriginal(builtEmbed.getTitle() == null ? "😘" : builtEmbed.getTitle())
                    .flatMap(msg -> msg.editMessageEmbeds(builtEmbed)).queue();
        }
    }
}
