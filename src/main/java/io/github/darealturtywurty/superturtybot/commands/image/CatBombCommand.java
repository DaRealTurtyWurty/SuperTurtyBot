
package io.github.darealturtywurty.superturtybot.commands.image;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.util.RedditUtils;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;

public class CatBombCommand extends ImageCommand {
    public CatBombCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Gets some random cat images";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "catbomb";
    }
    
    @Override
    public String getRichName() {
        return "Cat Bomb";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        final var replyFuture = new CompletableFuture<Long>();
        event.getChannel().sendMessage("Loading cats...").queue(msg -> replyFuture.complete(msg.getIdLong()));
        final String[] subreddits = { "Cats", "Kitten", "CatSpotting", "IllegallySmolCats", "IllegallyBigCats",
            "IllegallyLongCats", "Kitty", "GrumpyCats", "TruckerCats", "FromKittenToCat", "WetCats", "SeniorCats",
            "SleepingCats", "Displeased_Kitties", "FluffyCats", "SuspiciousCats", "CatsCalledFood", "catshuggingcats",
            "SadCats", "PirateKitties", "meowormyson", "goodcats", "pimpcats", "C_AT", "thisismylifemeow", "fatcats",
            "nowmycat", "divorcedcats", "notmycat", "ChristmasCats", "scrungycats", "AdorableCats", "AliveNamedCats",
            "Medieval_Cats" };
        
        final var actions = new ArrayList<RestAction<Message>>();
        for (int index = 0; index < ThreadLocalRandom.current().nextInt(3, 7); index++) {
            final SubredditReference subreddit = RedditUtils.getRandomSubreddit(subreddits);
            final RootCommentNode post = RedditUtils.findValidPost(subreddit, subreddits);
            final String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
                : post.getSubject().getUrl();
            actions.add(event.getChannel().sendMessage(mediaURL));
        }
        
        RestAction.allOf(actions)
            .queue(msgs -> replyFuture.thenAccept(id -> event.getChannel().deleteMessageById(id).queue()));
        event.getMessage().delete().queue();
    }
}
