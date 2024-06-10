package dev.darealturtywurty.superturtybot.commands.test;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommand;
import dev.darealturtywurty.superturtybot.commands.nsfw.NSFWCommandList;
import dev.darealturtywurty.superturtybot.commands.util.WikipediaCommand;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.RedditUtils;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import io.github.fastily.jwiki.core.NS;
import net.dean.jraw.ApiException;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

public class TestCommand extends CoreCommand {
    public TestCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.CORE;
    }

    @Override
    public String getDescription() {
        return "A test command!";
    }

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getRichName() {
        return "Test";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Optional<Long> ownerId = Environment.INSTANCE.ownerId();
        if (ownerId.isEmpty()) {
            reply(event, "❌ The owner ID is not set!", false, true);
            return;
        }

        if (event.getUser().getIdLong() != ownerId.get()) {
            reply(event, "❌ You must be the owner of the bot to use this command!", false, true);
            return;
        }

        reply(event, "✅ Running tests...");

        new Thread(() -> {
            try {
                List<String> failed = new ArrayList<>();
                for (NSFWCommandList.NSFWReddit command : NSFWCommand.NSFW_REDDIT_COMMANDS) {
                    Thread.sleep(3000);
                    String name = command.name();
                    String[] subreddits = command.subreddits();

                    if (subreddits.length < 1) {
                        event.getChannel().sendMessage("❌ The subreddits for the command `" + name + "` are empty!").queue();
                        continue;
                    }

                    for (String subreddit : subreddits) {
                        new Thread(() -> {
                            try {
                                SubredditReference subredditRef = RedditUtils.getSubreddit(subreddit);
                                try {
                                    subredditRef.about();
                                } catch (ApiException exception) {
                                    event.getChannel().sendMessage("❌ The subreddit `" + subreddit + "` for the command `" + name + "` does not exist!").queue();
                                    failed.add(subreddit);
                                    return;
                                }

                                RootCommentNode post = RedditUtils.findValidPost(subredditRef);
                                if(post == null) {
                                    event.getChannel().sendMessage("❌ The subreddit `" + subreddit + "` for the command `" + name + "` does not have any posts!").queue();
                                    failed.add(subreddit);
                                    return;
                                }

                                Either<EmbedBuilder, Collection<String>> result = RedditUtils.constructEmbed(true, post);
                                if (result == null) {
                                    event.getChannel().sendMessage("❌ The subreddit `" + subreddit + "` for the command `" + name + "` does not have any posts!").queue();
                                    failed.add(subreddit);
                                    return;
                                }

                                if (result.isLeft()) {
                                    var embed = result.getLeft().build();
                                    final String mediaURL = embed.getTitle();
                                    if (mediaURL == null) {
                                        event.getChannel().sendMessage("❌ Unable to get media URL for the subreddit `" + subreddit + "` for the command `" + name + "`!").queue();
                                        return;
                                    }

                                    if (RedditUtils.isEmbedVideo(mediaURL)) {
                                        event.getChannel().sendMessage(mediaURL).queue();
                                        return;
                                    }

                                    event.getChannel().sendMessage(embed.getTitle() == null ? "😘" : embed.getTitle()).setEmbeds(embed).queue();
                                } else {
                                    Collection<String> images = result.toOptional().orElse(List.of());
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
                                            event.getChannel().sendMessage("❌  There has been an error processing the command you tried to run. Please try again!").queue();
                                            Constants.LOGGER.error("Error getting image from URL: {}", image, exception);
                                            return;
                                        }
                                    }

                                    event.getChannel().sendMessage("Gallery 🖼️").setFiles(uploads).queue();
                                }
                            } catch (IllegalArgumentException exception) {
                                event.getChannel().sendMessage("❌ The subreddit `" + subreddit + "` for the command `" + name + "` does not have any posts!").queue();
                                failed.add(subreddit);
                            }
                        });

                        break;
                    }
                }

                if (failed.isEmpty()) {
                    event.getChannel().sendMessage("✅ All subreddits are valid!").queue();
                } else {
                    event.getChannel().sendMessage("❌ The following subreddits are invalid: " + String.join(", ", failed)).queue();
                }
            } catch (InterruptedException exception) {
                event.getChannel().sendMessage("❌ An error occurred while testing the subreddits!").queue();
            }

            event.getHook().editOriginal("✅ Tests have been completed!").queue();
        }).start();
    }
}
