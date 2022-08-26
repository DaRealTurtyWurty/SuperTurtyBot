package io.github.darealturtywurty.superturtybot.commands.music;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.StringUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class QueueCommand extends CoreCommand {
    public QueueCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }

    @Override
    public String getDescription() {
        return "Gets the current queue of the bot";
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getRichName() {
        return "Queue";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        final String componentId = event.getComponentId();
        final String[] parts = componentId.split("-");
        if (!componentId.startsWith("queue_") || !event.isFromGuild() || parts.length < 2)
            return;

        final String type = parts[0];
        final String messageId = parts[1];
        if (!event.getMessageId().equals(messageId))
            return;

        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue == null || queue.isEmpty()) {
            event.editMessage("There are currently no items in the queue. Use `/play` to add something to the queue!")
                .setEmbeds().queue();
            return;
        }

        final List<List<AudioTrack>> pages = Lists.partition(queue, 15);
        switch (type) {
            case "queue_pages_first": {
                final var embed = getPage(event.getGuild(), pages, 0);
                final Button firstPage = Button.primary("queue_pages_first-" + messageId, Emoji.fromUnicode("⏮️"))
                    .asDisabled();
                final Button previousPage = Button.primary("queue_pages_previous-" + messageId, Emoji.fromUnicode("◀️"))
                    .asDisabled();
                final Button close = Button.danger("queue_close-" + messageId, Emoji.fromUnicode("❌"));
                Button nextPage = Button.primary("queue_pages_next-" + messageId, Emoji.fromUnicode("▶️"));
                Button lastPage = Button.primary("queue_pages_last-" + messageId, Emoji.fromUnicode("⏭️"));
                if (pages.size() < 2) {
                    nextPage = nextPage.asDisabled();
                    lastPage = lastPage.asDisabled();
                }

                event.editMessageEmbeds(embed.build()).setActionRow(firstPage, previousPage, close, nextPage, lastPage)
                    .queue();
                break;
            }

            case "queue_pages_previous": {
                var embed = event.getMessage().getEmbeds().get(0);
                int page = Integer.parseInt(embed.getFooter().getText().replace("Page: ", "").split("/")[0]);
                embed = getPage(event.getGuild(), pages, --page - 1).build();

                Button firstPage = Button.primary("queue_pages_first-" + messageId, Emoji.fromUnicode("⏮️"));
                Button previousPage = Button.primary("queue_pages_previous-" + messageId, Emoji.fromUnicode("◀️"));
                if (page < 2) {
                    firstPage = firstPage.asDisabled();
                    previousPage = previousPage.asDisabled();
                }

                final Button close = Button.danger("queue_close-" + messageId, Emoji.fromUnicode("❌"));
                Button nextPage = Button.primary("queue_pages_next-" + messageId, Emoji.fromUnicode("▶️"));
                Button lastPage = Button.primary("queue_pages_last-" + messageId, Emoji.fromUnicode("⏭️"));
                if (page >= pages.size()) {
                    nextPage = nextPage.asDisabled();
                    lastPage = lastPage.asDisabled();
                }

                event.editMessageEmbeds(embed).setActionRow(firstPage, previousPage, close, nextPage, lastPage).queue();
                break;
            }

            case "queue_close": {
                event.getChannel().asTextChannel().deleteMessageById(messageId).queue();
                break;
            }

            case "queue_pages_next": {
                var embed = event.getMessage().getEmbeds().get(0);
                final int page = Integer.parseInt(embed.getFooter().getText().replace("Page: ", "").split("/")[0]);
                embed = getPage(event.getGuild(), pages, page).build();

                Button firstPage = Button.primary("queue_pages_first-" + messageId, Emoji.fromUnicode("⏮️"));
                Button previousPage = Button.primary("queue_pages_previous-" + messageId, Emoji.fromUnicode("◀️"));
                if (page + 1 < 2) {
                    firstPage = firstPage.asDisabled();
                    previousPage = previousPage.asDisabled();
                }

                final Button close = Button.danger("queue_close-" + messageId, Emoji.fromUnicode("❌"));
                Button nextPage = Button.primary("queue_pages_next-" + messageId, Emoji.fromUnicode("▶️"));
                Button lastPage = Button.primary("queue_pages_last-" + messageId, Emoji.fromUnicode("⏭️"));
                if (page + 1 >= pages.size()) {
                    nextPage = nextPage.asDisabled();
                    lastPage = lastPage.asDisabled();
                }

                event.editMessageEmbeds(embed).setActionRow(firstPage, previousPage, close, nextPage, lastPage).queue();
                break;
            }

            case "queue_pages_last": {
                final var embed = getPage(event.getGuild(), pages, pages.size() - 1);
                Button firstPage = Button.primary("queue_pages_first-" + messageId, Emoji.fromUnicode("⏮️"));
                Button previousPage = Button.primary("queue_pages_previous-" + messageId, Emoji.fromUnicode("◀️"));
                if (pages.size() < 2) {
                    firstPage = firstPage.asDisabled();
                    previousPage = previousPage.asDisabled();
                }

                final Button close = Button.danger("queue_close-" + messageId, Emoji.fromUnicode("❌"));
                final Button nextPage = Button.primary("queue_pages_next-" + messageId, Emoji.fromUnicode("▶️"))
                    .asDisabled();
                final Button lastPage = Button.primary("queue_pages_last-" + messageId, Emoji.fromUnicode("⏭️"))
                    .asDisabled();

                event.editMessageEmbeds(embed.build()).setActionRow(firstPage, previousPage, close, nextPage, lastPage)
                    .queue();
                break;
            }

            default:
                throw new UnsupportedOperationException(
                    "Button on queue page that should not exist!\nID: " + componentId);
        }
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply(true).setContent("❌ You must be in a server to use this command!")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getGuild().getAudioManager().isConnected()) {
            event.deferReply(true)
                .setContent("❌ I am not in a voice channel right now! Use `/joinvc` to put me in a voice channel.")
                .mentionRepliedUser(false).queue();
            return;
        }

        if (!event.getMember().getVoiceState().inAudioChannel()) {
            event.deferReply(true).setContent("❌ You must be in a voice channel to use this command!")
                .mentionRepliedUser(false).queue();
        }

        final List<AudioTrack> queue = AudioManager.getQueue(event.getGuild());
        if (queue == null || queue.isEmpty()) {
            event.deferReply(true)
                .setContent("There are currently no items in the queue. Use `/play` to add something to the queue!")
                .mentionRepliedUser(false).queue();
            return;
        }

        final List<List<AudioTrack>> pages = Lists.partition(queue, 15);
        final var embed = getPage(event.getGuild(), pages, 0);

        event.deferReply().addEmbeds(embed.build()).mentionRepliedUser(false).queue(msg -> {
            final long messageId = msg.getInteraction().getMessageChannel().getLatestMessageIdLong();
            final Button firstPage = Button.primary("queue_pages_first-" + messageId, Emoji.fromUnicode("⏮️"))
                .asDisabled();
            final Button previousPage = Button.primary("queue_pages_previous-" + messageId, Emoji.fromUnicode("◀️"))
                .asDisabled();
            final Button close = Button.danger("queue_close-" + messageId, Emoji.fromUnicode("❌"));
            Button nextPage = Button.primary("queue_pages_next-" + messageId, Emoji.fromUnicode("▶️"));
            Button lastPage = Button.primary("queue_pages_last-" + messageId, Emoji.fromUnicode("⏭️"));
            if (pages.size() < 2) {
                nextPage = nextPage.asDisabled();
                lastPage = lastPage.asDisabled();
            }

            msg.editMessageById(messageId, "").setActionRow(firstPage, previousPage, close, nextPage, lastPage).queue();

            event.getChannel().deleteMessageById(messageId).queueAfter(60, TimeUnit.SECONDS, success -> {
            }, error -> {
            });
        });
    }

    private static EmbedBuilder getPage(Guild guild, List<List<AudioTrack>> pages, int number) {
        final List<AudioTrack> queue = pages.stream().reduce(new ArrayList<>(), (list0, list1) -> {
            list0.addAll(list1);
            return list0;
        });

        final var embed = new EmbedBuilder();
        embed.setTimestamp(Instant.now());
        embed.setColor(Color.BLUE);
        embed.setTitle("Music queue for server: " + guild.getName());
        embed.setFooter("Page: " + (number + 1) + "/" + pages.size());

        final List<AudioTrack> page = pages.get(number);

        for (final AudioTrack audioTrack : page) {
            embed.appendDescription(
                queue.indexOf(audioTrack) + 1 + " - [" + StringUtils.millisecondsFormatted(audioTrack.getDuration())
                    + "] [" + audioTrack.getInfo().title.replaceAll("\\[[^\\]]++\\]|\\([^\\)]++\\)", "").trim() + "]("
                    + audioTrack.getInfo().uri + ") - "
                    + guild.getMemberById(String.valueOf(audioTrack.getUserData())).getAsMention() + "\n");
        }

        return embed;
    }
}
