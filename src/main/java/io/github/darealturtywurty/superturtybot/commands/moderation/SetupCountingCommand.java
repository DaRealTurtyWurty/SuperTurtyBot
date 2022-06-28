package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.awt.Color;
import java.time.Instant;
import java.util.List;

import org.apache.commons.text.WordUtils;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.TurtyBotDatabase;
import io.github.darealturtywurty.superturtybot.modules.counting.CountingMode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SetupCountingCommand extends CoreCommand {
    public SetupCountingCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.CHANNEL, "channel", "The channel to start counting on", true),
            new OptionData(OptionType.STRING, "mode", "The counting mode to use", true).addChoice("normal", "normal")
                .addChoice("none", "none"));
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Sets up a counting channel";
    }
    
    @Override
    public String getName() {
        return "setup-counting";
    }
    
    @Override
    public String getRichName() {
        return "Setup Counting";
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You must be in a server to use this command!", false, true);
            return;
        }
        
        final TextChannel channel = event.getOption("channel").getAsTextChannel();
        if (channel == null) {
            reply(event, "❌ You must supply a text channel!", false, true);
            return;
        }
        
        if (!event.getMember().isOwner()) {
            reply(event, "❌ You do not have permission to change the counting mode.", false, true);
            return;
        }
        
        final CountingMode mode = CountingMode.valueOf(event.getOption("mode").getAsString().toUpperCase().trim());
        if (mode == null) {
            reply(event, "❌ You must supply a valid counting mode!", false, true);
            return;
        }
        
        if (TurtyBotDatabase.COUNTING.isCountingChannel(event.getGuild(), channel)) {
            if (mode != CountingMode.NONE) {
                reply(event,
                    "❌ This channel is already a counting channel! Use mode: `None` to remove counting from a channel.",
                    false, true);
                return;
            }
            
            TurtyBotDatabase.COUNTING.setCountingChannel(event.getGuild(), channel.getIdLong(), CountingMode.NONE);
            reply(event, "✅ " + channel.getAsMention() + " has been unregistered as a counting channel!");
            final var embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTimestamp(Instant.now());
            embed.setDescription("This channel is no longer a counting channel!");
            channel.sendMessageEmbeds(embed.build()).queue();
            return;
        }
        
        TurtyBotDatabase.COUNTING.setCountingChannel(event.getGuild(), channel.getIdLong(), mode);
        reply(event, "✅ " + channel.getAsMention() + " has now been registered as a counting channel with mode: "
            + WordUtils.capitalize(mode.name().toLowerCase()));
    }
}
