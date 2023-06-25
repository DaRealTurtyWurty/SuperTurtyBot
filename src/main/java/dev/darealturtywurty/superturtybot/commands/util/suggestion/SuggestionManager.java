package dev.darealturtywurty.superturtybot.commands.util.suggestion;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.Permission;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import dev.darealturtywurty.superturtybot.commands.core.config.GuildConfigCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.SuggestionResponse;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class SuggestionManager extends ListenerAdapter {
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
        
        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("message", event.getMessageIdLong()));
        final Suggestion suggestion = Database.getDatabase().suggestions.find(filter).first();
        if (suggestion == null)
            return;
        
        event.getChannel().retrieveMessageById(suggestion.getMessage()).queue(msg -> {
            msg.addReaction(Emoji.fromUnicode("⬆️")).queue();
            msg.addReaction(Emoji.fromUnicode("⬇️")).queue();
        }, error -> Database.getDatabase().suggestions.deleteOne(filter));
    }
    
    public static CompletableFuture<Suggestion> addSuggestion(TextChannel suggestionChannel, Guild guild,
        Member suggester, String content, @Nullable String mediaUrl) {
        final var counter = new AtomicInteger();
        Database.getDatabase().suggestions.find(Filters.eq("guild", guild.getIdLong()))
            .forEach(suggestion -> counter.getAndIncrement());
        
        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.GRAY);
        embed.setTitle("Suggestion #" + counter.get());
        embed.setFooter("Suggested by " + suggester.getUser().getName(), suggester.getUser().getEffectiveAvatarUrl());
        embed.setDescription(content);
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            embed.setImage(mediaUrl);
            embed.appendDescription("\n\n" + "**Media not showing? [Click Me](" + mediaUrl + ")**");
        }
        
        final var future = new CompletableFuture<Suggestion>();
        suggestionChannel.sendMessageEmbeds(embed.build()).queue(msg -> {
            final var suggestion = new Suggestion(guild.getIdLong(), suggester.getIdLong(), msg.getIdLong(),
                System.currentTimeMillis());
            Database.getDatabase().suggestions.insertOne(suggestion);
            future.complete(suggestion);
            msg.addReaction(Emoji.fromUnicode("⬆️")).queue(success -> msg.addReaction(Emoji.fromUnicode("⬇️")).queue());
        });
        
        return future;
    }

    @NotNull
    public static CompletableFuture<Suggestion> deleteSuggestion(Guild guild, TextChannel suggestionChannel, Member member, int suggestionNumber) {
        final var future = new CompletableFuture<Suggestion>();
        List<Suggestion> suggestions = new ArrayList<>();
        Database.getDatabase().suggestions.find(Filters.eq("guild", guild.getIdLong()))
            .forEach(suggestions::add);
        suggestions = suggestions.stream().sorted(Comparator.comparingLong(Suggestion::getCreatedAt)).collect(Collectors.toList());
        if (suggestionNumber > suggestions.size()) {
            future.complete(null);
            return future;
        }

        if(suggestionNumber < 0) {
            future.complete(null);
            return future;
        }

        final Suggestion suggestion = suggestions.get(suggestionNumber);
        if (suggestion.getUser() != member.getIdLong() && !member.hasPermission(Permission.MANAGE_SERVER)) {
            future.complete(null);
            return future;
        }

        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("message", suggestion.getMessage()));
        Database.getDatabase().suggestions.deleteOne(filter);
        suggestionChannel.retrieveMessageById(suggestion.getMessage()).queue(msg -> {
            msg.delete().queue();
            future.complete(suggestion);
        });

        return future;
    }
    
    public static @Nullable TextChannel getSuggestionChannel(Guild guild) {
        final Bson serverConfigFilter = GuildConfigCommand.getFilter(guild);
        final GuildConfig config = GuildConfigCommand.get(serverConfigFilter, guild);

        TextChannel channel = guild.getTextChannelById(config.getSuggestions());
        if (channel == null) {
            final List<TextChannel> possibleChannels = guild.getTextChannelsByName("suggestions", false);
            if (possibleChannels.isEmpty())
                return null;

            channel = possibleChannels.get(0);
        }
        
        if (!channel.canTalk())
            return null;
        
        return channel;
    }
    
    public static @Nullable TextChannel getSuggestionChannel(MessageReceivedEvent event) {
        final TextChannel channel = getSuggestionChannel(event.getGuild());
        if (channel == null) {
            event.getMessage().reply("This server does not have suggestions enabled!").mentionRepliedUser(false)
                .queue();
            return null;
        }
        
        return channel;
    }
    
    public static @Nullable TextChannel getSuggestionChannel(SlashCommandInteractionEvent event) {
        final TextChannel channel = getSuggestionChannel(event.getGuild());
        if (channel == null) {
            event.deferReply(true).setContent("This server does not have suggestions enabled!")
                .mentionRepliedUser(false).mentionRepliedUser(false).queue();
            return null;
        }
        
        return channel;
    }
    
    public static CompletableFuture<Suggestion> respondSuggestion(Guild guild, TextChannel suggestionsChannel,
        Member responder, int number, String response, SuggestionResponse.Type type) {
        if (number < 0)
            return null;

        List<Suggestion> suggestions = new ArrayList<>();
        Database.getDatabase().suggestions.find(Filters.eq("guild", guild.getIdLong())).forEach(suggestions::add);
        
        if (number > suggestions.size())
            return null;
        
        suggestions = suggestions.stream().sorted(Comparator.comparing(Suggestion::getCreatedAt))
            .collect(Collectors.toList());

        if (number > suggestions.size())
            return null;
        
        final long time = System.currentTimeMillis();
        final Suggestion suggestion = suggestions.get(number);
        if (suggestion.getUser() != responder.getIdLong() && !responder.hasPermission(Permission.MANAGE_SERVER))
            return null;
        
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("message", suggestion.getMessage()));
        
        final var future = new CompletableFuture<Suggestion>();
        suggestionsChannel.retrieveMessageById(suggestion.getMessage()).queue(message -> {
            message.editMessageEmbeds(new EmbedBuilder(message.getEmbeds().get(0)).addField(
                type.richName + " by " + responder.getUser().getName(),  response, false).build()).queue();
            suggestion.getResponses().add(new SuggestionResponse(type, response, responder.getIdLong(), time));
            Database.getDatabase().suggestions.updateOne(filter, Updates.set("responses", suggestion.getResponses()));
            future.complete(suggestion);
        }, err -> Database.getDatabase().suggestions.deleteOne(filter));
        
        return future;
    }
}
