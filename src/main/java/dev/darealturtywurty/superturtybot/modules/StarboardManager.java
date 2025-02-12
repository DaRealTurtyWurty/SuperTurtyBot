package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Showcase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.conversions.Bson;

import java.awt.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                || event.getUser() == null || event.getUser().isBot() || event.getUser().isSystem())
            return;

        final GuildData config = GuildData.getOrCreateGuildData(event.getGuild());

        if (!config.getStarEmoji().equals(event.getReaction().getEmoji().getFormatted()))
            return;

        final TextChannel channel = event.getChannel().asTextChannel();

        final List<Long> channels = GuildData.getLongs(config.getShowcaseChannels());
        if (!channels.contains(channel.getIdLong()))
            return;

        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                Filters.eq("channel", channel.getIdLong()), Filters.eq("message", event.getMessageIdLong()));
        final Showcase showcase = Database.getDatabase().starboard.find(filter).first();
        if (showcase == null)
            return;

        event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue(m -> {
            final List<Bson> updates = new ArrayList<>();
            final MessageReaction reaction = m.getReactions()
                    .stream()
                    .filter(r -> config.getStarEmoji().equals(r.getEmoji().getFormatted()))
                    .findFirst()
                    .orElse(null);
            if (reaction == null)
                return;

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
                        starboard.retrieveMessageById(starboardMessageId).queue(
                                message -> message.editMessage(config.getStarEmoji() + " **" + showcase.getStars()
                                        + "** from <#" + showcase.getChannel() + ">").mentionRepliedUser(false).queue(),
                                error -> {
                                    final CompletableFuture<Long> messageId = sendStarboard(event.getJDA(), starboard,
                                            filter, showcase);
                                    messageId.thenAccept(id -> {
                                        if (id != -1L) {
                                            showcase.setStarboardMessage(id);
                                            updates.add(Updates.set("starboardMessage", showcase.getStarboardMessage()));
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

        final GuildData config = GuildData.getOrCreateGuildData(event.getGuild());
        final List<Long> channels = GuildData.getLongs(config.getShowcaseChannels());
        if (!channels.contains(channel.getIdLong()))
            return;

        final Message message = event.getMessage();

        if (config.isStarboardMediaOnly() && message.getAttachments().isEmpty()
                && !Constants.URL_VALIDATOR.isValid(message.getContentDisplay()) && message.getEmbeds().isEmpty())
            return;

        message.addReaction(Emoji.fromFormatted(config.getStarEmoji())).queue();

        Database.getDatabase().starboard.insertOne(new Showcase(event.getGuild().getIdLong(), channel.getIdLong(),
                message.getIdLong(), event.getAuthor().getIdLong()));
    }

    private static CompletableFuture<TextChannel> getStarboard(Guild guild) {
        final GuildData config = GuildData.getOrCreateGuildData(guild);
        if (!config.isStarboardEnabled())
            return CompletableFuture.completedFuture(null);

        final TextChannel starboard = guild.getTextChannelById(config.getStarboard());
        if (starboard != null)
            return CompletableFuture.completedFuture(starboard);

        final var channel = new CompletableFuture<TextChannel>();
        final List<TextChannel> found = guild.getTextChannelsByName("starboard", true);
        if (found.isEmpty()) {
            channel.complete(null);
        } else {
            channel.complete(found.getFirst());
        }

        return channel;
    }

    private static CompletableFuture<Long> sendStarboard(JDA jda, TextChannel channel, Bson filter, Showcase showcase) {
        final var future = new CompletableFuture<Long>();
        final TextChannel original = jda.getTextChannelById(showcase.getChannel());
        if (original == null) {
            future.complete(-1L);
            return future;
        }

        original.retrieveMessageById(showcase.getMessage()).queue(msg -> {
            final User author = msg.getAuthor();
            final Member member = original.getGuild().getMember(author);

            final ZonedDateTime dateTime = msg.getTimeCreated().toZonedDateTime();
            final String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            channel.sendMessage("⭐ **" + showcase.getStars() + "** from <#" + showcase.getChannel() + ">")
                    .setEmbeds(new EmbedBuilder()
                            .setAuthor(author.getEffectiveName(),
                                    member == null ? author.getEffectiveAvatarUrl() : member.getEffectiveAvatarUrl())
                            .setColor(member == null ? Color.CYAN : member.getColor() == null ? Color.CYAN : member.getColor())
                            .setDescription(msg.getContentRaw()).setFooter(showcase.getMessage() + " • " + formattedTime)
                            .addField("Source", "[Jump]" + "(" + msg.getJumpUrl() + ")", false)
                            .setImage(msg.getEmbeds().isEmpty()
                                    ? msg.getAttachments().isEmpty() ? null : msg.getAttachments().getFirst().getUrl()
                                    : msg.getEmbeds().getFirst().getImage() == null ? null : msg.getEmbeds().getFirst().getImage().getUrl())
                            .build())
                    .queue(ms -> future.complete(ms.getIdLong()));
        }, error -> {
            Database.getDatabase().starboard.deleteOne(filter);
            future.complete(-1L);
        });
        return future;
    }
}
