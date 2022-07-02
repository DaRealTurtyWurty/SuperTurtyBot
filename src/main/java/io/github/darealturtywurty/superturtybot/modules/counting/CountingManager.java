package io.github.darealturtywurty.superturtybot.modules.counting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.Counting;
import io.github.darealturtywurty.superturtybot.database.pojos.Counting.UserData;
import io.github.darealturtywurty.superturtybot.modules.counting.maths.MathHandler;
import io.github.darealturtywurty.superturtybot.modules.counting.maths.MathOperation;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CountingManager extends ListenerAdapter {
    public static final CountingManager INSTANCE = new CountingManager();
    private final Map<Long, List<Long>> countingChannels = new HashMap<>();
    
    public boolean isCountingChannel(Guild guild, TextChannel channel) {
        if (!this.countingChannels.containsKey(guild.getIdLong())
            || !this.countingChannels.get(guild.getIdLong()).contains(channel.getIdLong()))
            return false;
        
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("channel", channel.getIdLong()));
        final Counting profile = Database.getDatabase().counting.find(filter).first();
        if (profile == null) {
            this.countingChannels.get(guild.getIdLong()).remove(channel.getIdLong());
            return false;
        }
        
        return true;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (shouldIgnore(event))
            return;

        final TextChannel channel = event.getTextChannel();

        if (!this.countingChannels.containsKey(event.getGuild().getIdLong())
            || !this.countingChannels.get(event.getGuild().getIdLong()).contains(channel.getIdLong()))
            return;

        final Bson filter = Filters.and(Filters.eq("guild", event.getGuild().getIdLong()),
            Filters.eq("channel", channel.getIdLong()));
        final Counting profile = Database.getDatabase().counting.find(filter).first();
        if (profile == null) {
            this.countingChannels.get(event.getGuild().getIdLong()).remove(channel.getIdLong());
            return;
        }

        final CountingMode mode = CountingMode.valueOf(profile.getCountingMode());

        final List<Bson> updates = new ArrayList<>();

        final Message message = event.getMessage();
        final User user = message.getAuthor();
        final String content = message.getContentRaw();
        final float given = CountingMode.parse(mode, content);

        // If the provided value is not valid
        if (Float.isNaN(given))
            return;

        // If the user is the latest counter
        if (profile.getLatestCounter() == user.getIdLong()) {
            final List<UserData> users = profile.getUsers();
            final Optional<UserData> optData = users.stream().filter(data -> data.getUser() == user.getIdLong())
                .findFirst();

            UserData data;
            // Check that the user has their data in the database
            if (optData.isPresent()) {
                data = optData.get();

                // If the user's current count succession is already greater or equal to 3 then fail
                if (data.getCurrentCountSuccession() >= 100) {
                    final float starting = CountingMode.getStartingNumber(mode);
                    final float next = CountingMode.getNextNumber(mode, starting);
                    failChannel(profile, mode, message, filter, updates,
                        "❌ You cannot count more than 3 times in a row! The next number is: "
                            + CountingMode.parse(mode, starting, next),
                        starting, next);
                    return;
                }
            } else {
                // Add their data since its not present but will now need to be
                data = new UserData(user.getIdLong());
                profile.getUsers().add(data);
            }

            final int currentCountSuccession = data.getCurrentCountSuccession();
            users.forEach(u -> u.setCurrentCountSuccession(0));
            data.setCurrentCountSuccession(currentCountSuccession + 1);
            data.setTotalCounts(data.getTotalCounts() + 1);
        } else {
            profile.setLatestCounter(user.getIdLong());
            updates.add(Updates.set("latestCounter", profile.getLatestCounter()));
            
            final List<UserData> users = profile.getUsers();
            final Optional<UserData> optData = users.stream().filter(data -> data.getUser() == user.getIdLong())
                .findFirst();
            UserData data;
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
        }
        
        final float currentNext = profile.getNextNumber();
        if (currentNext == given) {
            message.addReaction("✅").queue(success -> {
                profile.setCurrentNumber(currentNext);
                updates.add(Updates.set("currentNumber", profile.getCurrentNumber()));
                
                profile.setCurrentCount(profile.getCurrentCount() + 1);
                updates.add(Updates.set("currentCount", profile.getCurrentCount()));

                if (mode == CountingMode.MATHS) {
                    final Pair<MathOperation, Float> maths = MathHandler.getNextNumber(currentNext);
                    profile.setNextNumber(maths.getRight());
                    updates.add(Updates.set("nextNumber", profile.getNextNumber()));
                    
                    if (mode.shouldNotify()) {
                        channel.sendMessage(profile.getCurrentCount() + ". The next number is: "
                            + MathHandler.parse(maths.getLeft(), currentNext, profile.getNextNumber())).queue();
                    }
                } else {
                    profile.setNextNumber(CountingMode.getNextNumber(mode, currentNext));
                    updates.add(Updates.set("nextNumber", profile.getNextNumber()));
                    
                    if (mode.shouldNotify()) {
                        channel.sendMessage(profile.getCurrentCount() + ". The next number is: "
                            + CountingMode.parse(mode, currentNext, profile.getNextNumber())).queue();
                    }
                }
                
                updates.add(Updates.set("users", profile.getUsers()));
                Database.getDatabase().counting.updateOne(filter, updates);
            });

            return;
        }
        
        final float starting = CountingMode.getStartingNumber(mode);
        if (mode == CountingMode.MATHS) {
            final Pair<MathOperation, Float> next = MathHandler.getNextNumber(starting);
            String should = String.valueOf(currentNext);
            if (should.endsWith(".0")) {
                should = should.substring(0, should.length() - 2);
            }

            failChannel(profile, mode, message, filter, updates,
                "❌ `" + content + "` is not the correct number! It should have been `" + should
                    + "`. The next number is: **" + MathHandler.parse(next.getLeft(), starting, next.getRight())
                    + "**.",
                starting, next.getRight());
            return;
        }

        final float next = CountingMode.getNextNumber(mode, starting);
        String should = String.valueOf(currentNext);
        if (should.endsWith(".0")) {
            should = should.substring(0, should.length() - 2);
        }
        
        failChannel(profile, mode, message, filter, updates,
            "❌ `" + content + "` is not the correct number! It should have been `" + should
                + "`. The next number is: **" + CountingMode.parse(mode, starting, next) + "**.",
            starting, next);
    }
    
    public boolean removeCountingChannel(Guild guild, TextChannel channel) {
        if (!isCountingChannel(guild, channel))
            return false;
        
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("channel", channel.getIdLong()));
        return Database.getDatabase().counting.deleteOne(filter).getDeletedCount() > 0;
    }
    
    public boolean setCountingChannel(Guild guild, TextChannel channel, CountingMode mode) {
        if (isCountingChannel(guild, channel))
            return false;

        if (this.countingChannels.containsKey(guild.getIdLong())) {
            this.countingChannels.get(guild.getIdLong()).add(channel.getIdLong());
        } else {
            this.countingChannels.put(guild.getIdLong(), new ArrayList<>(List.of(channel.getIdLong())));
        }

        return Database.getDatabase().counting.insertOne(new Counting(guild.getIdLong(), channel.getIdLong(), mode))
            .getInsertedId() != null;
    }
    
    private void failChannel(final Counting profile, final CountingMode mode, final Message message, final Bson filter,
        final List<Bson> updates, String response, float startAt, float nextNumber) {
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

        message.addReaction("💀").queue(success -> message.reply(response).mentionRepliedUser(false).queue());
    }
    
    private boolean shouldIgnore(MessageReceivedEvent event) {
        return !event.isFromGuild() || event.isFromThread() || event.isWebhookMessage() || event.getAuthor().isBot()
            || event.getAuthor().isSystem() || event.getChannelType() != ChannelType.TEXT;
    }
}
