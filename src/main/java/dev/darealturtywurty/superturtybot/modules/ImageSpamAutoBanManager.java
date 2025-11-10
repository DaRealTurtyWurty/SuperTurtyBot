package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.commands.moderation.BanCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the "image-only spam across channels" autoban logic so AutoModerator stays readable.
 */
public class ImageSpamAutoBanManager {
    private final Map<Long, Deque<ImageSpamMessage>> imageSpamMessages = new ConcurrentHashMap<>();

    public void handleMessage(Message message) {
        if (!message.isFromGuild())
            return;

        final Member member = message.getMember();
        if (member == null)
            return;

        final Guild guild = message.getGuild();
        final GuildData config = GuildData.getOrCreateGuildData(guild);
        if (!config.isImageSpamAutoBanEnabled())
            return;

        final int windowSeconds = config.getImageSpamWindowSeconds();
        final int minImages = config.getImageSpamMinImages();
        final int thresholdHours = config.getImageSpamNewMemberThresholdHours();

        if (!isEligibleForImageSpamCheck(member, thresholdHours) || !isImageOnlySpam(message, minImages))
            return;

        final long now = System.currentTimeMillis();
        final Deque<ImageSpamMessage> recentMessages = this.imageSpamMessages.computeIfAbsent(member.getIdLong(),
                ignored -> new ConcurrentLinkedDeque<>());
        recentMessages.addLast(new ImageSpamMessage(guild.getIdLong(), message.getChannel().getIdLong(), now));
        pruneHistory(recentMessages, now, TimeUnit.SECONDS.toMillis(windowSeconds));

        final Set<Long> channelsWithinWindow = new HashSet<>();
        for (final ImageSpamMessage entry : recentMessages) {
            if (entry.guildId() == guild.getIdLong())
                channelsWithinWindow.add(entry.channelId());
        }

        if (channelsWithinWindow.size() >= 2) {
            attemptAutoBan(guild, member);
            recentMessages.clear();
        }

        if (recentMessages.isEmpty())
            this.imageSpamMessages.remove(member.getIdLong());
    }

    private void pruneHistory(Deque<ImageSpamMessage> history, long now, long windowMillis) {
        ImageSpamMessage head;
        while ((head = history.peekFirst()) != null && now - head.timestamp() > windowMillis) {
            history.pollFirst();
        }
    }

    private boolean isEligibleForImageSpamCheck(Member member, int thresholdHours) {
        final OffsetDateTime joined = member.getTimeJoined();
        return joined.isAfter(OffsetDateTime.now().minusHours(thresholdHours));
    }

    private boolean isImageOnlySpam(Message message, int minImages) {
        if (!message.getContentRaw().isBlank())
            return false;

        final long imageAttachments = message.getAttachments().stream().filter(Message.Attachment::isImage).count();
        return imageAttachments >= minImages;
    }

    private void attemptAutoBan(Guild guild, Member member) {
        final Member selfMember = guild.getSelfMember();
        if (!selfMember.hasPermission(Permission.BAN_MEMBERS) || !selfMember.canInteract(member))
            return;

        final String reason = "Automatic ban: multi-channel image spam detected";
        guild.ban(member, 0, TimeUnit.DAYS).reason(reason).queue(
                success -> logImageSpamBan(guild, member, reason),
                error -> Constants.LOGGER.warn("Failed to auto-ban {} in {} for image spam", member.getIdLong(), guild.getIdLong(), error));
    }

    private void logImageSpamBan(Guild guild, Member member, String reason) {
        final var logging = BanCommand.canLog(guild);
        if (Boolean.TRUE.equals(logging.getKey())) {
            BanCommand.log(logging.getValue(), member.getAsMention()
                    + " was automatically banned for sending repeated multi-image messages across channels. Reason: `" + reason + "`.", false);
        }
    }

    private record ImageSpamMessage(long guildId, long channelId, long timestamp) {
    }
}
