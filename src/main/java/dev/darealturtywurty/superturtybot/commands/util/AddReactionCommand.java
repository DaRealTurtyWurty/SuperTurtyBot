package dev.darealturtywurty.superturtybot.commands.util;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddReactionCommand extends CoreCommand {
    private static final Pattern MESSAGE_LINK_PATTERN = Pattern.compile("https://discord\\.com/channels/([0-9]+|@me)/([0-9]+)/([0-9]+)");

    public AddReactionCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "message_link", "The link of the message to add a reaction to.", true),
                new OptionData(OptionType.STRING, "emoji_name", "The name of the emoji to add.", true),
                new OptionData(OptionType.STRING, "emoji_id", "The ID of the emoji to add. If not given, searches in this guild.", false)
        );
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Adds a reaction to a message.";
    }

    @Override
    public String getName() {
        return "addreaction";
    }

    @Override
    public String getRichName() {
        return "Add Reaction";
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        String messageLink = event.getOption("message_link", "", OptionMapping::getAsString);
        String @Nullable [] ids = getIds(event, messageLink);
        if (ids == null) return;
        String guildId = ids[0];
        String channelId = ids[1];
        String messageId = ids[2];

        Guild guild = event.getGuild();
        if (!guildId.equals(guild != null ? guild.getId() : "@me")) {
            event.getHook().editOriginal("❌ You must be in the same guild as the message!").queue();
            return;
        }
        MessageChannel messageChannel = getMessageChannel(event, guild, channelId);
        if (messageChannel == null) return;
        Emoji emoji = getEmoji(event, guild);
        if (emoji == null) return;
        event.getHook().editOriginal("Retrieving users who have reacted using this emoji...").queue();
        messageChannel.retrieveReactionUsersById(messageId, emoji).queue(users -> {
            if (!users.isEmpty()) {
                event.getHook().editOriginal("❌ There is already a reaction with that emoji!").queue();
                return;
            }

            messageChannel.addReactionById(messageId, emoji).queue(unused -> {
                event.getHook().editOriginal("✅ Added reaction!\nThe reaction will be removed %s.".formatted(
                        TimeFormat.RELATIVE.format(Instant.now().plusSeconds(15))
                )).queue();
                TurtyBot.EVENT_WAITER.builder(MessageReactionAddEvent.class)
                        .condition(addReactionEvent ->
                                addReactionEvent.getMessageId().equals(messageId) &&
                                        addReactionEvent.getEmoji().equals(emoji) &&
                                        addReactionEvent.getUser() == event.getUser() &&
                                        addReactionEvent.getMember() == event.getMember()
                        )
                        .timeout(15, TimeUnit.SECONDS)
                        .timeoutAction(() -> {
                            event.getHook().editOriginal("❌ Timed out!").queue();
                            messageChannel.removeReactionById(messageId, emoji).queue(
                                    unused1 -> {},
                                    error -> {
                                        if (error instanceof ErrorResponseException errorResponseException &&
                                                errorResponseException.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                                            event.getHook().editOriginal("❌ The message was deleted!").queue();
                                        }
                                    }
                            );
                        })
                        .failure(() -> {
                            messageChannel.removeReactionById(messageId, emoji).queue();
                            event.getHook().editOriginal("❌ Failed to add reaction!").queue();
                        })
                        .success(addReactionEvent -> {
                            messageChannel.removeReactionById(messageId, emoji).queue();
                            event.getHook().editOriginal("✅ Added reaction!").queue();
                        })
                        .build();
            }, error -> {
                event.getHook().editOriginal("❌ Failed to add reaction!").queue();
                if (error instanceof ErrorResponseException errorResponseException &&
                        errorResponseException.getErrorResponse() == ErrorResponse.UNKNOWN_EMOJI) {
                    event.getHook().editOriginal("❌ Invalid emoji!").queue();
                }
            });
        }, error -> {
            event.getHook().editOriginal("❌ Failed to retrieve users who have reacted using this emoji!").queue();
            if (error instanceof ErrorResponseException errorResponseException &&
                    errorResponseException.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                event.getHook().editOriginal("❌ Couldn't find the message!").queue();
            }
        });
    }

    private static @Nullable Emoji getEmoji(SlashCommandInteractionEvent event, Guild guild) {
        String emojiName = event.getOption("emoji_name", "", OptionMapping::getAsString);
        if (emojiName.startsWith(":") && emojiName.endsWith(":")) {
            emojiName = emojiName.substring(1, emojiName.length() - 1);
        }
        String emojiIdStr = event.getOption("emoji_id", OptionMapping::getAsString);
        if (emojiIdStr != null) {
            try {
                long emojiId = Long.parseUnsignedLong(emojiIdStr);
                return Emoji.fromCustom(emojiName, emojiId, false);
            } catch (NumberFormatException e) {
                event.getHook().editOriginal("❌ Invalid emoji ID!").queue();
                return null;
            }
        } else {
            if (guild == null) {
                event.getHook().editOriginal("❌ You must specify an emoji ID outside a guild!").queue();
                return null;
            }
            List<RichCustomEmoji> emojisWithThatName = guild.getEmojisByName(emojiName, true);
            if (emojisWithThatName.isEmpty()) {
                event.getHook().editOriginal("❌ Did not find that emoji in this guild!").queue();
                return null;
            }
            return emojisWithThatName.getFirst();
        }
    }

    private static @Nullable MessageChannel getMessageChannel(SlashCommandInteractionEvent event, Guild guild, String channelId) {
        MessageChannel messageChannel = guild != null ?
                guild.getTextChannelById(channelId) :
                event.getJDA().getPrivateChannelById(channelId);
        if (messageChannel == null) {
            event.getHook().editOriginal("❌ Invalid channel ID!").queue();
            return null;
        }
        if (event.getMember() == null ||
                messageChannel instanceof GuildMessageChannel guildMessageChannel &&
                        !event.getMember().hasPermission(guildMessageChannel, Permission.MESSAGE_ADD_REACTION)) {
            event.getHook().editOriginal("❌ You do not have permission to add reactions in that channel!").queue();
            return null;
        }
        return messageChannel;
    }

    private static String @Nullable [] getIds(SlashCommandInteractionEvent event, String messageLink) {
        String guildId;
        String channelId;
        String messageId;
        Matcher matcher = MESSAGE_LINK_PATTERN.matcher(messageLink);
        if (!matcher.matches()) {
            if (!Pattern.matches("[0-9]+", messageLink)) {
                event.getHook().editOriginal("❌ Invalid message link!").queue();
                return null;
            }
            Guild guild = event.getGuild();
            guildId = guild != null ? guild.getId() : "@me";
            channelId = event.getChannelId();
            if (channelId == null) {
                event.getHook().editOriginal("❌ This command must be run in a channel!").queue();
                return null;
            }
            messageId = messageLink;
        } else {
            guildId = matcher.group(1);
            channelId = matcher.group(2);
            messageId = matcher.group(3);
        }
        return new String[] { guildId, channelId, messageId };
    }
}
