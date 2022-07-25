package io.github.darealturtywurty.superturtybot.commands.util;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.vdurmont.emoji.EmojiParser;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class ReactionRoleCommand extends CoreCommand {
    private static final Map<Long, List<ReactionRole>> REACTION_ROLES = new HashMap<>();
    
    public ReactionRoleCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<SubcommandData> createSubcommands() {
        return List.of(
            new SubcommandData("create", "Creates a reaction role with the supplied roles")
                .addOption(OptionType.STRING, "message",
                    "The message that you want to provide along with the resulting embed", true)
                .addOption(OptionType.STRING, "emoji1", "The first reaction emoji (I must be able to use this emoji)",
                    true)
                .addOption(OptionType.ROLE, "role1", "The role to add for the first emoji", true)
                .addOption(OptionType.STRING, "emoji2", "The second reaction emoji (I must be able to use this emoji)",
                    false)
                .addOption(OptionType.ROLE, "role2", "The role to add for the second emoji", false)
                .addOption(OptionType.STRING, "emoji3", "The third reaction emoji (I must be able to use this emoji)",
                    false)
                .addOption(OptionType.ROLE, "role3", "The role to add for the third emoji", false)
                .addOption(OptionType.STRING, "emoji4", "The fourth reaction emoji (I must be able to use this emoji)",
                    false)
                .addOption(OptionType.ROLE, "role4", "The role to add for the fourth emoji", false)
                .addOption(OptionType.CHANNEL, "channel", "The channel to send this to", false),
            new SubcommandData("edit", "Edits an existing reaction role")
                .addOption(OptionType.STRING, "uuid", "The UUID of the reaction role", true)
                .addOption(OptionType.ROLE, "to_edit", "The role to replace", true)
                .addOption(OptionType.ROLE, "new_role", "The new role to replace the old one", true),
            new SubcommandData("remove", "Removes an existing reaction role")
                .addOption(OptionType.STRING, "uuid", "The UUID of the reaction role", true)
                .addOption(OptionType.ROLE, "role", "The role to remove from this reaction role", false)
                .addOption(OptionType.STRING, "emoji", "The emoji to remove from this reaction role", false),
            new SubcommandData("list", "Lists all of the reaction roles in this server"));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Allows this server to have a message where reactions grant roles.";
    }
    
    @Override
    public String getName() {
        return "reactionrole";
    }
    
    @Override
    public String getRichName() {
        return "Reaction Role";
    }
    
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.isFromGuild() || event.getUser().isBot() || event.getUser().isSystem()
            || !REACTION_ROLES.containsKey(event.getGuild().getIdLong()))
            return;
        
        final List<ReactionRole> reactionRoles = REACTION_ROLES.get(event.getGuild().getIdLong());
        if (reactionRoles.isEmpty())
            return;
        
        List<ReactionRole> matches = reactionRoles.stream()
            .filter(rr -> rr.getMessageInfo().messageId == event.getMessageIdLong()).toList();
        
        if (matches.isEmpty())
            return;
        
        matches = matches.stream().filter(rr -> rr.getEmoji().replace("<:", "").replace("<a:", "").replace(">", "")
            .equals(event.getReaction().getEmoji().getName())).toList();
        
        if (matches.isEmpty())
            return;
        
        final ReactionRole reactionRole = matches.get(0);
        if (event.getMember().getRoles().stream().anyMatch(role -> role.getIdLong() == reactionRole.getRoleId())) {
            event.getGuild()
                .removeRoleFromMember(event.getMember(), event.getGuild().getRoleById(reactionRole.getRoleId()))
                .queue(success -> {
                
                }, error -> {
                    
                });
        } else {
            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(reactionRole.getRoleId()))
                .queue(success -> {
                
                }, error -> {
                    
                });
        }
    }
    
    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (!event.isFromGuild() || event.getUser().isBot() || event.getUser().isSystem()
            || !REACTION_ROLES.containsKey(event.getGuild().getIdLong()))
            return;
        
        final List<ReactionRole> reactionRoles = REACTION_ROLES.get(event.getGuild().getIdLong());
        if (reactionRoles.isEmpty())
            return;
        
        List<ReactionRole> matches = reactionRoles.stream()
            .filter(rr -> rr.getMessageInfo().messageId == event.getMessageIdLong()).toList();
        
        if (matches.isEmpty())
            return;
        
        matches = matches.stream().filter(rr -> rr.getEmoji().replace("<:", "").replace("<a:", "").replace(">", "")
            .equals(event.getReaction().getEmoji().getName())).toList();
        
        if (matches.isEmpty())
            return;
        
        final ReactionRole reactionRole = matches.get(0);
        if (event.getMember().getRoles().stream().anyMatch(role -> role.getIdLong() == reactionRole.getRoleId())) {
            event.getGuild()
                .removeRoleFromMember(event.getMember(), event.getGuild().getRoleById(reactionRole.getRoleId()))
                .submit();
        } else {
            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(reactionRole.getRoleId()))
                .submit();
        }
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("You must be in a server to use this command!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        final String subcommand = event.getSubcommandName();
        switch (subcommand.toLowerCase().trim()) {
            case "create": {
                final String message = event.getOption("message").getAsString();
                final Map<String, Role> emojiRoleMap = new HashMap<>();
                for (int index = 0; index < 4; ++index) {
                    final String emoji = event.getOption("emoji" + index, null,
                        readEmoji(event.getJDA(), event.getGuild()));
                    
                    if (emoji == null || "?".equals(emoji)) {
                        continue;
                    }
                    
                    final Role role = event.getOption("role" + index, null, readRole());
                    
                    if (role == null || !event.getMember().canInteract(role)
                        || !event.getGuild().getSelfMember().canInteract(role)) {
                        continue;
                    }
                    
                    emojiRoleMap.put(emoji, role);
                }
                
                if (emojiRoleMap.isEmpty()) {
                    event.deferReply(true).setContent("You must supply at least 1 emoji and role")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final MessageChannel channel = event.getOption("channel", event.getChannel(),
                    option -> option.getAsChannel().asStandardGuildMessageChannel() == null ? event.getChannel()
                        : option.getAsChannel().asStandardGuildMessageChannel());
                
                event.deferReply(true).mentionRepliedUser(false).queue();
                
                final var embed = new EmbedBuilder();
                embed.setTitle(message);
                embed.setTimestamp(Instant.now());
                embed.setColor(event.getMember().getColorRaw());
                for (final Entry<String, Role> emojiRole : emojiRoleMap.entrySet()) {
                    embed.appendDescription("React with: " + emojiRole.getKey() + " to get "
                        + emojiRole.getValue().getAsMention() + "\n\n");
                }
                
                channel.sendMessageEmbeds(embed.build()).queue(msg -> {
                    final var messageInfo = new MessageInfo(event.getGuild().getIdLong(), channel.getIdLong(),
                        msg.getIdLong(), event.getUser().getIdLong());
                    msg.editMessageEmbeds(
                        embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " | ID: "
                            + messageInfo.uuid(), event.getUser().getEffectiveAvatarUrl()).build())
                        .queue();
                    
                    event.getHook().sendMessage("Reaction role added: " + msg.getJumpUrl()).queue();
                    
                    for (final Entry<String, Role> emojiRole : emojiRoleMap.entrySet()) {
                        msg.addReaction(Emoji.fromFormatted(stripEmote(emojiRole.getKey()))).queue();
                        
                        REACTION_ROLES.computeIfAbsent(event.getGuild().getIdLong(), id -> new ArrayList<>());
                        final List<ReactionRole> reactionRoles = REACTION_ROLES.get(event.getGuild().getIdLong());
                        
                        reactionRoles
                            .add(new ReactionRole(messageInfo, emojiRole.getKey(), emojiRole.getValue().getIdLong()));
                        
                        REACTION_ROLES.put(event.getGuild().getIdLong(), reactionRoles);
                    }
                });
                
                break;
            }
            
            case "edit": {
                final UUID uuid = event.getOption("uuid", null, option -> {
                    try {
                        return UUID.fromString(option.getAsString());
                    } catch (final IllegalArgumentException exception) {
                        return null;
                    }
                });
                
                if (uuid == null) {
                    event.deferReply(true)
                        .setContent(
                            "You must supply a valid UUID. You can use `/reactionrole list` for a list of valid UUIDs!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                List<ReactionRole> reactionRoles;
                if (!REACTION_ROLES.containsKey(event.getGuild().getIdLong())) {
                    event.deferReply(true).setContent("This server does not contain any reaction roles!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                reactionRoles = REACTION_ROLES.get(event.getGuild().getIdLong());
                
                if (reactionRoles.isEmpty()) {
                    event.deferReply(true).setContent("This server does not contain any reaction roles!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final List<ReactionRole> found = reactionRoles.stream()
                    .filter(rr -> rr.getMessageInfo().uuid().equals(uuid)
                        && rr.getMessageInfo().authorId() == event.getUser().getIdLong())
                    .toList();
                if (found.isEmpty()) {
                    event.deferReply(true)
                        .setContent(
                            "You must supply a valid UUID. You can use `/reactionrole list` for a list of valid UUIDs!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final ReactionRole first = found.get(0);
                event.getGuild().getTextChannelById(first.getMessageInfo().channelId())
                    .retrieveMessageById(first.getMessageInfo().messageId()).queue(message -> {
                        final Role role = event.getOption("to_edit", null, readRole());
                        
                        if (role == null) {
                            event.deferReply(true).setContent("You must supply a valid role!").mentionRepliedUser(false)
                                .queue();
                            return;
                        }
                        
                        if (!event.getMember().canInteract(role)) {
                            event.deferReply(true)
                                .setContent("You can only use this command on a role that you can interact with!")
                                .mentionRepliedUser(false).queue();
                            return;
                        }
                        
                        final Optional<ReactionRole> optionalFoundRole = found.stream()
                            .filter(rr -> rr.roleId == role.getIdLong()).findFirst();
                        if (!optionalFoundRole.isPresent()) {
                            event.deferReply(true).setContent("There is no reaction role that matches this role!")
                                .mentionRepliedUser(false).queue();
                            return;
                        }
                        
                        final ReactionRole foundReactionRole = optionalFoundRole.get();
                        if (foundReactionRole == null) {
                            event.deferReply(true).setContent("You must supply a valid role!").mentionRepliedUser(false)
                                .queue();
                            return;
                        }
                        
                        final Role foundRole = event.getGuild().getRoleById(foundReactionRole.getRoleId());
                        
                        if (foundRole == null) {
                            event.deferReply(true).setContent("You must supply a valid role!").mentionRepliedUser(false)
                                .queue();
                            return;
                        }
                        
                        if (!event.getMember().canInteract(foundRole)) {
                            event.deferReply(true)
                                .setContent("You can only use this command on a role that you can interact with!")
                                .mentionRepliedUser(false).queue();
                            return;
                        }
                        
                        final Role newRole = event.getOption("new_role", null, readRole());
                        
                        if (newRole == null) {
                            event.deferReply(true).setContent("You must supply a valid role!").mentionRepliedUser(false)
                                .queue();
                            return;
                        }
                        
                        if (!event.getMember().canInteract(newRole)) {
                            event.deferReply(true)
                                .setContent("You can only use this command with a role that you can interact with!")
                                .mentionRepliedUser(false).queue();
                            return;
                        }
                        
                        reactionRoles.remove(foundReactionRole);
                        
                        final var newReactionRole = new ReactionRole(foundReactionRole.getMessageInfo(),
                            foundReactionRole.getEmoji(), newRole.getIdLong());
                        reactionRoles.add(newReactionRole);
                        
                        message.clearReactions(Emoji.fromFormatted(stripEmote(foundReactionRole.getEmoji())))
                            .queue(success -> message
                                .addReaction(Emoji.fromFormatted(stripEmote(foundReactionRole.getEmoji()))).queue());
                        
                        final var embed = new EmbedBuilder(message.getEmbeds().get(0));
                        embed.setDescription(message.getEmbeds().get(0).getDescription().replace(role.getAsMention(),
                            newRole.getAsMention()));
                        message.editMessageEmbeds(embed.build()).queue();
                        
                        event.deferReply(true).setContent("I have successfully replaced " + foundRole.getAsMention()
                            + " with " + newRole.getAsMention() + "!").mentionRepliedUser(false).queue();
                        
                        REACTION_ROLES.put(event.getGuild().getIdLong(), reactionRoles);
                    }, error -> {
                        event.deferReply(true)
                            .setContent(
                                "This reaction role no longer exists and has now been removed from my database!")
                            .mentionRepliedUser(false).queue();
                        reactionRoles.removeAll(found);
                        REACTION_ROLES.put(event.getGuild().getIdLong(), reactionRoles);
                    });
                
                break;
            }
            
            case "remove": {
                final UUID uuid = event.getOption("uuid", null, option -> {
                    try {
                        return UUID.fromString(option.getAsString());
                    } catch (final IllegalArgumentException exception) {
                        return null;
                    }
                });
                
                if (uuid == null) {
                    event.deferReply(true)
                        .setContent(
                            "You must supply a valid UUID. You can use `/reactionrole list` for a list of valid UUIDs!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                List<ReactionRole> reactionRoles;
                if (!REACTION_ROLES.containsKey(event.getGuild().getIdLong())) {
                    event.deferReply(true).setContent("This server does not contain any reaction roles!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                reactionRoles = REACTION_ROLES.get(event.getGuild().getIdLong());
                
                if (reactionRoles.isEmpty()) {
                    event.deferReply(true).setContent("This server does not contain any reaction roles!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final List<ReactionRole> found = reactionRoles.stream()
                    .filter(rr -> rr.getMessageInfo().uuid().equals(uuid)
                        && rr.getMessageInfo().authorId() == event.getUser().getIdLong())
                    .toList();
                
                if (found.isEmpty()) {
                    event.deferReply(true)
                        .setContent(
                            "You must supply a valid UUID. You can use `/reactionrole list` for a list of valid UUIDs!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final ReactionRole first = found.get(0);
                event.getGuild().getTextChannelById(first.getMessageInfo().channelId())
                    .retrieveMessageById(first.getMessageInfo().messageId()).queue(message -> {
                        final Role role = event.getOption("role", null, readRole());
                        final String emoji = event.getOption("emoji", null,
                            readEmoji(event.getJDA(), event.getGuild()));
                        
                        if (role == null && emoji == null) {
                            event.deferReply(true).setContent("You must supply either a role or an emoji to remove!")
                                .mentionRepliedUser(false).queue();
                            return;
                        }
                        
                        if (role != null) {
                            if (!event.getMember().canInteract(role)) {
                                event.deferReply(true)
                                    .setContent("You can only use this command on a role that you can interact with!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            final Optional<ReactionRole> optionalFoundRole = found.stream()
                                .filter(rr -> rr.roleId == role.getIdLong()).findFirst();
                            if (!optionalFoundRole.isPresent()) {
                                event.deferReply(true).setContent("There is no reaction role that matches this role!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            final ReactionRole foundReactionRole = optionalFoundRole.get();
                            if (foundReactionRole == null) {
                                event.deferReply(true).setContent("You must supply a valid role!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            final Role foundRole = event.getGuild().getRoleById(foundReactionRole.getRoleId());
                            
                            if (foundRole != null && !event.getMember().canInteract(foundRole)) {
                                event.deferReply(true)
                                    .setContent("You can only use this command on a role that you can interact with!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            if (found.size() <= 1) {
                                message.delete().queue();
                                reactionRoles.removeAll(found);
                                event.deferReply(true)
                                    .setContent("I have removed this reaction role since it was now empty!")
                                    .mentionRepliedUser(false).queue();
                            } else {
                                reactionRoles.remove(foundReactionRole);
                                message.clearReactions(Emoji.fromFormatted(stripEmote(foundReactionRole.getEmoji())))
                                    .queue();
                                
                                final var embed = new EmbedBuilder(message.getEmbeds().get(0));
                                
                                final String[] lines = message.getEmbeds().get(0).getDescription().split("\n");
                                String result = message.getEmbeds().get(0).getDescription();
                                for (final String line : lines) {
                                    if (line.contains(foundRole.getAsMention())) {
                                        result = result.replace(line, "");
                                    }
                                }
                                
                                embed.setDescription(result);
                                message.editMessageEmbeds(embed.build()).queue();
                                
                                event.deferReply(true).setContent("I have successfully removed " + role.getAsMention()
                                    + " and its corresponding emoji!").mentionRepliedUser(false).queue();
                            }
                            
                            REACTION_ROLES.put(event.getGuild().getIdLong(), reactionRoles);
                        } else {
                            final Optional<ReactionRole> optionalFoundRole = found.stream()
                                .filter(rr -> rr.emoji.equals(emoji)).findFirst();
                            if (!optionalFoundRole.isPresent()) {
                                event.deferReply(true).setContent("There is no reaction role that matches this emoji!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            final ReactionRole foundReactionRole = optionalFoundRole.get();
                            if (foundReactionRole == null) {
                                event.deferReply(true).setContent("You must supply a valid emoji!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            final Role correspondingRole = event.getGuild().getRoleById(foundReactionRole.getRoleId());
                            if (correspondingRole != null && !event.getMember().canInteract(correspondingRole)) {
                                event.deferReply(true)
                                    .setContent("You can only use this command on a role that you can interact with!")
                                    .mentionRepliedUser(false).queue();
                                return;
                            }
                            
                            if (found.size() <= 1) {
                                message.delete().queue();
                                reactionRoles.removeAll(found);
                                event.deferReply(true)
                                    .setContent("I have removed this reaction role since it was now empty!")
                                    .mentionRepliedUser(false).queue();
                            } else {
                                reactionRoles.remove(foundReactionRole);
                                message.clearReactions(Emoji.fromFormatted(stripEmote(foundReactionRole.getEmoji())))
                                    .queue();
                                
                                final var embed = new EmbedBuilder(message.getEmbeds().get(0));
                                
                                final String[] lines = message.getEmbeds().get(0).getDescription().split("\n");
                                String result = message.getEmbeds().get(0).getDescription();
                                for (final String line : lines) {
                                    if (line.contains(foundReactionRole.getEmoji())) {
                                        result = result.replace(line, "");
                                    }
                                }
                                
                                embed.setDescription(result);
                                message.editMessageEmbeds(embed.build()).queue();
                                
                                event.deferReply(true)
                                    .setContent("I have successfully removed " + emoji + " and its corresponding role!")
                                    .mentionRepliedUser(false).queue();
                            }
                        }
                    }, error -> {
                        reactionRoles.removeAll(found);
                        REACTION_ROLES.put(event.getGuild().getIdLong(), reactionRoles);
                        
                        event.deferReply(true)
                            .setContent(
                                "This reaction role no longer exists and has now been removed from my database!")
                            .mentionRepliedUser(false).queue();
                    });
                
                break;
            }
            
            case "list": {
                List<ReactionRole> reactionRoles;
                if (!REACTION_ROLES.containsKey(event.getGuild().getIdLong())) {
                    event.deferReply(true).setContent("This server does not contain any reaction roles!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                reactionRoles = REACTION_ROLES.get(event.getGuild().getIdLong());
                
                if (reactionRoles.isEmpty()) {
                    event.deferReply(true).setContent("This server does not contain any reaction roles!")
                        .mentionRepliedUser(false).queue();
                    return;
                }
                
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(Color.BLUE);
                embed.setFooter(event.getUser().getName() + "#" + event.getUser().getDiscriminator(),
                    event.getUser().getEffectiveAvatarUrl());
                embed.setTitle("Reaction roles in this server:");
                
                final var future = new CompletableFuture<Boolean>();
                final Stream<ReactionRole> streaming = reactionRoles.stream()
                    .sorted(Comparator.comparingLong(rr -> rr.getMessageInfo().messageId()));
                
                final var counter = new AtomicInteger(0);
                streaming.forEach(
                    reactionRole -> event.getGuild().getTextChannelById(reactionRole.getMessageInfo().channelId())
                        .retrieveMessageById(reactionRole.getMessageInfo().messageId()).queue(msg -> {
                            final Role role = event.getGuild().getRoleById(reactionRole.getRoleId());
                            if (role == null) {
                                reactionRoles.remove(reactionRole);
                                msg.clearReactions(Emoji.fromFormatted(stripEmote(reactionRole.getEmoji())));
                            } else {
                                embed.appendDescription(role.getAsMention() + " - [View here](" + msg.getJumpUrl()
                                    + ")\n\t UUID: " + reactionRole.getMessageInfo().uuid() + "\n\n");
                            }
                            
                            if (counter.incrementAndGet() >= reactionRoles.size()) {
                                future.complete(true);
                            }
                        }, error -> {
                            reactionRoles.remove(reactionRole);
                            error.printStackTrace();
                        }));
                
                REACTION_ROLES.put(event.getGuild().getIdLong(), reactionRoles);
                future
                    .thenAccept(done -> event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue());
                break;
            }
            
            default: {
                event.deferReply(true).setContent("You must provide a valid action!").mentionRepliedUser(false).queue();
                break;
            }
        }
    }
    
    @NotNull
    private static Function<OptionMapping, String> readEmoji(JDA jda, Guild guild) {
        return option -> {
            final String string = option.getAsString();
            
            final List<String> emojis = EmojiParser.extractEmojis(string);
            final List<Long> emotes = MentionType.EMOJI.getPattern().matcher(string).results()
                .map(result -> string.substring(result.start(), result.end())).map(str -> {
                    final String[] parts = str.split(":");
                    if (parts.length < 1)
                        return 0L;
                    
                    final String id = parts[parts.length - 1].replace(">", "").replace("<", "");
                    try {
                        return Long.parseLong(id);
                    } catch (final NumberFormatException exception) {
                        return 0L;
                    }
                }).toList();
            if (emojis.isEmpty()) {
                if (emotes.isEmpty())
                    return null;
                
                final RichCustomEmoji emote = jda.getEmojiById(emotes.get(0));
                if (emote == null || emote.getGuild().getMemberById(guild.getSelfMember().getIdLong()) == null
                    || !emote.getGuild().getMemberById(guild.getSelfMember().getIdLong()).canInteract(emote))
                    return null;
                
                return emote.getAsMention();
            }
            
            return Emoji.fromFormatted(emojis.get(0)).getName();
        };
    }
    
    @NotNull
    private static Function<OptionMapping, Role> readRole() {
        return option -> {
            try {
                return option.getAsRole();
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        };
    }
    
    private static String stripEmote(String emote) {
        return emote.replace("<:", "").replace("<a:", "").replace(">", "");
    }
    
    public static final class ReactionRole {
        private final MessageInfo messageInfo;
        private String emoji;
        private long roleId;
        
        public ReactionRole(MessageInfo messageInfo, String emoji, long roleId) {
            this.messageInfo = messageInfo;
            this.emoji = emoji;
            this.roleId = roleId;
        }
        
        public String getEmoji() {
            return this.emoji;
        }
        
        public MessageInfo getMessageInfo() {
            return this.messageInfo;
        }
        
        public long getRoleId() {
            return this.roleId;
        }
        
        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }
        
        public void setRoleId(long roleId) {
            this.roleId = roleId;
        }
    }
    
    private record MessageInfo(@Nullable Long guildId, long channelId, long messageId, long authorId, UUID uuid) {
        public MessageInfo(@Nullable Long guildId, long channelId, long messageId, long authorId) {
            this(guildId, channelId, messageId, authorId, UUID.randomUUID());
        }
    }
}
