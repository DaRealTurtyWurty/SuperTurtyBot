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

public class DogBombCommand extends ImageCommand {
    public DogBombCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.IMAGE;
    }
    
    @Override
    public String getDescription() {
        return "Gets some random dog images";
    }
    
    @Override
    public ImageCategory getImageCategory() {
        return ImageCategory.ANIMAL;
    }
    
    @Override
    public String getName() {
        return "dogbomb";
    }
    
    @Override
    public String getRichName() {
        return "Dog Bomb";
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        final var replyFuture = new CompletableFuture<Long>();
        event.getChannel().sendMessage("Loading dogs...").queue(msg -> replyFuture.complete(msg.getIdLong()));
        final String[] subreddits = { "Dogs", "lookatmydog", "dogpictures", "dogswearinghats", "dogswitheyebrows",
            "DogsWithUnderbites", "woofbarkwoof", "beachdogs", "dogswatchingyoueat", "airedale", "akita",
            "alaskanmalamute", "Americanbulldog", "americaneskimo/", "AmericanBully", "AmStaffPitts",
            "australiancattledog", "Australianshepherd", "basenji", "basset", "beagle", "beardedcollies", "Beauceron",
            "bergerbelge", "bernesemountaindogs", "bichonfrise", "BlackMouthCur", "BorderCollie", "BorderTerrier",
            "BostonTerrier", "boxer", "BrittanySpaniel", "buhund", "bulldogs", "GifsofBulldogs", "bullterrier",
            "Catahoula", "AllAboutCBRs", "chihuahua", "chinesecrested", "chowchow", "clumberspaniels", "cockerspaniel",
            "coonhounds", "corgi", "dachshund", "dalmatians", "dobermanpinscher", "EnglishSetter", "frogdogs", "galgos",
            "germanshepherds", "goldenretrievers", "greatdanes", "greatpyrenees", "greyhounds", "hounds", "irishsetter",
            "italiangreyhounds", "jackrussellterrier", "jindo", "kangal", "kelpie", "labrador", "labradors", "labs",
            "leos", "maltese", "Mastiff", "MiniaturePinscher", "mutt", "neapolitanmastiff", "newfoundlander",
            "papillon", "pitbulls", "pomeranians", "poodles", "presacanario", "pug", "ratterriers", "Rottweiler",
            "roughcollies", "samoyeds", "schipperke", "schnauzers", "scottishterriers", "shiba", "shihtzu",
            "siberianhusky", "sighthounds", "springerspaniel", "StandardPoodles", "SwissMountainDogs", "tollers",
            "toyfoxterriers", "vizsla", "weimaraner", "welshterrier", "WestHighlandTerriers", "whippets",
            "xoloitzquintli" };

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
