package io.github.darealturtywurty.superturtybot.commands.nsfw;

import io.github.darealturtywurty.superturtybot.core.util.RedditUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RedditNSFWCommand extends NSFWCommand {
    private final String name;
    private final String[] subreddits;
    
    public RedditNSFWCommand(NSFWCategory category, String name, String... subreddits) {
        super(category);
        this.name = name;
        this.subreddits = subreddits;
    }
    
    @Override
    public String getDescription() {
        return "Gets a random " + getName() + " image/video.";
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        // TODO: Check user config to see if they have denied NSFW usage
        // TODO: Check server config to see if the server has disabled this command
        if (event.isFromGuild() && !event.getTextChannel().isNSFW())
            return;
        
        // Essential
        super.runNormalMessage(event);
        
        final EmbedBuilder embed = RedditUtils.constructEmbed(true, this.subreddits);
        if (embed == null) {
            event.getChannel()
                .sendMessage("There has been an error processing the command you tried to run. Please try again!")
                .queue();
            return;
        }
        
        final String mediaURL = embed.build().getTitle();
        if (mediaURL.contains("redgifs") || mediaURL.contains("xvideos") || mediaURL.contains("xhamster")
            || mediaURL.contains("xxx") || mediaURL.contains("porn") || mediaURL.contains("nsfw")
            || mediaURL.contains("gfycat") || mediaURL.contains("/watch.") || mediaURL.contains("reddit.com")
            || mediaURL.contains("twitter") || mediaURL.contains("hub") || mediaURL.contains("imgur")) {
            event.getChannel().sendMessage(mediaURL).queue();
            return;
        }
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
