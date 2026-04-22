package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.VoiceChannelNotifier;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class VoiceChannelNotifierManager extends ListenerAdapter {
    private static final long NOTIFICATION_SCAN_INTERVAL_SECONDS = 5L;
    public static final VoiceChannelNotifierManager INSTANCE = new VoiceChannelNotifierManager();

    private final Map<Long, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<Long, List<TrackedVoiceChannel>> trackedChannels = new ConcurrentHashMap<>();

    private VoiceChannelNotifierManager() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::processNotifications, 5, NOTIFICATION_SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::cleanupTrackedChannels, 30, 30, TimeUnit.MINUTES);
        ShutdownHooks.register(scheduler::shutdown);
    }

    private void processNotifications() {
        trackedChannels.forEach((guildId, trackedVoiceChannels) -> {
            Guild guild = guilds.get(guildId);
            if (guild == null)
                return;

            GuildData guildData = GuildData.getOrCreateGuildData(guild);
            Map<String, VoiceChannelNotifier> voiceChannelNotifiers = guildData.getVoiceChannelNotifiers();
            if (voiceChannelNotifiers.isEmpty()) {
                trackedChannels.remove(guildId);
                guilds.remove(guildId);
                return;
            }

            trackedVoiceChannels.forEach(trackedVoiceChannel -> {
                VoiceChannelNotifier notifier = voiceChannelNotifiers.get(Long.toString(trackedVoiceChannel.channelId()));
                if (notifier == null || !notifier.isEnabled())
                    return;

                VoiceChannel channel = guild.getVoiceChannelById(trackedVoiceChannel.channelId());
                if (channel == null)
                    return;

                List<TrackedUser> activeUsers = trackedVoiceChannel.trackedUsers().stream()
                        .filter(TrackedUser::isInChannel)
                        .toList();
                if (activeUsers.isEmpty())
                    return;

                if (notifier.isAnnouncePerJoin()) {
                    activeUsers.stream()
                            .filter(trackedUser -> !trackedUser.notified())
                            .filter(trackedUser -> trackedUser.hasBeenInChannelFor(notifier.getCooldownMs()))
                            .forEach(trackedUser -> notifyUser(guild, trackedVoiceChannel, trackedUser, channel, notifier));
                    return;
                }

                if (activeUsers.size() != 1)
                    return;

                TrackedUser trackedUser = activeUsers.getFirst();
                if (!trackedUser.notified() && trackedUser.hasBeenInChannelFor(notifier.getCooldownMs())) {
                    notifyUser(guild, trackedVoiceChannel, trackedUser, channel, notifier);
                }
            });
        });
    }

    private void notifyUser(Guild guild, TrackedVoiceChannel trackedVoiceChannel, TrackedUser trackedUser,
                            VoiceChannel channel, VoiceChannelNotifier notifier) {
        Member member = guild.getMemberById(trackedUser.userId());
        if (member == null)
            return;

        notifier.sendNotification(member, channel, false);
        trackedVoiceChannel.markUserNotified(trackedUser.userId(), trackedUser.joinedAt());
    }

    private void cleanupTrackedChannels() {
        long now = System.currentTimeMillis();
        trackedChannels.forEach((_, trackedVoiceChannels) -> {
            trackedVoiceChannels.forEach(trackedVoiceChannel ->
                    trackedVoiceChannel.trackedUsers()
                            .removeIf(trackedUser ->
                                    !trackedUser.isInChannel()
                                            && now - trackedUser.leftAt() > TimeUnit.MINUTES.toMillis(30)));

            trackedVoiceChannels.removeIf(trackedVoiceChannel ->
                    trackedVoiceChannel.trackedUsers().isEmpty());
        });
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        guilds.put(guild.getIdLong(), guild);

        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        Map<String, VoiceChannelNotifier> voiceChannelNotifiers = guildData.getVoiceChannelNotifiers();
        if (voiceChannelNotifiers.isEmpty()) {
            this.trackedChannels.remove(guild.getIdLong());
            this.guilds.remove(guild.getIdLong());
            return;
        }

        List<TrackedVoiceChannel> trackedVoiceChannels = trackedChannels.computeIfAbsent(guild.getIdLong(),
                _ -> new CopyOnWriteArrayList<>());

        AudioChannelUnion channelJoined = event.getChannelJoined();
        AudioChannelUnion channelLeft = event.getChannelLeft();
        if (channelJoined != null) {
            VoiceChannelNotifier notifier = voiceChannelNotifiers.get(Long.toString(channelJoined.getIdLong()));
            if (notifier != null && notifier.isEnabled()) {
                trackedVoiceChannels.stream()
                        .filter(trackedVoiceChannel -> trackedVoiceChannel.channelId() == channelJoined.getIdLong())
                        .findFirst()
                        .ifPresentOrElse(trackedVoiceChannel -> trackedVoiceChannel.trackUser(event.getMember().getIdLong()),
                                () -> {
                                    var newTrackedVoiceChannel = new TrackedVoiceChannel(channelJoined.getIdLong(), new CopyOnWriteArrayList<>());
                                    newTrackedVoiceChannel.trackUser(event.getMember().getIdLong());
                                    trackedVoiceChannels.add(newTrackedVoiceChannel);
                                });

                if (notifier.getCooldownMs() <= 0L) {
                    notifier.sendNotification(event.getMember(), channelJoined, false);
                    trackedVoiceChannels.stream()
                            .filter(trackedVoiceChannel -> trackedVoiceChannel.channelId() == channelJoined.getIdLong())
                            .findFirst()
                            .ifPresent(trackedVoiceChannel -> trackedVoiceChannel.markUserNotified(event.getMember().getIdLong(),
                                    trackedVoiceChannel.latestJoinTimeFor(event.getMember().getIdLong())));
                }
            }
        }

        if (channelLeft != null) {
            VoiceChannelNotifier notifier = voiceChannelNotifiers.get(Long.toString(channelLeft.getIdLong()));
            if (notifier != null && notifier.isEnabled()) {
                trackedVoiceChannels.stream()
                        .filter(trackedVoiceChannel -> trackedVoiceChannel.channelId() == channelLeft.getIdLong())
                        .findFirst()
                        .ifPresent(trackedVoiceChannel -> trackedVoiceChannel.untrackUser(event.getMember().getIdLong()));

                if (notifier.isNotifyLeaves()) {
                    notifier.sendNotification(event.getMember(), channelLeft, true);
                }
            }
        }
    }

    public void saveGuildNotifiers(GuildData guildData) {
        Document notifierDocument = new Document();
        guildData.getVoiceChannelNotifiers().forEach((channelId, notifier) ->
                notifierDocument.append(channelId, new Document()
                        .append("voiceChannelId", notifier.getVoiceChannelId())
                        .append("sendToChannelId", notifier.getSendToChannelId())
                        .append("mentionRoles", notifier.getMentionRoles())
                        .append("message", notifier.getMessage())
                        .append("enabled", notifier.isEnabled())
                        .append("announcePerJoin", notifier.isAnnouncePerJoin())
                        .append("notifyLeaves", notifier.isNotifyLeaves())
                        .append("cooldownMs", notifier.getCooldownMs())));

        Database.getDatabase().guildData.updateOne(
                Filters.eq("guild", guildData.getGuild()),
                new Document("$set", new Document("voiceChannelNotifiers", notifierDocument)),
                new UpdateOptions().upsert(true)
        );
    }

    private record TrackedVoiceChannel(long channelId, List<TrackedUser> trackedUsers) {
        public void trackUser(long userId) {
            trackedUsers.add(new TrackedUser(userId, System.currentTimeMillis(), null, false));
        }

        public void untrackUser(long userId) {
            trackedUsers.stream()
                    .filter(trackedUser -> trackedUser.userId() == userId && trackedUser.isInChannel())
                    .findFirst()
                    .ifPresent(trackedUser -> trackedUsers.set(trackedUsers.indexOf(trackedUser),
                            new TrackedUser(trackedUser.userId(), trackedUser.joinedAt(), System.currentTimeMillis(),
                                    trackedUser.notified())));
        }

        public void markUserNotified(long userId, long joinedAt) {
            trackedUsers.stream()
                    .filter(trackedUser -> trackedUser.userId() == userId && trackedUser.joinedAt() == joinedAt && trackedUser.isInChannel())
                    .findFirst()
                    .ifPresent(trackedUser -> trackedUsers.set(trackedUsers.indexOf(trackedUser),
                            new TrackedUser(trackedUser.userId(), trackedUser.joinedAt(), trackedUser.leftAt(), true)));
        }

        public long latestJoinTimeFor(long userId) {
            return trackedUsers.stream()
                    .filter(trackedUser -> trackedUser.userId() == userId)
                    .mapToLong(TrackedUser::joinedAt)
                    .max()
                    .orElse(-1L);
        }
    }

    private record TrackedUser(long userId, long joinedAt, Long leftAt, boolean notified) {
        public boolean isInChannel() {
            return leftAt == null;
        }

        public boolean hasBeenInChannelFor(long duration) {
            return isInChannel()
                    ? System.currentTimeMillis() - joinedAt >= duration
                    : leftAt - joinedAt >= duration;
        }
    }
}
