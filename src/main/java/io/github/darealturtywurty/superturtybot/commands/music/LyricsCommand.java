package io.github.darealturtywurty.superturtybot.commands.music;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import genius.SongSearch;
import genius.SongSearch.Hit;
import io.github.darealturtywurty.superturtybot.commands.music.handler.AudioManager;
import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.core.util.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class LyricsCommand extends CoreCommand {
    private static final Map<Long, String> ID_LYRIC_MAP = new HashMap<>();
    
    public LyricsCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MUSIC;
    }
    
    @Override
    public String getDescription() {
        return "Get lyrics for the currently playing song";
    }
    
    @Override
    public String getName() {
        return "lyrics";
    }
    
    @Override
    public String getRichName() {
        return "Song Lyrics";
    }
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.isFromGuild())
            return;
        
        final String id = event.getComponentId();
        if (!id.startsWith("lyrics-"))
            return;
        
        final String[] parts = id.split("-");
        final long channel = Long.parseLong(parts[1]);
        final long user = Long.parseLong(parts[2]);
        final long hitId = Long.parseLong(parts[3]);
        final int page = Integer.parseInt(parts[4].replace("page", ""));
        final String action = parts[5];
        
        int newPage = page;
        if ("prev".equals(action)) {
            newPage--;
            
            if (newPage == 0) {
                event.editButton(event.getButton().asDisabled()).queue();
            }

            if (newPage < 0) {
                event.deferEdit().queue();
                return;
            }
            
            final String lyrics = ID_LYRIC_MAP.get(hitId);
            final String prevPage = lyrics.substring(0 + newPage * 1024,
                Math.min(lyrics.length(), 1024 + newPage * 1024));
            final ActionRow row = event.getMessage().getActionRows().get(0);
            Button previous = row.getButtons().get(0);
            previous = previous.withId(parts[0] + "-" + channel + "-" + user + "-" + hitId + "-" + newPage + "-prev");
            Button close = row.getButtons().get(1).withDisabled(false);
            close = close.withId(parts[0] + "-" + channel + "-" + user + "-" + hitId + "-" + newPage + "-close");
            Button next = row.getButtons().get(2).withDisabled(false);
            next = next.withId(parts[0] + "-" + channel + "-" + user + "-" + hitId + "-" + newPage + "-next");
            
            if (newPage != 0) {
                event
                    .editMessageEmbeds(
                        new EmbedBuilder(event.getMessage().getEmbeds().get(0)).setDescription(prevPage).build())
                    .setComponents(ActionRow.of(previous, close, next)).queue();
                
            } else {
                event.getHook()
                    .editOriginalEmbeds(
                        new EmbedBuilder(event.getMessage().getEmbeds().get(0)).setDescription(prevPage).build())
                    .setComponents(ActionRow.of(previous, close, next)).queue();
            }
        } else if ("close".equals(action)) {
            event.deferEdit().flatMap(InteractionHook::deleteOriginal).queue();
            ID_LYRIC_MAP.remove(hitId);
        } else if ("next".equals(action)) {
            newPage++;
            
            final String lyrics = ID_LYRIC_MAP.get(hitId);
            if (newPage == lyrics.length() / 1024) {
                event.editButton(event.getButton().withDisabled(true)).queue();
            }

            if (newPage > lyrics.length() / 1024)
                return;
            
            if (newPage > 0) {
                final String nextPage = lyrics.substring(0 + newPage * 1024,
                    Math.min(lyrics.length(), 1024 + newPage * 1024));
                
                final ActionRow row = event.getMessage().getActionRows().get(0);
                Button previous = row.getButtons().get(0).withDisabled(false);
                previous = previous
                    .withId(parts[0] + "-" + channel + "-" + user + "-" + hitId + "-" + newPage + "-prev");
                Button close = row.getButtons().get(1);
                close = close.withId(parts[0] + "-" + channel + "-" + user + "-" + hitId + "-" + newPage + "-close");
                Button next = row.getButtons().get(2);
                next = next.withId(parts[0] + "-" + channel + "-" + user + "-" + hitId + "-" + newPage + "-next");
                
                if (newPage != lyrics.length() / 1024) {
                    event
                        .editMessageEmbeds(
                            new EmbedBuilder(event.getMessage().getEmbeds().get(0)).setDescription(nextPage).build())
                        .setComponents(ActionRow.of(previous, close, next)).queue();

                } else {
                    event.getHook()
                        .editOriginalEmbeds(
                            new EmbedBuilder(event.getMessage().getEmbeds().get(0)).setDescription(nextPage).build())
                        .setComponents(ActionRow.of(previous, close, next)).queue();
                }
            }
        }
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !AudioManager.isPlaying(event.getGuild())) {
            reply(event, "❌ You must be in a server with a track currently playing!", false, true);
            return;
        }
        
        event.deferReply().mentionRepliedUser(false).queue();
        
        try {
            final SongSearch search = Constants.GENIUS_LYRICS
                .search(AudioManager.getCurrentlyPlaying(event.getGuild()).getInfo().title);
            final LinkedList<Hit> hits = search.getHits();
            if (hits.isEmpty()) {
                event.getHook().editOriginal("❌ There are no lyrics found for this song!").queue();
                return;
            }
            
            final Hit hit = hits.get(0);
            final var embed = new EmbedBuilder();
            embed.setTitle("Lyrics for " + hit.getTitleWithFeatured(), hit.getUrl());
            embed.setThumbnail(hit.getThumbnailUrl());
            embed.setTimestamp(Instant.now());
            embed.setColor(Color.BLUE);
            embed.setFooter(hit.getArtist().getName(), hit.getArtist().getImageUrl());
            
            final String lyrics = hit.fetchLyrics();
            ID_LYRIC_MAP.put(hit.getId(), lyrics);
            
            final String page0 = lyrics.substring(0, Math.min(lyrics.length(), 1024));
            embed.setDescription(page0);
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.primary("lyrics-" + event.getChannel().getId() + "-" + event.getUser().getId() + "-"
                        + hit.getId() + "-page0" + "-prev", Emoji.fromUnicode("◀️")).asDisabled(),
                    Button.danger("lyrics-" + event.getChannel().getId() + "-" + event.getUser().getId() + "-"
                        + hit.getId() + "-page0" + "-close", Emoji.fromUnicode("⚠️")),
                    Button.primary("lyrics-" + event.getChannel().getId() + "-" + event.getUser().getId() + "-"
                        + hit.getId() + "-page0" + "-next", Emoji.fromUnicode("▶️"))))
                .queue();
        } catch (final IOException exception) {
            exception.printStackTrace();
            reply(event, "❌ There has been an issue getting the lyrics for this track!", false, true);
        }
    }
}
