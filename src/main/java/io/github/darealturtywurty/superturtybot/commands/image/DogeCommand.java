package io.github.darealturtywurty.superturtybot.commands.image;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.RedditUtils;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DogeCommand extends ImageCommand {
    public DogeCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Gets a random doge image";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "doge";
    }
    
    @Override
    public String getRichName() {
        return "Doge";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        final SubredditReference subreddit = RedditUtils.getSubreddit("Doge");
        final RootCommentNode post = RedditUtils.findValidPost(subreddit, "Doge");
        final String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
            : post.getSubject().getUrl();
        event.getMessage().reply(mediaURL).mentionRepliedUser(false).queue();
    }
}
