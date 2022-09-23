package io.github.darealturtywurty.superturtybot.core.util;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyDuration;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyInstant;
import org.graalvm.polyglot.proxy.ProxyIterable;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyTime;
import org.graalvm.polyglot.proxy.ProxyTimeZone;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.MessageFlag;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageActivity.ActivityType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.AuthorInfo;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.MessageEmbed.Footer;
import net.dv8tion.jda.api.entities.MessageEmbed.ImageInfo;
import net.dv8tion.jda.api.entities.MessageEmbed.Provider;
import net.dv8tion.jda.api.entities.MessageEmbed.Thumbnail;
import net.dv8tion.jda.api.entities.MessageEmbed.VideoInfo;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Role.RoleTags;
import net.dv8tion.jda.api.entities.RoleIcon;
import net.dv8tion.jda.api.entities.StageInstance.PrivacyLevel;
import net.dv8tion.jda.api.entities.ThreadMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel.AutoArchiveDuration;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.sticker.RichSticker;
import net.dv8tion.jda.api.entities.sticker.Sticker.StickerFormat;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

//@formatter:off
public final class ProxyFactory {
    private ProxyFactory() {
    }

    public static ProxyObject actionComponent(ActionComponent actionComponent) {
        return fromMap(actionComponent,
            Map.of(
                "id", actionComponent.getId(),
                "maxPerRow", actionComponent.getMaxPerRow(),
                "type", toProxy(actionComponent.getType()),
                "isDisabled", function(args -> actionComponent.isDisabled())
            )
        );
    }

    public static ProxyObject actionRow(ActionRow actionRow) {
        return fromMap(actionRow,
            Map.of(
                "actionComponents", toProxy(actionRow.getActionComponents()),
                "buttons", toProxy(actionRow.getButtons()),
                "components", toProxy(actionRow.getComponents()),
                "type", toProxy(actionRow.getType()),
                "iterator", toProxy(actionRow.iterator()),
                "isDisabled", function(args -> actionRow.isDisabled()),
                "isEmpty", function(args -> actionRow.isEmpty()),
                "isEnabled", function(args -> actionRow.isEnabled()),
                "isValid", function(args -> actionRow.isValid())
            )
        );
    }

    public static ProxyArray array(Collection<Object> objects) {
        return ProxyArray.fromList(objects.stream().map(ProxyFactory::toProxy).toList());
    }

    public static ProxyArray array(Object[] objects) {
        return ProxyArray.fromList(Stream.of(objects).map(ProxyFactory::toProxy).toList());
    }

    public static Map<String, Object> audioChannelDetails(AudioChannel channel) {
        final Map<String, Object> entries = guildChannelDetails(channel);
        entries.put("bitrate", channel.getBitrate());
        entries.put("region", toProxy(channel.getRegion()));
        return entries;
    }

    public static ProxyObject authorInfo(AuthorInfo authorInfo) {
        return fromMap(authorInfo,
            Map.of(
                "iconURL", authorInfo.getIconUrl(),
                "name", authorInfo.getName(),
                "proxyIconURL", authorInfo.getProxyIconUrl(),
                "url", authorInfo.getUrl()
            )
        );
    }

    public static ProxyObject autoArchiveDuration(AutoArchiveDuration archiveDuration) {
        return fromMap(archiveDuration,
            Map.of(
                "name", archiveDuration.name(),
                "ordinal", archiveDuration.ordinal(),
                "minutes", archiveDuration.getMinutes()
            )
        );
    }

    public static ProxyObject button(Button button) {
        return fromMap(button,
            Map.of(
                "emoji", toProxy(button.getEmoji()),
                "style", toProxy(button.getStyle()),
                "type", toProxy(button.getType()),
                "id", button.getId(),
                "label", button.getLabel(),
                "url", button.getUrl(),
                "maxPerRow", button.getMaxPerRow(),
                "isDisabled", function(args -> button.isDisabled())
            )
        );
    }

    public static ProxyObject buttonStyle(ButtonStyle buttonStyle) {
        return fromMap(buttonStyle,
            Map.of(
                "name", buttonStyle.name(),
                "ordinal", buttonStyle.ordinal(),
                "rawKey", toProxy(buttonStyle.getKey())
            )
        );
    }

    public static ProxyObject category(Category category) {
        final Map<String, Object> entries = guildChannelDetails(category);
        entries.put("channels", toProxy(category.getChannels()));
        entries.put("members", toProxy(category.getMembers()));
        entries.put("position", category.getPosition());
        entries.put("positionRaw", category.getPositionRaw());
        return fromMap(category, entries);
    }

    public static Map<String, Object> channelDetails(Channel channel) {
        return Map.of(
            "mention", channel.getAsMention(),
            "id", channel.getIdLong(),
            "name", channel.getName(),
            "timeCreated", toProxy(channel.getTimeCreated()),
            "type", toProxy(channel.getType())
        );
    }

