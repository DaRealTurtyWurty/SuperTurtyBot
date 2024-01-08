
package dev.darealturtywurty.superturtybot.commands.fun;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.RedditUtils;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.TimeUnit;

public class MemeCommand extends CoreCommand {
    public MemeCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }
    
    @Override
    public String getDescription() {
        return "Gets some hot memes";
    }
    
    @Override
    public String getName() {
        return "meme";
    }
    
    @Override
    public String getRichName() {
        return "Meme";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        reply(event, "Loading meme...");
        final String[] subreddits = { "memes", "dankmemes", "blackpeopletwitter", "memeeconomy", "me_irl",
            "adviceanimals", "deepfriedmemes", "surrealmemes", "nukedmemes", "bigbangedmemes", "wackytictacs",
            "bonehurtingjuice" };
        
        final SubredditReference subreddit = RedditUtils.getRandomSubreddit(subreddits);
        final RootCommentNode post = RedditUtils.findValidPost(subreddit, subreddits);
        if(post == null) {
            event.getHook().editOriginal("❌ Unable to find a valid post!").queue();
            return;
        }

        final String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
            : post.getSubject().getUrl();
        if (mediaURL == null) {
            event.getHook().editOriginal("❌ Unable to find a valid post!").queue();
            return;
        }

        event.getHook().editOriginal(mediaURL).queue();
    }
}
