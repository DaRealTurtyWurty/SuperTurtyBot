package dev.darealturtywurty.superturtybot.commands.nsfw;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Either;
import dev.darealturtywurty.superturtybot.core.util.RedditUtils;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NSFWCommand extends CoreCommand {
    private static final Set<NSFWCommandList.NSFWReddit> NSFW_REDDIT_COMMANDS = new HashSet<>();
    private static final Map<String, Consumer<NSFWCommandList.CommandData>> NSFW_OTHER_COMMANDS = new HashMap<>();

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
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
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
                    event.deferReply().setContent("Loading...").queue();
                    runNonReddit(NSFWCommandList.CommandData.from(event), subcommand);
                    return;
                }
            }

            event.deferReply().setContent("Loading...").queue();
            runReddit(event.getHook(), event.getUser(), group, subcommand);
            return;
        }

        event.deferReply().setContent("Loading...").queue();
        runNonReddit(NSFWCommandList.CommandData.from(event), subcommand);
    }

    private static void runNonReddit(NSFWCommandList.CommandData data, String subcommand) {
        final Consumer<NSFWCommandList.CommandData> command = NSFW_OTHER_COMMANDS.get(subcommand);
        if (command != null) {
            command.accept(data);
        } else {
            data.hook().editOriginal("You must specify a valid subcommand!").setComponents().setFiles().setEmbeds()
                    .queue();
        }
    }

    private static void runReddit(InteractionHook hook, User user, String group, String subcommand) {
        NSFWCommandList.NSFWReddit reddit = NSFW_REDDIT_COMMANDS.stream().filter(cmd -> cmd.name().equals(subcommand))
                .findFirst().orElse(null);
        if (reddit != null) {
            final Either<EmbedBuilder, Collection<String>> eitherEmbedOrImages = RedditUtils.constructEmbed(true,
                    reddit.subreddits());
            if (eitherEmbedOrImages == null) {
                hook.editOriginal("There has been an error processing the command you tried to run. Please try again!")
                        .setComponents().setEmbeds().setFiles().queue();
                return;
            }

            if (eitherEmbedOrImages.isRight()) {
                Collection<String> images = eitherEmbedOrImages.toOptional().orElse(List.of());
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
                        hook.editOriginal(
                                        "There has been an error processing the command you tried to run. Please try again!")
                                .setComponents().setEmbeds().setFiles().queue();
                        exception.printStackTrace();
                        return;
                    }
                }

                hook.editOriginal("Gallery 🖼️").setComponents().setEmbeds().setFiles(uploads)
                        .queue(msg -> addRegenerateButton(hook, user, group, subcommand));
                return;
            }

            EmbedBuilder embed = eitherEmbedOrImages.isLeft() ? eitherEmbedOrImages.getLeft() : null;
            if (embed == null) {
                hook.editOriginal("❌ There has been an error processing the command you tried to run. Please try again!")
                        .setComponents().setEmbeds().setFiles().queue();
                return;
            }

            final String mediaURL = embed.build().getTitle();
            if (mediaURL == null) {
                hook.editOriginal("❌ There has been an error processing the command you tried to run. Please try again!")
                        .setComponents().setEmbeds().setFiles().queue();
                return;
            }

            if (mediaURL.contains("redgifs") || mediaURL.contains("xvideos") || mediaURL.contains(
                    "xhamster") || mediaURL.contains("xxx") || mediaURL.contains("porn") || mediaURL.contains(
                    "nsfw") || mediaURL.contains("gfycat") || mediaURL.contains("/watch.") || mediaURL.contains(
                    "reddit.com") || mediaURL.contains("twitter") || mediaURL.contains("hub") || mediaURL.contains(
                    "imgur") || mediaURL.contains("youtube")) {
                hook.editOriginal(mediaURL).setComponents().setEmbeds().setFiles()
                        .queue(msg -> addRegenerateButton(hook, user, group, subcommand));
                return;
            }

            MessageEmbed builtEmbed = embed.build();
            hook.editOriginal(builtEmbed.getTitle() == null ? "😘" : builtEmbed.getTitle()).setComponents().setEmbeds()
                    .setFiles().flatMap(msg -> msg.editMessageEmbeds(builtEmbed))
                    .queue(msg -> addRegenerateButton(hook, user, group, subcommand));
        }
    }

    public static void addRegenerateButton(Message message, User user, String group, String subcommand) {
        message.editMessageComponents(ActionRow.of(
                Button.primary("regenerate-" + message.getId() + "-" + user.getId() + "-" + group + "-" + subcommand,
                        "🔁 Regenerate"))).queue();
    }

    public static void addRegenerateButton(InteractionHook hook, User user, String group, String subcommand) {
        hook.retrieveOriginal().queue(message -> hook.editOriginalComponents(ActionRow.of(
                Button.primary("regenerate-" + message.getId() + "-" + user.getId() + "-" + group + "-" + subcommand,
                        "🔁 Regenerate"))).queue());
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            event.reply("This command can only be used in a guild!").setEphemeral(true).queue();
            return;
        }

        if (!Objects.requireNonNull(event.getButton().getId()).startsWith("regenerate-")) return;

        String[] split = event.getButton().getId().split("-");
        if (split.length != 5) {
            event.reply("There has been an error processing the command you tried to run. Please try again!")
                    .setEphemeral(true).queue();
            return;
        }

        long messageId = Long.parseLong(split[1]);
        long userId = Long.parseLong(split[2]);
        String group = split[3];
        String subcommand = split[4];

        if (event.getMessageIdLong() != messageId) {
            return;
        }

        if (event.getUser().getIdLong() != userId) {
            event.reply("You do not have permission to regenerate this command!").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        if (NSFW_REDDIT_COMMANDS.stream().anyMatch(reddit -> reddit.name().equals(subcommand))) {
            if (group.equals("fake")) {
                if (NSFW_OTHER_COMMANDS.containsKey(subcommand)) {
                    runNonReddit(NSFWCommandList.CommandData.from(event), subcommand);
                    return;
                }
            }

            runReddit(event.getHook(), event.getUser(), group, subcommand);
            return;
        }

        runNonReddit(NSFWCommandList.CommandData.from(event), subcommand);
    }
}