    public static ProxyObject channelType(ChannelType type) {
        return fromMap(type,
            Map.of(
                "name", type.name(),
                "ordinal", type.ordinal(),
                "sortBucket", type.getSortBucket(),
                "isAudio", function(args -> type.isAudio()),
                "isGuild", function(args -> type.isGuild()),
                "isMessage", function(args -> type.isMessage()),
                "isThread", function(args -> type.isThread()),
                "discordId", type.getId()
            )
        );
    }

    public static ProxyObject color(Color color) {
        return fromMap(color,
            Map.of(
                "red", color.getRed(),
                "green", color.getGreen(),
                "blue", color.getBlue(),
                "alpha", color.getAlpha(),
                "rgb", color.getRGB(),
                "brighter", function(args -> color.brighter()),
                "darker", function(args -> color.darker())
            )
        );
    }

    public static ProxyObject commandType(Command.Type commandType) {
        return fromMap(commandType,
            Map.of(
                "id", commandType.getId(),
                "name", commandType.name(),
                "ordinal", commandType.ordinal()
            )
        );
    }

    public static ProxyObject componentType(Component.Type componentType) {
        return fromMap(componentType,
            Map.of(
                "name", componentType.name(),
                "ordinal", componentType.ordinal(),
                "maxPerRow", componentType.getMaxPerRow()
            )
        );
    }

    public static ProxyObject embedType(EmbedType embedType) {
        return fromMap(embedType,
            Map.of(
                "name", embedType.name(),
                "ordinal", embedType.ordinal()
            )
        );
    }

    public static ProxyObject emoji(CustomEmoji emoji) {
        return fromMap(emoji,
            Map.of(
                "timeCreated", toProxy(emoji.getTimeCreated()),
                "mention", emoji.getAsMention(),
                "id", emoji.getIdLong(),
                "name", emoji.getName(),
                "isAnimated", function(args -> emoji.isAnimated())
            )
        );
    }

    public static ProxyObject field(Field field) {
        return fromMap(field,
            Map.of(
                "name", field.getName(),
                "value", field.getValue(),
                "isInline", function(args -> field.isInline())
            )
        );
    }

    public static ProxyObject footer(Footer footer) {
        return fromMap(footer,
            Map.of(
                "iconURL", footer.getIconUrl(),
                "text", footer.getText(),
                "proxyIconURL", footer.getProxyIconUrl()
            )
        );
    }

    public static ProxyExecutable function(Function<List<Value>, Object> function) {
        return args -> function.apply(Arrays.asList(args));
    }

    public static ProxyObject guild(Guild guild) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("categories", toProxy(guild.getCategories()));
        entries.put("channels", toProxy(guild.getChannels()));
        entries.put("boosters", toProxy(guild.getBoosters()));
        entries.put("description", guild.getDescription());
        entries.put("emotes", toProxy(guild.getEmojis()));
        entries.put("icon", guild.getIconUrl());
        entries.put("locale", toProxy(guild.getLocale()));
        entries.put("members", toProxy(guild.getMembers()));
        entries.put("name", guild.getName());
        entries.put("owner", toProxy(guild.getOwner()));
        entries.put("ownerId", guild.getOwnerIdLong());
        entries.put("roles", toProxy(guild.getRoles()));
        entries.put("bot", toProxy(guild.getSelfMember()));
        entries.put("timeCreated", toProxy(guild.getTimeCreated()));
        entries.put("hasBoostProgressBar", function(args -> guild.isBoostProgressBarEnabled()));

