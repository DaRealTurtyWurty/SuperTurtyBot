package dev.darealturtywurty.superturtybot.modules.counting;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Counting;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Counting.UserData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildConfig;
import dev.darealturtywurty.superturtybot.modules.counting.maths.MathHandler;
import dev.darealturtywurty.superturtybot.modules.counting.maths.MathOperation;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class CountingManager extends ListenerAdapter {
    public static final CountingManager INSTANCE = new CountingManager();

    public boolean isCountingChannel(Guild guild, TextChannel channel) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
                Filters.eq("channel", channel.getIdLong()));
        final Counting profile = Database.getDatabase().counting.find(filter).first();
        return profile != null;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (shouldIgnore(event)) return;

        final TextChannel channel = event.getChannel().asTextChannel();

        Guild guild = event.getGuild();
        GuildConfig guildConfig = Database.getDatabase().guildConfig.find(Filters.eq("guild", guild.getIdLong()))
                .first();
        if (guildConfig == null) return;

        int maxSuccession = guildConfig.getMaxCountingSuccession();

        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
                Filters.eq("channel", channel.getIdLong()));
        final Counting profile = Database.getDatabase().counting.find(filter).first();
        if (profile == null) return;

        final CountingMode mode = CountingMode.valueOf(profile.getCountingMode());

        final List<Bson> updates = new ArrayList<>();

        final Message message = event.getMessage();
        final User user = message.getAuthor();
        final String content = message.getContentRaw();
        final float given = CountingMode.parse(mode, content);

        // If the provided value is not valid
        if (Float.isNaN(given)) return;

        UserData data;
        // If the user is the latest counter
        if (profile.getLatestCounter() == user.getIdLong()) {
            final List<UserData> users = profile.getUsers();
            final Optional<UserData> optData = users.stream().filter(d -> d.getUser() == user.getIdLong()).findFirst();

            // Check that the user has their data in the database
            if (optData.isPresent()) {
                data = optData.get();

                // If the user's current count succession is already greater or equal to maxSuccession then fail
                if (data.getCurrentCountSuccession() >= maxSuccession) {
                    final float starting = CountingMode.getStartingNumber(mode);
                    final float next = CountingMode.getNextNumber(mode, starting);
                    failChannel(profile, mode, message, filter, updates,
                            "❌ You cannot count more than %s times in a row! The next number is: %s".formatted(
                                    maxSuccession, CountingMode.parse(mode, starting, next)), starting, next);
                    return;
                }
            } else {
                // Add their data since it's not present but will now need to be
                data = new UserData(user.getIdLong());
                profile.getUsers().add(data);
            }

            final int currentCountSuccession = data.getCurrentCountSuccession();
            users.forEach(u -> u.setCurrentCountSuccession(0));
            data.setCurrentCountSuccession(currentCountSuccession + 1);
            data.setTotalCounts(data.getTotalCounts() + 1);

            channel.getPermissionOverrides().stream().filter(permissionOverride -> {
                EnumSet<Permission> denied = permissionOverride.getDenied();
                return denied.contains(Permission.MESSAGE_SEND) && denied.contains(Permission.CREATE_INSTANT_INVITE);
            }).forEach(permissionOverride -> permissionOverride.delete().queue());
        } else {
            profile.setLatestCounter(user.getIdLong());
            updates.add(Updates.set("latestCounter", profile.getLatestCounter()));

            final List<UserData> users = profile.getUsers();
            final Optional<UserData> optData = users.stream().filter(d -> d.getUser() == user.getIdLong()).findFirst();
            if (optData.isPresent()) {
                data = optData.get();
            } else {
                data = new UserData(user.getIdLong());
                profile.getUsers().add(data);
            }

            final int currentCountSuccession = data.getCurrentCountSuccession();
            users.forEach(u -> u.setCurrentCountSuccession(0));
            data.setCurrentCountSuccession(currentCountSuccession + 1);
            data.setTotalCounts(data.getTotalCounts() + 1);

            channel.getPermissionOverrides().stream().filter(permissionOverride -> {
                EnumSet<Permission> denied = permissionOverride.getDenied();
                return denied.contains(Permission.MESSAGE_SEND) && denied.contains(Permission.CREATE_INSTANT_INVITE);
            }).forEach(permissionOverride -> permissionOverride.delete().queue());
        }

        final float currentNext = profile.getNextNumber();
        if (MathHandler.to1DecimalPlace(currentNext) == MathHandler.to1DecimalPlace(given)) {
            message.addReaction(Emoji.fromUnicode(data.getCurrentCountSuccession() < maxSuccession ? "✅" : "🚫"))
                    .queue();

            profile.setCurrentNumber(currentNext);
            updates.add(Updates.set("currentNumber", profile.getCurrentNumber()));

            profile.setCurrentCount(profile.getCurrentCount() + 1);
            updates.add(Updates.set("currentCount", profile.getCurrentCount()));

            if (mode == CountingMode.MATHS) {
                final Pair<MathOperation, Float> maths = MathHandler.getNextNumber(currentNext);
                profile.setNextNumber(maths.getRight());
                updates.add(Updates.set("nextNumber", profile.getNextNumber()));

                if (mode.shouldNotify()) {
                    channel.sendMessage(
                                    profile.getCurrentCount() + ". The next number is: " + MathHandler.parse(maths.getLeft(),
                                            currentNext, profile.getNextNumber()))
                            .queue(msg -> profile.setLastCountingMessageMillis(
                                    msg.getTimeCreated().toInstant().toEpochMilli()));
                }
            } else {
                profile.setNextNumber(CountingMode.getNextNumber(mode, currentNext));
                updates.add(Updates.set("nextNumber", profile.getNextNumber()));

                if (mode.shouldNotify()) {
                    channel.sendMessage(
                            profile.getCurrentCount() + ". The next number is: " + CountingMode.parse(mode, currentNext,
                                    profile.getNextNumber())).queue(msg -> profile.setLastCountingMessageMillis(
                            msg.getTimeCreated().toInstant().toEpochMilli()));
                }
            }

            updates.add(Updates.set("users", profile.getUsers()));
            Database.getDatabase().counting.updateOne(filter, updates);

            if (data.getCurrentCountSuccession() == maxSuccession) {
                channel.upsertPermissionOverride(message.getMember())
                        .setDenied(Permission.MESSAGE_SEND, Permission.CREATE_INSTANT_INVITE).queue();
            }

            return;
        }

        final float starting = CountingMode.getStartingNumber(mode);
        if (mode == CountingMode.MATHS) {
            final Pair<MathOperation, Float> next = MathHandler.getNextNumber(starting);
            String should = String.valueOf(currentNext);
            if (should.endsWith(".0")) {
                should = should.substring(0, should.length() - 2);
            }

            should = String.format("%.1f", Float.parseFloat(should));

            failChannel(profile, mode, message, filter, updates,
                    "❌ `" + content + "` is not the correct number! It should have been `" + should + "`. The next number is: **" + MathHandler.parse(
                            next.getLeft(), starting, next.getRight()) + "**.", starting, next.getRight());
            return;
        }

        final float next = CountingMode.getNextNumber(mode, starting);
        String should = CountingMode.parse(mode, starting, currentNext);

        failChannel(profile, mode, message, filter, updates,
                "❌ `" + content + "` is not the correct number! It should have been `" + should + "`. The next number is: **" + CountingMode.parse(
                        mode, starting, next) + "**.", starting, next);
    }

    public boolean removeCountingChannel(Guild guild, TextChannel channel) {
        if (!isCountingChannel(guild, channel)) return false;

        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
                Filters.eq("channel", channel.getIdLong()));
        return Database.getDatabase().counting.deleteOne(filter).getDeletedCount() > 0;
    }

    public boolean setCountingChannel(Guild guild, TextChannel channel, CountingMode mode) {
        if (isCountingChannel(guild, channel)) return false;

        var profile = new Counting(guild.getIdLong(), channel.getIdLong(), mode);
        profile.setCurrentNumber(CountingMode.getStartingNumber(mode));

        String parsed;
        if (mode == CountingMode.MATHS) {
            final Pair<MathOperation, Float> next = MathHandler.getNextNumber(profile.getCurrentNumber());
            profile.setNextNumber(next.getRight());
            parsed = MathHandler.parse(next.getLeft(), profile.getCurrentNumber(), profile.getNextNumber());
        } else {
            profile.setNextNumber(CountingMode.getNextNumber(mode, profile.getCurrentNumber()));
            parsed = CountingMode.parse(mode, profile.getCurrentNumber(), profile.getNextNumber());
        }

        channel.sendMessage("The next number is: " + parsed)
                .queue(msg -> profile.setLastCountingMessageMillis(msg.getTimeCreated().toInstant().toEpochMilli()));

        return Database.getDatabase().counting.insertOne(profile).getInsertedId() != null;
    }

    private void failChannel(final Counting profile, final CountingMode mode, final Message message, final Bson filter, final List<Bson> updates, String response, float startAt, float nextNumber) {
        if (message.getTimeCreated().toInstant().toEpochMilli() < profile.getLastCountingMessageMillis()) return;

        final List<UserData> users = profile.getUsers();
        users.forEach(u -> u.setCurrentCountSuccession(0));
        updates.add(Updates.set("users", users));

        profile.setCurrentCount(0);
        updates.add(Updates.set("currentCount", profile.getCurrentCount()));

        profile.setLatestCounter(0L);
        updates.add(Updates.set("latestCounter", profile.getLatestCounter()));

        profile.setCurrentNumber(startAt);
        updates.add(Updates.set("currentNumber", profile.getCurrentNumber()));

        profile.setNextNumber(nextNumber);
        updates.add(Updates.set("nextNumber", profile.getNextNumber()));

        Database.getDatabase().counting.updateOne(filter, updates);

        message.addReaction(Emoji.fromUnicode("💀")).queue(success -> message.reply(response).mentionRepliedUser(false)
                .queue(msg -> profile.setLastCountingMessageMillis(msg.getTimeCreated().toInstant().toEpochMilli())));
    }

    private boolean shouldIgnore(MessageReceivedEvent event) {
        return !event.isFromGuild() || event.isFromThread() || event.isWebhookMessage() || event.getAuthor()
                .isBot() || event.getAuthor().isSystem() || event.getChannelType() != ChannelType.TEXT;
    }
}
