package dev.darealturtywurty.superturtybot.database.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VoiceChannelNotifier {
    private long voiceChannelId;
    private long sendToChannelId;
    private List<Long> mentionRoles;
    private String message;
    private boolean enabled;
    private boolean announcePerJoin;
    private boolean notifyLeaves;
    private long cooldownMs;

    public void sendNotification(Member member, AudioChannelUnion channel, boolean left) {
        sendNotification(member, (AudioChannel) channel, left, _ -> {
        });
    }

    public void sendNotification(Member member, AudioChannel channel, boolean left) {
        sendNotification(member, channel, left, _ -> {
        });
    }

    public void sendNotification(Member member, AudioChannel channel, boolean left, Consumer<Message> afterSend) {
        Guild guild = member.getGuild();
        TextChannel textChannel = guild.getTextChannelById(sendToChannelId);
        if (textChannel == null)
            return;

        String mentionRolesString = mentionRoles.stream()
                .map(roleId -> "<@&" + roleId + ">")
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        String action = left ? "left" : "joined";
        String finalMessage = message.replace("{user}", member.getAsMention())
                .replace("{channel}", channel.getAsMention())
                .replace("{mentions}", mentionRolesString)
                .replace("{action}", action);

        long usersInChannel = guild.getVoiceStates().stream()
                .filter(voiceState -> voiceState.inAudioChannel()
                        && voiceState.getChannel() != null
                        && voiceState.getChannel().getIdLong() == channel.getIdLong())
                .count();

        var embed = new EmbedBuilder()
                .setTitle("Voice Channel Notification")
                .addField("User", member.getEffectiveName(), true)
                .addField("Channel", channel.getAsMention(), true)
                .addField("Action", action.toUpperCase(), true)
                .addField("Users in Channel", usersInChannel + " / " + channel.getUserLimit(), true)
                .setColor(left ? 0xFF0000 : 0x00FF00)
                .setTimestamp(Instant.now())
                .build();
        textChannel.sendMessageEmbeds(embed)
                .setContent(finalMessage)
                .setAllowedMentions(EnumSet.complementOf(EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE)))
                .queue(afterSend, _ -> {
                });
    }
}