        return fromMap(guild, entries);
    }

    public static Map<String, Object> guildChannelDetails(GuildChannel channel) {
        final Map<String, Object> entries = channelDetails(channel);
        entries.put("guild", toProxy(channel.getGuild()));
        return entries;
    }

    public static Map<String, Object> guildMessageChannelDetails(GuildMessageChannel channel) {
        final Map<String, Object> entries = new HashMap<>(messageChannelDetails(channel));
        entries.put("canTalk", function(args -> channel.canTalk()));
        return entries;
    }

    public static ProxyObject imageInfo(ImageInfo imageInfo) {
        return fromMap(imageInfo,
            Map.of(
                "height", imageInfo.getHeight(),
                "width", imageInfo.getWidth(),
                "proxyURL", imageInfo.getProxyUrl(),
                "url", imageInfo.getUrl()
            )
        );
    }

    public static ProxyInstant instant(Instant instant) {
        return ProxyInstant.from(instant);
    }

    public static ProxyInstant instant(TemporalAccessor accessor) {
        return instant(Instant.from(accessor));
    }

    public static ProxyObject interaction(Interaction interaction) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("id", interaction.getIdLong());
        entries.put("channel", toProxy(interaction.getChannel()));
        entries.put("channelType", toProxy(interaction.getChannelType()));
        entries.put("guildLocale", toProxy(interaction.getGuildLocale()));
        entries.put("member", toProxy(interaction.getMember()));
        entries.put("token", interaction.getToken());
        entries.put("rawPosition", interaction.getTypeRaw());
        entries.put("type", toProxy(interaction.getType()));
        entries.put("user", toProxy(interaction.getUser()));
        entries.put("userLocale", toProxy(interaction.getUserLocale()));
        entries.put("isAcknowledged", function(args -> interaction.isAcknowledged()));
        entries.put("isFromGuild", function(args -> interaction.isFromGuild()));

        return fromMap(interaction, entries);
    }

    public static ProxyObject interactionHook(InteractionHook interactionHook) {
        return fromMap(interactionHook,
            Map.of(
                "interaction", toProxy(interactionHook.getInteraction()),
                "expirationTimestamp", interactionHook.getExpirationTimestamp(),
                "isExpired", function(args -> interactionHook.isExpired())
            )
        );
    }

    public static ProxyObject interactionType(InteractionType interactionType) {
        return fromMap(interactionType,
            Map.of(
                "key", interactionType.getKey(),
                "name", interactionType.name(),
                "ordinal", interactionType.ordinal()
            )
        );
    }

    public static ProxyObject itemComponent(ItemComponent itemComponent) {
        return fromMap(itemComponent,
            Map.of(
                "maxPerRow", itemComponent.getMaxPerRow(),
                "type", toProxy(itemComponent.getType())
            )
        );
    }

    public static ProxyObject locale(Locale locale) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("country", locale.getCountry());
        entries.put("displayCountry", locale.getDisplayCountry());
        entries.put("displayLanguage", locale.getDisplayLanguage());
        entries.put("displayName", locale.getDisplayName());
        entries.put("displayScript", locale.getDisplayScript());
        entries.put("displayVariant", locale.getDisplayVariant());
        entries.put("extensionKeys", toProxy(locale.getExtensionKeys()));
        entries.put("iso3Country", locale.getISO3Country());
        entries.put("iso3Language", locale.getISO3Language());
        entries.put("language", locale.getLanguage());
        entries.put("script", locale.getScript());
        entries.put("unicodeLocaleAttributes", toProxy(locale.getUnicodeLocaleAttributes()));
        entries.put("unicodeLocaleKeys", toProxy(locale.getUnicodeLocaleKeys()));
        entries.put("variant", locale.getVariant());
        entries.put("hasExtensions", function(args -> locale.hasExtensions()));
        entries.put("stripExtensions", function(args -> toProxy(locale.stripExtensions())));
        entries.put("languageTag", locale.toLanguageTag());
        return fromMap(locale, entries);
    }

    public static ProxyHashMap map(Map<Object, Object> map) {
        final Map<Object, Object> newMap = new HashMap<>();
        for (final Entry entry : map.entrySet()) {
            newMap.put(toProxy(entry.getKey()), toProxy(entry.getValue()));
        }

        return ProxyHashMap.from(map);
    }

    public static ProxyObject member(Member member) {
        return fromMap(member,
            Map.of(
                "id", member.getIdLong(),
                "effectiveName", member.getEffectiveName(),
                "user", toProxy(member.getUser()),
                "color", toProxy(member.getColor()),
                "avatarURL", member.getEffectiveAvatarUrl(),
                "guild", toProxy(member.getGuild()),
                "onlineStatus", toProxy(member.getOnlineStatus()),
                "permissions", toProxy(member.getPermissions()),
                "role", toProxy(member.getRoles())
            )
        );
    }

    public static ProxyObject message(Message message) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("id", message.getIdLong());
        entries.put("actionRows", toProxy(message.getActionRows()));
        entries.put("activity", toProxy(message.getActivity()));
        entries.put("attachments", toProxy(message.getAttachments()));
        entries.put("author", toProxy(message.getAuthor()));
        entries.put("buttons", toProxy(message.getButtons()));
        entries.put("category", toProxy(message.getCategory()));
        entries.put("channel", toProxy(message.getChannel()));
        entries.put("channelType", toProxy(message.getChannelType()));
        entries.put("displayContent", message.getContentDisplay());
        entries.put("rawContent", message.getContentRaw());
        entries.put("strippedContent", message.getContentStripped());
        entries.put("embeds", toProxy(message.getEmbeds()));
        entries.put("flags", toProxy(message.getFlags()));
        entries.put("rawFlags", message.getFlagsRaw());
        entries.put("guild", toProxy(message.getGuild()));
        entries.put("interaction", toProxy(message.getInteraction()));
        entries.put("invites", toProxy(message.getInvites()));
        entries.put("jumpURL", message.getJumpUrl());
        entries.put("member", toProxy(message.getMember()));
        entries.put("mentionedChannels", toProxy(message.getMentions().getChannels()));
        entries.put("mentionedMembers", toProxy(message.getMentions().getMembers()));
        entries.put("mentionedRoles", toProxy(message.getMentions().getRoles()));
        entries.put("mentionedUsers", toProxy(message.getMentions().getUsers()));
        entries.put("messageReference", toProxy(message.getMessageReference()));
        entries.put("nonce", message.getNonce());
        entries.put("reactions", toProxy(message.getReactions()));
        entries.put("referencedMessage", toProxy(message.getReferencedMessage()));
        entries.put("stickers", toProxy(message.getStickers()));
        entries.put("timeCreated", toProxy(message.getTimeCreated()));
        entries.put("timeEdited", toProxy(message.getTimeEdited()));
        entries.put("messageType", toProxy(message.getType()));
        entries.put("isEdited", function(args -> message.isEdited()));
        entries.put("isEphemeral", function(args -> message.isEphemeral()));
        entries.put("isFromGuild", function(args -> message.isFromGuild()));
        entries.put("isPinned", function(args -> message.isPinned()));
        entries.put("isSuppressedEmbeds", function(args -> message.isSuppressedEmbeds()));
        entries.put("isTTS", function(args -> message.isTTS()));
        entries.put("isWebhookMessage", function(args -> message.isWebhookMessage()));
        entries.put("doesMentionEveryone", function(args -> message.getMentions().mentionsEveryone()));
        return fromMap(message, entries);
    }

    public static ProxyObject messageActivity(MessageActivity messageActivity) {
        return fromMap(messageActivity,
            Map.of(
                "partyId", messageActivity.getPartyId(),
                "type", toProxy(messageActivity.getType()),
                "application", toProxy(messageActivity.getApplication())
            )
        );
    }

    public static ProxyObject messageActivityApplication(MessageActivity.Application messageActivityApplication) {
        return fromMap(messageActivityApplication,
            Map.of(
                "coverId", messageActivityApplication.getCoverId(),
                "timeCreated", toProxy(messageActivityApplication.getTimeCreated()),
                "coverURL", messageActivityApplication.getCoverUrl(),
                "description", messageActivityApplication.getDescription(),
                "iconId", messageActivityApplication.getIconId(),
                "iconURL", messageActivityApplication.getIconUrl(),
                "id", messageActivityApplication.getIdLong(),
                "name", messageActivityApplication.getName()
            )
        );
    }

    public static ProxyObject messageActivityType(ActivityType messageActivityType) {
        return fromMap(messageActivityType,
            Map.of(
                "name", messageActivityType.name(),
                "ordinal", messageActivityType.ordinal(),
                "id", messageActivityType.getId()
            )
        );
    }

    public static Map<String, Object> messageChannelDetails(MessageChannel channel) {
        final Map<String, Object> entries = new HashMap<>(channelDetails(channel));
        entries.put("latestMessageId", channel.getLatestMessageIdLong());
        return entries;
    }

    public static ProxyObject messageEmbed(MessageEmbed messageEmbed) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("author", toProxy(messageEmbed.getAuthor()));
        entries.put("color", toProxy(messageEmbed.getColor()));
        entries.put("fields", toProxy(messageEmbed.getFields()));
        entries.put("description", messageEmbed.getDescription());
        entries.put("footer", toProxy(messageEmbed.getFooter()));
        entries.put("length", messageEmbed.getLength());
        entries.put("imageInfo", toProxy(messageEmbed.getImage()));
        entries.put("siteProvider", toProxy(messageEmbed.getSiteProvider()));
        entries.put("title", messageEmbed.getTitle());
        entries.put("thumbnail", toProxy(messageEmbed.getThumbnail()));
        entries.put("url", messageEmbed.getUrl());
        entries.put("timestamp", toProxy(messageEmbed.getTimestamp()));
        entries.put("type", toProxy(messageEmbed.getType()));
        entries.put("videoInfo", toProxy(messageEmbed.getVideoInfo()));
        entries.put("isEmpty", function(args -> messageEmbed.isEmpty()));
        entries.put("isSendable", function(args -> messageEmbed.isSendable()));

        return fromMap(messageEmbed, entries);
    }

    public static ProxyObject messageFlag(MessageFlag messageFlag) {
        return fromMap(messageFlag,
            Map.of(
                "name", messageFlag.name(),
                "ordinal", messageFlag.ordinal(),
                "value", messageFlag.getValue()
            )
        );
    }

    public static ProxyObject messageInteraction(Message.Interaction messageInteraction) {
        return fromMap(messageInteraction,
            Map.of(
                "id", messageInteraction.getIdLong(),
                "member", toProxy(messageInteraction.getMember()),
                "user", toProxy(messageInteraction.getUser()),
                "timeCreated", toProxy(messageInteraction.getTimeCreated()),
                "type", toProxy(messageInteraction.getType()),
                "rawType", messageInteraction.getTypeRaw(),
                "name", messageInteraction.getName()
            )
        );
    }

    public static ProxyObject messageReaction(MessageReaction messageReaction) {
        return fromMap(messageReaction,
            Map.of(
                "messageId", messageReaction.getMessageIdLong(),
                "channel", toProxy(messageReaction.getChannel()),
                "channelType", toProxy(messageReaction.getChannelType()),
                "guild", toProxy(messageReaction.getGuild()),
                "reactionEmote", toProxy(messageReaction.getEmoji()),
                "hasCount", function(args -> messageReaction.hasCount()),
                "isSelf", function(args -> messageReaction.isSelf())
            )
        );
    }

    public static ProxyObject messageReceivedEvent(MessageReceivedEvent event) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("author", toProxy(event.getAuthor()));
        entries.put("channel", toProxy(event.getChannel()));
        entries.put("channelType", toProxy(event.getChannelType()));
        entries.put("guild", toProxy(event.getGuild()));
        entries.put("member", toProxy(event.getMember()));
        entries.put("message", toProxy(event.getMessage()));
        entries.put("messageId", event.getMessageIdLong());
        entries.put("responseNumber", event.getResponseNumber());
        entries.put("isFromGuild", function(args -> event.isFromGuild()));
        entries.put("isWebhookMessage", function(args -> event.isWebhookMessage()));
        entries.put("isFromThread", function(args -> event.isFromThread()));

        return fromMap(event, entries);
    }

    public static ProxyObject messageReference(MessageReference messageReference) {
        return fromMap(messageReference,
            Map.of(
                "channel", toProxy(messageReference.getChannel()),
                "channelId", messageReference.getChannelIdLong(),
                "guild", toProxy(messageReference.getGuild()),
                "guildId", messageReference.getGuildIdLong(),
                "messageId", messageReference.getMessageIdLong(),
                "message", toProxy(messageReference.getMessage()),
                "channel", toProxy(messageReference.getChannel()),
                "channelId", toProxy(messageReference.getChannelIdLong())
            )
        );
    }

    public static ProxyObject messageSticker(RichSticker messageSticker) {
        return fromMap(messageSticker,
            Map.of(
                "id", messageSticker.getIdLong(),
                "formatType", toProxy(messageSticker.getFormatType()),
                "tags", toProxy(messageSticker.getTags()),
                "timeCreated", toProxy(messageSticker.getTimeCreated()),
                "description", messageSticker.getDescription(),
                "iconURL", messageSticker.getIconUrl(),
                "name", messageSticker.getName()
            )
        );
    }

    public static ProxyObject messageType(MessageType messageType) {
        return fromMap(messageType,
            Map.of(
                "name", messageType.name(),
                "ordinal", messageType.ordinal(),
                "id", messageType.getId(),
                "isSystem", function(args -> messageType.isSystem())
            )
        );
    }

    public static ProxyObject onlineStatus(OnlineStatus status) {
        return fromMap(status,
            Map.of(
                "name", status.name(),
                "ordinal", status.ordinal(),
                "apiKey", status.getKey()
            )
        );
    }

    public static ProxyObject optionMapping(OptionMapping optionMapping) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("asAttachment", function(args -> toProxy(optionMapping.getAsAttachment())));
        entries.put("asAudioChannel", function(args -> toProxy(optionMapping.getAsChannel().asAudioChannel())));
        entries.put("asBoolean", function(args -> optionMapping.getAsBoolean()));
        entries.put("asDouble", function(args -> optionMapping.getAsDouble()));
        entries.put("asInteger", function(args -> optionMapping.getAsInt()));
        entries.put("asLong", function(args -> optionMapping.getAsLong()));
        entries.put("asGuildChannel", function(args -> toProxy(optionMapping.getAsChannel().asGuildMessageChannel())));
        entries.put("asMember", function(args -> toProxy(optionMapping.getAsMember())));
        entries.put("asMentionable", function(args -> toProxy(optionMapping.getAsMentionable())));
        entries.put("asMessageChannel", function(args -> toProxy(optionMapping.getAsChannel().asStandardGuildMessageChannel())));
        entries.put("asNewsChannel", function(args -> toProxy(optionMapping.getAsChannel().asNewsChannel())));
        entries.put("asRole", function(args -> toProxy(optionMapping.getAsRole())));
        entries.put("asStageChannel", function(args -> toProxy(optionMapping.getAsChannel().asStageChannel())));
        entries.put("asString", function(args -> optionMapping.getAsString()));
        entries.put("asTextChannel", function(args -> toProxy(optionMapping.getAsChannel().asTextChannel())));
        entries.put("asThreadChannel", function(args -> toProxy(optionMapping.getAsChannel().asThreadChannel())));
        entries.put("asUser", function(args -> toProxy(optionMapping.getAsUser())));
        entries.put("asVoiceChannel", function(args -> toProxy(optionMapping.getAsChannel().asVoiceChannel())));
        entries.put("channelType", toProxy(optionMapping.getChannelType()));
        entries.put("mentionedChannels", toProxy(optionMapping.getMentions()));
        entries.put("mentionedMembers", toProxy(optionMapping.getMentions()));
        entries.put("mentionedRoles", toProxy(optionMapping.getMentions().getRoles()));
        entries.put("mentionedUsers", toProxy(optionMapping.getMentions().getUsers()));
        entries.put("mentions", toProxy(optionMapping.getMentions()));
        entries.put("name", optionMapping.getName());
        entries.put("type", toProxy(optionMapping.getType()));

        return fromMap(optionMapping, entries);
    }

    public static ProxyObject optionType(OptionType optionType) {
        return fromMap(optionType,
            Map.of(
                "name", optionType.name(),
                "ordinal", optionType.ordinal(),
                "rawKey", optionType.getKey(),
                "canSupportChoices", function(args -> optionType.canSupportChoices())
            )
        );
    }

    public static ProxyObject permission(Permission permission) {
        return fromMap(permission,
            Map.of(
                "name", permission.name(),
                "ordinal", permission.ordinal(),
                "displayName", permission.getName(),
                "offset", permission.getOffset(),
                "rawValue", permission.getRawValue(),
                "isChannel", function(args -> permission.isChannel()),
                "isGuild", function(args -> permission.isGuild()),
                "isText", function(args -> permission.isText()),
                "isVoice", function(args -> permission.isVoice())
            )
        );
    }

    public static ProxyObject privacyLevel(PrivacyLevel level) {
        return fromMap(level,
            Map.of(
                "name", level.name(),
                "ordinal", level.ordinal(),
                "apiValue", level.getKey()
            )
        );
    }

    public static ProxyObject privateChannel(PrivateChannel channel) {
        final Map<String, Object> entries = messageChannelDetails(channel);
        entries.put("user", toProxy(channel.getUser()));
        entries.put("name", channel.getName());
        return fromMap(channel, entries);
    }

    public static ProxyObject provider(Provider provider) {
        return fromMap(provider,
            Map.of(
                "url", provider.getUrl(),
                "name", provider.getName()
            )
        );
    }

    public static ProxyObject region(Region region) {
        return fromMap(region,
            Map.of(
                "emoji", region.getEmoji(),
                "internalName", region.getKey(),
                "humanName", region.getName(),
                "name", region.name(),
                "ordinal", region.ordinal(),
                "isVIP", function(args -> region.isVip())
            )
        );
    }

    public static ProxyObject role(Role role) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("mention", role.getAsMention());
        entries.put("color", toProxy(role.getColor()));
        entries.put("guild", toProxy(role.getGuild()));
        entries.put("roleIcon", toProxy(role.getIcon()));
        entries.put("id", role.getIdLong());
        entries.put("name", role.getName());
        entries.put("permissions", toProxy(role.getPermissions()));
        entries.put("position", role.getPosition());
        entries.put("rawPosition", role.getPositionRaw());
        entries.put("tags", toProxy(role.getTags()));
        entries.put("timeCreated", toProxy(role.getTimeCreated()));
        entries.put("isHoisted", function(args -> role.isHoisted()));
        entries.put("isManaged", function(args -> role.isManaged()));
        entries.put("isMentionable", function(args -> role.isMentionable()));
        entries.put("isPublic", function(args -> role.isPublicRole()));
        return fromMap(role, entries);
    }

    public static ProxyObject roleIcon(RoleIcon roleIcon) {
        return fromMap(roleIcon,
            Map.of(
                "emoji", roleIcon.getEmoji(),
                "iconId", roleIcon.getIconId(),
                "iconURL", roleIcon.getIconUrl(),
                "isEmoji", function(args -> roleIcon.isEmoji())
            )
        );
    }

    public static ProxyObject roleTags(RoleTags roleTags) {
        return fromMap(roleTags,
            Map.of(
                "botId", roleTags.getBotIdLong(),
                "integrationId", roleTags.getIntegrationIdLong(),
                "isBoost", function(args -> roleTags.isBoost()),
                "isBot", function(args -> roleTags.isIntegration()),
                "isIntegration", function(args -> roleTags.isIntegration())
            )
        );
    }

    public static ProxyObject slashCommandInteractionEvent(SlashCommandInteractionEvent event) {
        final Map<String, Object> entries = new HashMap<>();
        entries.put("user", toProxy(event.getUser()));
        entries.put("channel", toProxy(event.getChannel()));
        entries.put("channelType", toProxy(event.getChannelType()));
        entries.put("guild", toProxy(event.getGuild()));
        entries.put("member", toProxy(event.getMember()));
        entries.put("guildLocale", toProxy(event.getGuildLocale()));
        entries.put("commandId", event.getCommandIdLong());
        entries.put("responseNumber", event.getResponseNumber());
        entries.put("isFromGuild", function(args -> event.isFromGuild()));
        entries.put("isAcknowledged", function(args -> event.isAcknowledged()));
        entries.put("id", event.getIdLong());
        entries.put("commandPath", event.getCommandPath());
        entries.put("commandString", event.getCommandString());
        entries.put("commandType", toProxy(event.getCommandType()));
        entries.put("hook", toProxy(event.getHook()));
        entries.put("interation", toProxy(event.getInteraction()));
        entries.put("name", event.getName());
        entries.put("options", toProxy(event.getOptions()));
        entries.put("subcommandGroup", event.getSubcommandGroup());
        entries.put("subcommandName", event.getSubcommandName());
        entries.put("token", event.getToken());
        entries.put("type", toProxy(event.getType()));
        entries.put("rawType", event.getTypeRaw());
        entries.put("userLocale", toProxy(event.getUserLocale()));

        return fromMap(event, entries);
    }

    public static ProxyObject stageChannel(StageChannel channel) {
        final Map<String, Object> entries = new HashMap<>(audioChannelDetails(channel));
        entries.put("topic", channel.getStageInstance().getTopic());
        entries.put("privacyLevel", toProxy(channel.getStageInstance().getPrivacyLevel()));
        entries.put("audience", toProxy(channel.getStageInstance().getAudience()));
        entries.put("speakers", toProxy(channel.getStageInstance().getSpeakers()));
        return fromMap(channel, entries);
    }

    public static ProxyObject stickerFormat(StickerFormat stickerFormat) {
        return fromMap(stickerFormat,
            Map.of(
                "extension", stickerFormat.getExtension(),
                "name", stickerFormat.name(),
                "ordinal", stickerFormat.ordinal()
            )
        );
    }

    public static ProxyObject textChannel(TextChannel channel) {
        final Map<String, Object> entries = new HashMap<>(guildMessageChannelDetails(channel));
        entries.put("slowmode", channel.getSlowmode());
        return fromMap(channel, entries);
    }

    public static ProxyObject threadChannel(ThreadChannel channel) {
        final Map<String, Object> entries = new HashMap<>(guildMessageChannelDetails(channel));
        entries.put("isPublic", function(args -> channel.isPublic()));
        entries.put("messageCount", channel.getMessageCount());
        entries.put("memberCount", channel.getMemberCount());
        entries.put("isJoined", function(args -> channel.isJoined()));
        entries.put("isLocked", function(args -> channel.isLocked()));
        entries.put("isInvitable", function(args -> channel.isInvitable()));
        entries.put("parentChannel", ProxyObject.fromMap(guildMessageChannelDetails(channel.getParentMessageChannel())));
        entries.put("members", toProxy(channel.getThreadMembers()));
        entries.put("isOwner", function(args -> channel.isOwner()));
        entries.put("ownerId", channel.getOwnerIdLong());
        entries.put("owner", toProxy(channel.getOwner()));
        entries.put("isArchived", function(args -> channel.isArchived()));
        entries.put("lastArchiveUpdate", toProxy(channel.getTimeArchiveInfoLastModified()));
        entries.put("slowmode", channel.getSlowmode());
        entries.put("autoArchiveDuration", toProxy(channel.getAutoArchiveDuration()));
        return fromMap(channel, entries);
    }

    public static ProxyObject threadMember(ThreadMember threadMember) {
        return fromMap(threadMember,
            Map.of(
                "mention", threadMember.getAsMention(),
                "id", threadMember.getIdLong(),
                "user", toProxy(threadMember.getGuild()),
                "member", toProxy(threadMember.getMember()),
                "avatarURL", toProxy(threadMember.getThread()),
                "timeCreated", toProxy(threadMember.getTimeCreated()),
                "timeJoined", toProxy(threadMember.getTimeJoined()),
                "user", toProxy(threadMember.getUser()),
                "isThreadOwner", function(args -> threadMember.isThreadOwner()),
                "rawFlags", threadMember.getFlagsRaw()
            )
        );
    }

    public static ProxyObject thumbnail(Thumbnail thumbnail) {
        return fromMap(thumbnail,
            Map.of(
                "height", thumbnail.getHeight(),
                "width", thumbnail.getWidth(),
                "proxyURL", thumbnail.getProxyUrl(),
                "url", thumbnail.getUrl()
            )
        );
    }

    @SuppressWarnings("unchecked")
    public static Object toProxy(Object object) {
        if(object instanceof final User user) return user(user);
        if(object instanceof final Member member) return function(args -> member(member));
        if(object instanceof final Guild guild) return function(args -> guild(guild));
        if(object instanceof final CustomEmoji emoji) return emoji(emoji);
        if(object instanceof final Role role) return function(args -> role(role));
        if(object instanceof final RoleTags roleTags) return roleTags(roleTags);
        if(object instanceof final RoleIcon roleIcon) return roleIcon(roleIcon);
        if(object instanceof final Message message) return function(args -> message(message));
        if(object instanceof final ActionRow actionRow) return actionRow(actionRow);
        if(object instanceof final ActionComponent actionComponent) return actionComponent(actionComponent);
        if(object instanceof final Component.Type componentType) return componentType(componentType);
        if(object instanceof final Button button) return button(button);
        if(object instanceof final ItemComponent itemComponent) return itemComponent(itemComponent);
        if(object instanceof final MessageActivity messageActivity) return messageActivity(messageActivity);
        if(object instanceof final MessageActivity.ActivityType messageActivityType) return messageActivityType(messageActivityType);
        if(object instanceof final MessageActivity.Application messageActivityApplication) return messageActivityApplication(messageActivityApplication);
        if(object instanceof final Message.Interaction messageInteraction) return function(args -> messageInteraction(messageInteraction));
        if(object instanceof final MessageReference messageReference) return function(args -> messageReference(messageReference));
        if(object instanceof final MessageReaction messageReaction) return function(args -> messageReaction(messageReaction));
        if(object instanceof final RichSticker messageSticker) return function(args -> messageSticker(messageSticker));
        if(object instanceof final MessageReceivedEvent messageReceivedEvent) return function(args -> messageReceivedEvent(messageReceivedEvent));
        if(object instanceof final SlashCommandInteractionEvent slashCommandInteractionEvent) return function(args -> slashCommandInteractionEvent(slashCommandInteractionEvent));
        if(object instanceof final Command.Type commandType) return commandType(commandType);
        if(object instanceof final InteractionType interactionType) return interactionType(interactionType);
        if(object instanceof final InteractionHook interactionHook) return function(args -> interactionHook(interactionHook));
        if(object instanceof final Interaction interaction) return function(args -> interaction(interaction));
        if(object instanceof final StickerFormat stickerFormat) return stickerFormat(stickerFormat);
        if(object instanceof final MessageType messageType) return messageType(messageType);
        if(object instanceof final MessageFlag messageFlag) return messageFlag(messageFlag);
        if(object instanceof final ButtonStyle buttonStyle) return buttonStyle(buttonStyle);
        if(object instanceof final Permission permission) return permission(permission);
        if(object instanceof final ChannelType channelType) return channelType(channelType);
        if(object instanceof final AutoArchiveDuration autoArchiveDuration) return autoArchiveDuration(autoArchiveDuration);
        if(object instanceof final Region region) return region(region);
        if(object instanceof final PrivacyLevel privacyLevel) return privacyLevel(privacyLevel);
        if(object instanceof final OnlineStatus onlineStatus) return onlineStatus(onlineStatus);
        if(object instanceof final MessageEmbed messageEmbed) return messageEmbed(messageEmbed);
        if(object instanceof final AuthorInfo authorInfo) return authorInfo(authorInfo);
        if(object instanceof final Footer footer) return footer(footer);
        if(object instanceof final ImageInfo imageInfo) return imageInfo(imageInfo);
        if(object instanceof final Provider provider) return provider(provider);
        if(object instanceof final Thumbnail thumbnail) return thumbnail(thumbnail);
        if(object instanceof final VideoInfo videoInfo) return videoInfo(videoInfo);
        if(object instanceof final Category category) return function(args -> category(category));
        if(object instanceof final ThreadChannel threadChannel) return function(args -> threadChannel(threadChannel));
        if(object instanceof final TextChannel textChannel) return function(args -> textChannel(textChannel));
        if(object instanceof final GuildMessageChannel guildMessageChannel) return function(args -> ProxyObject.fromMap(guildMessageChannelDetails(guildMessageChannel)));
        if(object instanceof final StageChannel stageChannel) return function(args -> stageChannel(stageChannel));
        if(object instanceof final VoiceChannel voiceChannel) return function(args -> voiceChannel(voiceChannel));
        if(object instanceof final AudioChannel audioChannel) return function(args -> ProxyObject.fromMap(audioChannelDetails(audioChannel)));
        if(object instanceof final GuildChannel guildChannel) return function(args -> ProxyObject.fromMap(guildChannelDetails(guildChannel)));
        if(object instanceof final PrivateChannel privateChannel) return function(args -> privateChannel(privateChannel));
        if(object instanceof final MessageChannel messageChannel) return function(args -> ProxyObject.fromMap(messageChannelDetails(messageChannel)));
        if(object instanceof final Channel channel) return function(args -> ProxyObject.fromMap(channelDetails(channel)));
        if(object instanceof final Locale locale) return locale(locale);
        if(object instanceof final Color color) return color(color);
        if(object instanceof final Instant instant) return instant(instant);
        if(object instanceof final LocalDate date) return ProxyDate.from(date);
        if(object instanceof final LocalTime time) return ProxyTime.from(time);
        if(object instanceof final Duration duration) return ProxyDuration.from(duration);
        if(object instanceof final ZoneId timeZone) return ProxyTimeZone.from(timeZone);
        if(object instanceof final TemporalAccessor accessor) return instant(accessor);
        if(object instanceof final Object[] array) return array(array);
        if(object instanceof final Map map) return map(map);
        if(object instanceof final Collection collection) return array(collection);
        if(object instanceof final Iterable iterable) return ProxyIterable.from(iterable);
        if(object instanceof final Iterator iterator) return ProxyIterator.from(iterator);

        return object;
    }

    public static ProxyObject user(User user) {
        return fromMap(user,
            Map.of(
                "mention", user.getAsMention(),
                "avatarURL", user.getEffectiveAvatarUrl(),
                "discriminator", user.getDiscriminator(),
                "name", user.getName(),
                "id", user.getIdLong(),
                "timeCreated", ProxyInstant.from(Instant.from(user.getTimeCreated())),
                "isBot", function(args -> user.isBot()),
                "isSystem", function(args -> user.isSystem())
            )
        );
    }

    public static ProxyObject videoInfo(VideoInfo videoInfo) {
        return fromMap(videoInfo,
            Map.of(
                "height", videoInfo.getHeight(),
                "videoInfo", videoInfo.getWidth(),
                "url", videoInfo.getUrl()
            )
        );
    }

    public static ProxyObject voiceChannel(VoiceChannel channel) {
        final Map<String, Object> entries = new HashMap<>(audioChannelDetails(channel));
        entries.put("userLimit", channel.getUserLimit());
        return fromMap(channel, entries);
    }

    private static ProxyObject fromMap(Object object, Map<String, Object> values) {
        final Map<String, Object> newBindings = new HashMap<>(values);
        newBindings.put("toString", function(args -> object.toString()));
        newBindings.put("hashCode", function(args -> object.hashCode()));
        newBindings.put("equals",
            function(args ->
                !args.isEmpty() &&
                args.get(0).isMetaInstance(object.getClass()) &&
                object.equals(args.get(0).as(object.getClass()))
            )
        );
        return ProxyObject.fromMap(newBindings);
    }
}
