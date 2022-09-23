package io.github.darealturtywurty.superturtybot.commands.util.suggestion;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.darealturtywurty.superturtybot.Environment;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SuggestCommand extends CoreCommand {
    public SuggestCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "suggestion", "The thing that you want to suggest", true),
            new OptionData(OptionType.STRING, "media_url", "A media URL that you would like to add to your suggestion",
                false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Suggests something for this server into the suggestion channel";
    }
    
    @Override
    public String getHowToUse() {
        return "/suggest [suggestion]\n/suggest [suggestion] [mediaURL]";
    }
    
    @Override
    public String getName() {
        return "suggest";
    }
    
    @Override
    public String getRichName() {
        return "Suggest";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent(
                "This command can only be used inside of a server! If you meant to suggest a bot feature/improvement, you can use `{$}botsuggest`!"
                    .replace("{$}", Environment.INSTANCE.defaultPrefix()))
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final TextChannel suggestionChannel = SuggestionManager.getSuggestionChannel(event);
        if (suggestionChannel == null)
            return;
        
        final String suggestionStr = event.getOption("suggestion").getAsString();
        if (suggestionStr.isBlank()) {
            event.deferReply(true).setContent("You must provide something to suggest!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        event.deferReply(false).mentionRepliedUser(false).queue();
        final String mediaURL = event.getOption("media_url", null, OptionMapping::getAsString);
        final CompletableFuture<Suggestion> suggestion = SuggestionManager.addSuggestion(suggestionChannel,
            event.getGuild(), event.getMember(), suggestionStr, mediaURL);
        suggestion.thenAccept(sug -> {
            final var embed = new EmbedBuilder();
            embed.setTimestamp(Instant.now());
            embed.setColor(sug != null ? Color.GREEN : Color.RED);
            embed.setTitle(
                sug != null ? "✅ Suggestion successfully added!" : "❌ There was an issue adding this suggestion!",
                sug != null
                    ? "https://discord.com/channels/" + event.getGuild().getIdLong() + "/"
                        + suggestionChannel.getIdLong() + "/" + sug.getMessage()
                    : "");
            embed.setFooter("Created by: " + event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                event.getMember().getEffectiveAvatarUrl());
            
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        });
    }
}
