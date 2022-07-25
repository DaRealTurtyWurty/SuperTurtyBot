package io.github.darealturtywurty.superturtybot.commands.image;

import static io.github.darealturtywurty.superturtybot.commands.image.AbstractImageCommand.ImageCategory.ANIMAL;
import static io.github.darealturtywurty.superturtybot.commands.image.AbstractImageCommand.ImageCategory.FUN;
import static io.github.darealturtywurty.superturtybot.commands.image.AbstractImageCommand.ImageCategory.SCENERY;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.JsonObject;

import io.github.darealturtywurty.superturtybot.commands.image.AbstractImageCommand.ImageCategory;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.core.util.RedditUtils;
import io.github.darealturtywurty.superturtybot.registry.Registry;
import net.dean.jraw.references.SubredditReference;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.RestAction;

public class ImageCommandRegistry extends Registry<ImageCommandType> {
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> BIRD = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/birb").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this bird image.")
                .mentionRepliedUser(false).queue();
        }
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> BLEP = (event, cmd) -> {
        final SubredditReference subreddit = RedditUtils.getSubreddit("Blep");
        final RootCommentNode post = RedditUtils.findValidPost(subreddit, "Blep");
        final String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
            : post.getSubject().getUrl();
        event.deferReply().setContent(mediaURL).mentionRepliedUser(false).queue();
    };

    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> BUNNY = (event, cmd) -> {
        try {
            final URLConnection connection = new URL(
                ThreadLocalRandom.current().nextBoolean() ? "https://api.bunnies.io/v2/loop/random/?media=mp4"
                    : "https://api.bunnies.io/v2/loop/random/?media=gif").openConnection();
            final JsonObject result = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = result.get("media").getAsJsonObject().get("poster").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this bunny image.")
                .mentionRepliedUser(false).queue();
        }
    };

    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> CAT_BOMB = (event, cmd) -> {
        event.deferReply().setContent("Loading cats...").queue();
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

        RestAction.allOf(actions).queue(success -> event.getHook().deleteOriginal().queue());
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> CAT = (event, cmd) -> {
        try {
            final BufferedImage image = ImageIO.read(new URL("https://cataas.com/cat"));
            final var stream = new ByteArrayOutputStream();
            ImageIO.write(image, image.getType() == BufferedImage.TYPE_INT_ARGB ? "png" : "jpg", stream);
            event.deferReply()
                .addFile(new ByteArrayInputStream(stream.toByteArray()),
                    "cat." + (image.getType() == BufferedImage.TYPE_INT_ARGB ? "png" : "jpg"))
                .mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this cat image.")
                .mentionRepliedUser(false).queue();
        }
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> DOG_BOMB = (event, cmd) -> {
        event.deferReply().setContent("Loading dogs...").queue();
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
        
        RestAction.allOf(actions).queue(success -> event.getHook().deleteOriginal().queue());
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> DOG = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://dog.ceo/api/breeds/image/random").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("message").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this dog image.")
                .mentionRepliedUser(false).queue();
        }
    };

    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> DOGE = (event, cmd) -> {
        final SubredditReference subreddit = RedditUtils.getSubreddit("Doge");
        final RootCommentNode post = RedditUtils.findValidPost(subreddit, "Doge");
        final String mediaURL = post.getSubject().getUrl().isBlank() ? post.getSubject().getThumbnail()
            : post.getSubject().getUrl();
        event.deferReply().setContent(mediaURL).mentionRepliedUser(false).queue();
    };

    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> DUCK = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://random-d.uk/api/v2/random").openConnection();
            final JsonObject result = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = result.get("url").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been a problem gathering this duck image!")
                .mentionRepliedUser(false).queue();
        }
    };

    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> FOX = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/fox").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this fox image.")
                .mentionRepliedUser(false).queue();
        }
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> KOALA = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/koala").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this koala image.")
                .mentionRepliedUser(false).queue();
        }
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> PANDA = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/panda").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this panda image.")
                .mentionRepliedUser(false).queue();
        }
    };

    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> RACCOON = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/raccoon").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this raccoon image.")
                .mentionRepliedUser(false).queue();
        }
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> RED_PANDA = (event, cmd) -> {
        try {
            final URLConnection connection = new URL("https://some-random-api.ml/img/red_panda").openConnection();
            final JsonObject body = Constants.GSON.fromJson(new InputStreamReader(connection.getInputStream()),
                JsonObject.class);
            final String url = body.get("link").getAsString();
            event.deferReply().setContent(url).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this red panda image.")
                .mentionRepliedUser(false).queue();
        }
    };
    
    private static final BiConsumer<SlashCommandInteractionEvent, ImageCommandType> SNAKE = (event, cmd) -> {
        try {
            final Document page = Jsoup.connect("https://generatorfun.com/random-snake-image").get();
            event.deferReply().setContent(page.select("img").first().attr("abs:src")).mentionRepliedUser(false).queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            event.deferReply(true).setContent("â?Œ There has been an issue gathering this snake image.")
                .mentionRepliedUser(false).queue();
        }
    };

    public ImageCommandRegistry() {
        pexelsAnimal("bee", 3);
        register("bird", new ImageCommandType(BIRD, ANIMAL));
        register("blep", new ImageCommandType(BLEP, ANIMAL));
        register("bunny", new ImageCommandType(BUNNY, ANIMAL));
        register("catbomb", new ImageCommandType(CAT_BOMB, FUN));
        register("cat", new ImageCommandType(CAT, ANIMAL));
        pexelsAnimal("cow", 3);
        pexelsAnimal("crab", 1);
        register("dogbomb", new ImageCommandType(DOG_BOMB, FUN));
        register("dog", new ImageCommandType(DOG, ANIMAL));
        register("doge", new ImageCommandType(DOGE, ANIMAL));
        pexelsAnimal("dolphin", 3);
        register("duck", new ImageCommandType(DUCK, ANIMAL));
        pexelsAnimal("elephant", 3);
        register("foodporn", new PexelsImageCommandType(FUN, 3, "food"));
        pexels("forest", SCENERY, 3);
        register("fox", new ImageCommandType(FOX, ANIMAL));
        pexelsAnimal("giraffe", 3);
        pexelsAnimal("gorilla", 1);
        pexelsAnimal("horse", 3);
        pexelsAnimal("insect", 3);
        pexelsAnimal("jellyfish", 3);
        register("koala", new ImageCommandType(KOALA, ANIMAL));
        pexelsAnimal("lion", 3);
        pexelsAnimal("monkey", 3);
        pexels("nature", SCENERY, 3);
        pexelsAnimal("owl", 3);
        register("panda", new ImageCommandType(PANDA, ANIMAL));
        pexelsAnimal("pig", 2);
        register("raccoon", new ImageCommandType(RACCOON, ANIMAL));
        register("redpdanda", new ImageCommandType(RED_PANDA, ANIMAL));
        pexelsAnimal("sheep", 3);
        register("snake", new ImageCommandType(SNAKE, ANIMAL));
        pexels("space", SCENERY, 3);
        pexelsAnimal("spider", 2);
        pexelsAnimal("squirrel", 3);
        pexelsAnimal("tiger", 3);
        pexelsAnimal("turtle", 3);
        pexelsAnimal("whale", 3);
        pexelsAnimal("wolf", 2);
        pexelsAnimal("zebra", 2);
    }
    
    private ImageCommandType pexels(String name, ImageCategory category, int maxPages) {
        return register(name, new PexelsImageCommandType(category, maxPages));
    }

    private ImageCommandType pexelsAnimal(String name, int maxPages) {
        return pexels(name, ImageCategory.ANIMAL, maxPages);
    }
}
