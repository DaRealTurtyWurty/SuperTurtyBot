package io.github.darealturtywurty.superturtybot.modules;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.core.util.Constants;
import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Showcase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class StarboardManager extends ListenerAdapter {
    public static final StarboardManager INSTANCE = new StarboardManager();
    private static final EnumSet<Permission> ALLOWED_PERMS = EnumSet.of(Permission.VIEW_CHANNEL);
    private static final EnumSet<Permission> DISALLOWED_PERMS = EnumSet.of(Permission.MESSAGE_SEND,
        Permission.MESSAGE_ADD_REACTION, Permission.CREATE_PUBLIC_THREADS, Permission.CREATE_PRIVATE_THREADS,
        Permission.MESSAGE_SEND_IN_THREADS);
    
    private StarboardManager() {
    }
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromGuild() || event.isFromThread() || !event.isFromType(ChannelType.TEXT)
            || event.getUser().isBot() || event.getUser().isSystem())
            return;
        
        // TODO: Check server config for starring emoji/emote
        if (!"⭐".equals(event.getReaction().getEmoji().getName()))
            return;

        final TextChannel channel = event.getChannel().asTextChannel();

        // TODO: Check server config first
        if (!"showcases".equalsIgnoreCase(channel.getName()))
            return;

        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("channel", channel.getIdLong()), Filters.eq("message", event.getMessageIdLong()));
        final Showcase showcase = Database.getDatabase().starboard.find(filter).first();
        if (showcase == null)
            return;

        event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue(m -> {
            final List<Bson> updates = new ArrayList<>();
            final MessageReaction reaction = m.getReactions().stream().filter(r -> "⭐".equals(r.getEmoji().getName()))
                .findFirst().get();
            showcase.setStars(reaction.getCount());
            updates.add(Updates.set("stars", showcase.getStars()));
            
            final long starboardMessageId = showcase.getStarboardMessage();
            if (showcase.getStars() >= 5) {
                getStarboard(event.getGuild()).thenAccept(starboard -> {
                    if (starboardMessageId == 0) {
                        final CompletableFuture<Long> messageId = sendStarboard(event.getJDA(), starboard, filter,
                            showcase);
                        messageId.thenAccept(id -> {
                            if (id != -1L) {
                                showcase.setStarboardMessage(id);
                                updates.add(Updates.set("starboardMessage", showcase.getStarboardMessage()));
                                Database.getDatabase().starboard.updateOne(filter, updates);
                            } else {
                                updates.clear();
                            }
                        });
                    } else {
                        starboard.retrieveMessageById(starboardMessageId)
                            .queue(message -> message
                                .editMessage("⭐ **" + showcase.getStars() + "** from <#" + showcase.getChannel() + ">")
                                .mentionRepliedUser(false).queue(), error -> {
                                    final CompletableFuture<Long> messageId = sendStarboard(event.getJDA(), starboard,
                                        filter, showcase);
                                    messageId.thenAccept(id -> {
                                        if (id != -1L) {
                                            showcase.setStarboardMessage(id);
                                            updates
                                                .add(Updates.set("starboardMessage", showcase.getStarboardMessage()));
                                            Database.getDatabase().starboard.updateOne(filter, updates);
                                        } else {
                                            updates.clear();
                                        }
                                    });
                                });
                    }
                });
                
                return;
            }
            
            Database.getDatabase().starboard.updateOne(filter, updates);
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.isFromThread() || event.isWebhookMessage()
            || !event.isFromType(ChannelType.TEXT) || event.getAuthor().isBot() || event.getAuthor().isSystem())
            return;
        
        final TextChannel channel = event.getChannel().asTextChannel();

        // TODO: Check server config first
        if (!"showcases".equalsIgnoreCase(channel.getName()))
            return;

        final Message message = event.getMessage();
        
        // TODO: Check server config to see if its a media only starboard
        if (message.getAttachments().isEmpty() && !Constants.URL_VALIDATOR.isValid(message.getContentDisplay())
            && message.getEmbeds().isEmpty())
            return;
        
        // TODO: Get emoji from server config
        message.addReaction(Emoji.fromUnicode("⭐")).queue();
        
        System.out.println(message.getIdLong());
        Database.getDatabase().starboard.insertOne(new Showcase(event.getGuild().getIdLong(), channel.getIdLong(),
            message.getIdLong(), event.getAuthor().getIdLong()));
    }
    
    // TODO: Check the server config as the first thing
    private static CompletableFuture<TextChannel> getStarboard(Guild guild) {
        final var channel = new CompletableFuture<TextChannel>();
        final List<TextChannel> found = guild.getTextChannelsByName("starboard", true);
        if (found.isEmpty()) {
            guild.createTextChannel("starboard")
                .addPermissionOverride(guild.getPublicRole(), ALLOWED_PERMS, DISALLOWED_PERMS).queue(channel::complete);
        } else {
            channel.complete(found.get(0));
        }

        return channel;
    }

    private static CompletableFuture<Long> sendStarboard(JDA jda, TextChannel channel, Bson filter, Showcase showcase) {
        final var future = new CompletableFuture<Long>();
        final TextChannel original = jda.getTextChannelById(showcase.getChannel());
        original.retrieveMessageById(showcase.getMessage()).queue(msg -> {
            final User author = msg.getAuthor();
            final Member member = original.getGuild().getMember(author);

            final ZonedDateTime dateTime = msg.getTimeCreated().toZonedDateTime();
            final String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            channel.sendMessage("⭐ **" + showcase.getStars() + "** from <#" + showcase.getChannel() + ">")
                .setEmbeds(new EmbedBuilder()
                    .setAuthor(author.getName(),
                        member == null ? author.getEffectiveAvatarUrl() : member.getEffectiveAvatarUrl())
                    .setColor(member == null ? Color.CYAN : member.getColor() == null ? Color.CYAN : member.getColor())
                    .setDescription(msg.getContentRaw()).setFooter(showcase.getMessage() + " • " + formattedTime)
                    .addField("Source", "[Jump]" + "(" + msg.getJumpUrl() + ")", false)
                    .setImage(msg.getEmbeds().isEmpty()
                        ? msg.getAttachments().isEmpty() ? null : msg.getAttachments().get(0).getUrl()
                        : msg.getEmbeds().get(0).getImage().getUrl())
                    .build())
                .queue(ms -> future.complete(ms.getIdLong()));
        }, error -> {
            Database.getDatabase().starboard.deleteOne(filter);
            future.complete(-1L);
        });
        return future;
    }
}
