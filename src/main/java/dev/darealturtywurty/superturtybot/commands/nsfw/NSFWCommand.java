package dev.darealturtywurty.superturtybot.commands.nsfw;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.RedditUtils;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NSFWCommand extends CoreCommand {
    public static final Set<NSFWCommandList.NSFWReddit> NSFW_REDDIT_COMMANDS = new HashSet<>();
    private static final Map<String, Consumer<NSFWCommandList.CommandData>> NSFW_OTHER_COMMANDS = new HashMap<>();

    static {
        NSFWCommandList.addAll(NSFW_REDDIT_COMMANDS);
        NSFWCommandList.addAll(NSFW_OTHER_COMMANDS);
    }

    public NSFWCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<SubcommandGroupData> createSubcommandGroupData() {
        return List.of(new SubcommandGroupData("real1", "Real NSFW commands (part 1)").addSubcommands(subcommand("ass"),
                        subcommand("porn"), subcommand("nsfw"), subcommand("gay"), subcommand("cock"), subcommand("pussy"),
                        subcommand("4k"), subcommand("anal"), subcommand("asian"), subcommand("bbc"), subcommand("bdsm"),
                        subcommand("boobs"), subcommand("cosplay"), subcommand("cum"), subcommand("feet"), subcommand("ebony"),
                        subcommand("gangbang"), subcommand("lesbian"), subcommand("interracial"), subcommand("gonewild"),
                        subcommand("pawg"), subcommand("public"), subcommand("teen"), subcommand("thigh"), subcommand("trap")),
                new SubcommandGroupData("real2", "Real NSFW commands (part 2)").addSubcommands(subcommand("boobjob"),
                        subcommand("petite"), subcommand("passion"), subcommand("hardcore"), subcommand("milf"),
                        subcommand("funny"), subcommand("femboy")),
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
        event.deferReply().queue();

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().editOriginal("You must specify a subcommand!").queue();
            return;
        }

        if (!isValidChannel(event.getChannel())) {
            event.getHook().editOriginal("This command can only be used in NSFW channels!").queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild != null) {
            GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong()))
                    .first();
            if (config == null) {
                config = new GuildData(guild.getIdLong());
                Database.getDatabase().guildData.insertOne(config);
            }

            List<Long> enabledChannels = GuildData.getLongs(config.getNsfwChannels());
            if (enabledChannels.isEmpty()) {
                event.getHook().editOriginal("❌ This server has no NSFW channels configured!").queue();
                return;
            }

            MessageChannel channel = event.getChannelType() == ChannelType.TEXT ? event.getChannel()
                    : event.getChannel().asThreadChannel().getParentMessageChannel();
            if (!enabledChannels.contains(channel.getIdLong())) {
                event.getHook().editOriginal("❌ This channel is not configured as an NSFW channel!").queue();
                return;
            }
        }

        String group = event.getSubcommandGroup();
        if (group == null) {
            event.getHook().editOriginal("❌ You must specify a subcommand group!").queue();
            return;
        }

        if (NSFW_REDDIT_COMMANDS.stream().anyMatch(reddit -> reddit.name().equals(subcommand))) {
            if (group.equals("fake")) {
                if (NSFW_OTHER_COMMANDS.containsKey(subcommand)) {
                    event.getHook().editOriginal("Loading...").queue();
                    runNonReddit(NSFWCommandList.CommandData.from(event), subcommand);
                    return;
                }
            }

            event.getHook().editOriginal("Loading...").queue();
            runReddit(event.getHook(), event.getUser(), group, subcommand);
            return;
        }

        event.deferReply().setContent("Loading...").queue();
        runNonReddit(NSFWCommandList.CommandData.from(event), subcommand);
    }

    public static boolean isValidChannel(MessageChannelUnion channel) {
        if(channel == null)
            return false;

        if(channel.getType() == ChannelType.UNKNOWN)
            return false;

        // if it's a private channel
        // TODO: Add a check for if the user is 18+ (and utilise the user config too)
        if(channel.getType() == ChannelType.PRIVATE)
            return true;

        // if it's a text channel and is nsfw
        if (channel.getType() == ChannelType.TEXT && channel.asTextChannel().isNSFW())
            return true;

        // if it's a thread channel and the parent is nsfw
        if ((channel.getType() == ChannelType.GUILD_PUBLIC_THREAD
                || channel.getType() == ChannelType.GUILD_PRIVATE_THREAD
                || channel.getType() == ChannelType.GUILD_NEWS_THREAD
                && channel.asThreadChannel().getParentMessageChannel().asTextChannel().isNSFW()))
            return true;

        // if it's a private channel
        return channel.getType() == ChannelType.PRIVATE;
    }

    private static void runNonReddit(NSFWCommandList.CommandData data, String subcommand) {
        final Consumer<NSFWCommandList.CommandData> command = NSFW_OTHER_COMMANDS.get(subcommand);
        if (command != null) {
            command.accept(data);
        } else {
            data.hook().editOriginal("❌  You must specify a valid subcommand!").setComponents().setFiles().setEmbeds()
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
                hook.editOriginal("❌  There has been an error processing the command you tried to run. Please try again!")
                        .setComponents().setEmbeds().setFiles().queue();
                return;
            }

            if (eitherEmbedOrImages.isRight()) {
                Collection<String> images = eitherEmbedOrImages.toOptional().orElse(List.of());
                List<FileUpload> uploads = new ArrayList<>();

                int index = 0;
                for (String image : images) {
                    try {
                        URLConnection connection = new URI(image).toURL().openConnection();
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                        connection.connect();
                        uploads.add(
                                FileUpload.fromData(connection.getInputStream(), "image_%d.png".formatted(index++)));
                    } catch (IOException | URISyntaxException exception) {
                        hook.editOriginal(
                                        "❌  There has been an error processing the command you tried to run. Please try again!")
                                .setComponents().setEmbeds().setFiles().queue();
                        Constants.LOGGER.error("Error getting image from URL: {}", image, exception);
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

            if (RedditUtils.isEmbedVideo(mediaURL)) {
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
            event.reply("❌ This command can only be used in a guild!").setEphemeral(true).queue();
            return;
        }

        if (!Objects.requireNonNull(event.getButton().getCustomId()).startsWith("regenerate-")) return;

        String[] split = event.getButton().getCustomId().split("-");
        if (split.length != 5) {
            event.reply("❌ There has been an error processing the command you tried to run. Please try again!")
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
            event.reply("❌ You do not have permission to regenerate this command!").setEphemeral(true).queue();
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
