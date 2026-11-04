package dev.darealturtywurty.superturtybot.modules.privacy;

import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.modules.AutoModerator;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.user.GenericUserEvent;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public final class PrivacyEventManager extends InterfacedEventManager {
    @Override
    public void handle(@NotNull GenericEvent event) {
        PrivacyPreferences.optionalOperation(() -> {
            dispatch(event);
            return null;
        });
    }

    private void dispatch(GenericEvent event) {
        Long userId = actor(event);
        if (userId == null || !PrivacyPreferences.isOptedOut(userId)) {
            if (userId != null && (event instanceof MessageReceivedEvent message && !message.getAuthor().isBot()
                || event instanceof GenericCommandInteractionEvent command && !command.getName().equals("privacy")
                || event instanceof GenericComponentInteractionCreateEvent component
                    && !component.getComponentId().startsWith("privacy:")
                || event instanceof GenericMessageReactionEvent || event instanceof GenericGuildVoiceEvent
                || event instanceof ModalInteractionEvent)) {
                try {
                    RetentionActivity.touch(userId);
                } catch (RuntimeException exception) {
                    Constants.LOGGER.warn("Could not save retention activity ({}); event handling continues",
                        exception.getClass().getSimpleName());
                }
            }
            super.handle(event);
            return;
        }

        switch (event) {
            case GenericCommandInteractionEvent command -> {
                if (PrivacyPreferences.isAllowedCommand(command.getName())) {
                    super.handle(event);
                } else {
                    command.reply(PrivacyPreferences.BLOCKED_MESSAGE).setEphemeral(true).queue();
                }

                return;
            }
            case CommandAutoCompleteInteractionEvent autocomplete -> {
                if (PrivacyPreferences.isAllowedCommand(autocomplete.getName())) {
                    super.handle(event);
                } else {
                    autocomplete.replyChoices().queue();
                }

                return;
            }
            case GenericComponentInteractionCreateEvent component -> {
                if (component.getComponentId().startsWith("privacy:")) {
                    super.handle(event);
                } else {
                    component.reply(PrivacyPreferences.BLOCKED_MESSAGE).setEphemeral(true).queue();
                }

                return;
            }
            case ModalInteractionEvent modal -> {
                modal.reply(PrivacyPreferences.BLOCKED_MESSAGE).setEphemeral(true).queue();
                return;
            }
            default -> {
            }
        }

        for (Object listener : getRegisteredListeners()) {
            if (listener instanceof AutoModerator
                || (event instanceof MessageReceivedEvent && listener instanceof CoreCommand)) {
                ((ListenerAdapter) listener).onEvent(event);
            }
        }
    }

    private static Long actor(GenericEvent event) {
        if (event instanceof GenericInteractionCreateEvent interaction)
            return interaction.getUser().getIdLong();
        if (event instanceof MessageReceivedEvent message)
            return message.getAuthor().getIdLong();
        if (event instanceof MessageUpdateEvent message)
            return message.getAuthor().getIdLong();
        if (event instanceof GenericMessageReactionEvent reaction)
            return reaction.getUserIdLong();
        if (event instanceof GenericGuildMemberEvent member)
            return member.getUser().getIdLong();
        if (event instanceof GuildMemberRemoveEvent member)
            return member.getUser().getIdLong();
        if (event instanceof GenericGuildVoiceEvent voice)
            return voice.getMember().getIdLong();
        if (event instanceof GenericUserEvent user)
            return user.getUser().getIdLong();
        return null;
    }
}
