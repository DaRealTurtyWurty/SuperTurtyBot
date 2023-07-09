
package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.RedditUtils;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class ProgrammingMemeCommand extends CoreCommand {
    public ProgrammingMemeCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }
    
    @Override
    public String getDescription() {
        return "Gets some hot programming memes";
    }
    
    @Override
    public String getName() {
        return "programmingmeme";
    }
    
    @Override
    public String getRichName() {
        return "Programming Meme";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().setContent("Loading meme...").queue();
        final String[] subreddits = { "ProgrammerHumor", "programmingmemes" };
        
        final SubredditReference subreddit = RedditUtils.getRandomSubreddit(subreddits);
        final RootCommentNode post = RedditUtils.findValidPost(subreddit, subreddits);
        final String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
            : post.getSubject().getUrl();
        event.getHook().editOriginal(mediaURL).queue();
    }
}
