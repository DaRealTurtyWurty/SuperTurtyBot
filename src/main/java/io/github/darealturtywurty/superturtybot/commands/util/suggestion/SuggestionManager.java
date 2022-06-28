package io.github.darealturtywurty.superturtybot.commands.util.suggestion;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

// TODO: Save to database
public final class SuggestionManager extends ListenerAdapter {
    private static final Map<Long, List<Suggestion>> SUGGESTIONS = new HashMap<>();
    public static final SuggestionManager INSTANCE = new SuggestionManager();
    
    private SuggestionManager() {
    }
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromGuild() || event.getUser().isBot() || event.getUser().isSystem())
            return;
        
        final TextChannel suggestionChannel = getSuggestionChannel(event.getGuild());
        if (suggestionChannel == null || event.getChannel().getIdLong() != suggestionChannel.getIdLong())
            return;
        
        final List<Suggestion> suggestions = SUGGESTIONS.get(event.getGuild().getIdLong());
        
        final long messageId = event.getMessageIdLong();
        final Optional<Suggestion> optionalSuggestion = suggestions.stream()
            .filter(suggestion -> suggestion != null && suggestion.messageId() == messageId).findFirst();
        if (!optionalSuggestion.isPresent())
            return;

        final Suggestion suggestion = optionalSuggestion.get();
        event.getChannel().retrieveMessageById(messageId).queue(msg -> {
            final List<MessageReaction> reactions = msg.getReactions();
            
            final Optional<MessageReaction> upVote = reactions.stream()
                .filter(reaction -> "⬆️".equals(reaction.getReactionEmote().getAsReactionCode())).findFirst();
            final Optional<MessageReaction> downVote = reactions.stream()
                .filter(reaction -> "⬇️".equals(reaction.getReactionEmote().getAsReactionCode())).findFirst();
            
            if (!upVote.isPresent()) {
                msg.addReaction("⬆️").queue();
            }

            if (!downVote.isPresent()) {
                msg.addReaction("⬇️").queue();
            }
            
            final MessageReaction upvote = reactions.stream()
                .filter(reaction -> "⬆️".equals(reaction.getReactionEmote().getAsReactionCode())).findFirst().get();
            final MessageReaction downvote = reactions.stream()
                .filter(reaction -> "⬇️".equals(reaction.getReactionEmote().getAsReactionCode())).findFirst().get();
            
            if (compareEmote(event.getReaction(), upvote)) {
                downvote.retrieveUsers().queue(users -> {
                    if (users.stream().anyMatch(user -> user.getIdLong() == event.getUserIdLong())) {
                        event.getReaction().removeReaction(event.getUser()).queue();
                    }
                });

                return;
            }
            
            if (compareEmote(event.getReaction(), downvote)) {
                upvote.retrieveUsers().queue(users -> {
                    if (users.stream().anyMatch(user -> user.getIdLong() == event.getUserIdLong())) {
                        event.getReaction().removeReaction(event.getUser()).queue();
                    }
                });
                
                return;
            }
            
            event.getReaction().removeReaction(event.getUser()).queue();
        }, error -> {
            suggestions.remove(suggestion);
            SUGGESTIONS.put(event.getGuild().getIdLong(), suggestions);
        });
    }
    
    public static CompletableFuture<Suggestion> addSuggestion(TextChannel suggestionChannel, Guild guild,
        Member suggester, String content, @Nullable String mediaUrl) {
        SUGGESTIONS.computeIfAbsent(guild.getIdLong(), id -> new ArrayList<>());
        final List<Suggestion> suggestions = SUGGESTIONS.get(guild.getIdLong());
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.GRAY);
        embed.setTitle("Suggestion #" + suggestions.size());
        embed.setFooter("Suggested by " + suggester.getUser().getName() + "#" + suggester.getUser().getDiscriminator(),
            suggester.getUser().getEffectiveAvatarUrl());
        embed.setDescription(content);
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            embed.setImage(mediaUrl);
            embed.appendDescription("\n\n" + "**Media not showing? [Click Me](" + mediaUrl + ")**");
        }
        
        final var future = new CompletableFuture<Suggestion>();
        suggestionChannel.sendMessageEmbeds(embed.build()).queue(msg -> {
            final var suggestion = new Suggestion(msg.getIdLong(), new ArrayList<>());
            suggestions.add(suggestion);
            future.complete(suggestion);
            msg.addReaction("⬆️").queue(success -> msg.addReaction("⬇️").queue());
        });
        
        return future;
    }
    
    @Nullable
    public static TextChannel getSuggestionChannel(Guild guild) {
        // TODO: Check server config first
        final List<TextChannel> channels = guild.getTextChannelsByName("suggestions", false);
        if (channels.isEmpty())
            return null;
        
        final TextChannel suggestionsChannel = channels.get(0);
        if (!suggestionsChannel.canTalk())
            return null;
        
        return suggestionsChannel;
    }
    
    public static TextChannel getSuggestionChannel(MessageReceivedEvent event) {
        // TODO: Check server config first
        final List<TextChannel> channels = event.getGuild().getTextChannelsByName("suggestions", false);
        if (channels.isEmpty()) {
            event.getMessage().reply("This server does not have suggestions enabled!").mentionRepliedUser(false)
                .queue();
            return null;
        }
        
        final TextChannel suggestionsChannel = channels.get(0);
        if (!suggestionsChannel.canTalk()) {
            event.getMessage()
                .reply("I cannot use the channel " + suggestionsChannel.getAsMention()
                    + "! If there is another suggestions channel, please use the server config to set the channel! "
                    + "If you do not have permission to access the config, please contact a server administrator!")
                .mentionRepliedUser(false).queue();
            return null;
        }
        
        return suggestionsChannel;
    }
    
    public static TextChannel getSuggestionChannel(SlashCommandInteractionEvent event) {
        // TODO: Check server config first
        final List<TextChannel> channels = event.getGuild().getTextChannelsByName("suggestions", false);
        if (channels.isEmpty()) {
            event.deferReply(true).setContent("This server does not have suggestions enabled!")
                .mentionRepliedUser(false).queue();
            return null;
        }
        
        final TextChannel suggestionsChannel = channels.get(0);
        if (!suggestionsChannel.canTalk()) {
            event.deferReply(true)
                .setContent("I cannot use the channel " + suggestionsChannel.getAsMention()
                    + "! If there is another suggestions channel, please use the server config to set the channel! "
                    + "If you do not have permission to access the config, please contact a server administrator!")
                .mentionRepliedUser(false).queue();
            return null;
        }
        
        return suggestionsChannel;
    }
    
    public static CompletableFuture<Suggestion> respondSuggestion(Guild guild, TextChannel suggestionsChannel,
        Member responder, int number, String response, Response.Type type) {
        if (!SUGGESTIONS.containsKey(guild.getIdLong()) || SUGGESTIONS.get(guild.getIdLong()) == null
            || SUGGESTIONS.get(guild.getIdLong()).isEmpty())
            return null;
        
        final List<Suggestion> suggestions = SUGGESTIONS.get(guild.getIdLong());
        if (number > suggestions.size() || number < 0)
            return null;
        
        final long time = System.currentTimeMillis();
        final Suggestion suggestion = suggestions.get(number);
        
        final CompletableFuture<Suggestion> future = new CompletableFuture<>();
        suggestionsChannel.retrieveMessageById(suggestion.messageId()).queue(message -> {
            message.editMessageEmbeds(new EmbedBuilder(message.getEmbeds().get(0)).addField(
                type.richName + " by " + responder.getUser().getName() + "#" + responder.getUser().getDiscriminator(),
                response, false).build()).queue();
            suggestion.responses().add(new Response(type, response, responder.getIdLong(), time));
            future.complete(suggestion);
        }, err -> {
            suggestions.remove(suggestion);
            future.complete(null);
        });
        
        return future;
    }
    
    private static boolean compareEmote(MessageReaction reaction0, MessageReaction reaction1) {
        return reaction0.getReactionEmote().equals(reaction1.getReactionEmote());
    }
}
