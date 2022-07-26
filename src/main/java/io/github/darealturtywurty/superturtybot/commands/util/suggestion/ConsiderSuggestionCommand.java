package io.github.darealturtywurty.superturtybot.commands.util.suggestion;

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import io.github.darealturtywurty.superturtybot.database.pojos.SuggestionResponse;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Suggestion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ConsiderSuggestionCommand extends CoreCommand {
    public ConsiderSuggestionCommand() {
        super(new Types(false, true, false, false));
    }
    
    @Override
    public String getAccess() {
        return "Server Owner";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }
    
    @Override
    public String getDescription() {
        return "Considers a suggestion";
    }
    
    @Override
    public String getHowToUse() {
        return ".consider [number]\n.consider [number] [reason]";
    }
    
    @Override
    public String getName() {
        return "consider";
    }
    
    @Override
    public String getRichName() {
        return "Consider Suggestion";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runNormalMessage(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            event.getMessage().reply("You must be in a server to use this command!").mentionRepliedUser(false).queue();
            return;
        }

        if (event.getAuthor().getIdLong() != event.getGuild().getOwnerIdLong()) {
            reply(event, "❌ You must be the server owner to use this command!", false);
            return;
        }
        
        final TextChannel suggestionChannel = SuggestionManager.getSuggestionChannel(event);
        if (suggestionChannel == null)
            return;
        
        final String message = event.getMessage().getContentRaw();
        final String[] args = message.split(" ");
        if (args.length < 2) {
            event.getMessage().reply("You must supply at least the suggestion number!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        String response = "No reason given";
        int suggestionNumber = 0;
        try {
            suggestionNumber = Integer.parseInt(args[1]);
        } catch (final NumberFormatException exception) {
            event.getMessage().reply("You must supply a valid suggestion number!").mentionRepliedUser(false).queue();
            return;
        }
        
        if (args.length >= 3) {
            response = String.join(" ", args).replace(args[0] + " " + args[1], "");
        }
        
        final CompletableFuture<Suggestion> suggestion = SuggestionManager.respondSuggestion(event.getGuild(),
            suggestionChannel, event.getMember(), suggestionNumber, response, SuggestionResponse.Type.CONSIDERED);
        
        suggestion.thenAccept(sug -> {
            if (sug == null) {
                event.getMessage().reply("You must provide a valid suggestion number!").mentionRepliedUser(false)
                    .queue();
                return;
            }
            
            event.getMessage().delete().queue();
            event.getAuthor().openPrivateChannel().queue(channel -> {
                final var embed = new EmbedBuilder();
                embed.setTimestamp(Instant.now());
                embed.setColor(sug != null ? Color.GREEN : Color.RED);
                embed.setTitle("✅ Response successfully added!", "https://discord.com/channels/"
                    + event.getGuild().getIdLong() + "/" + suggestionChannel.getIdLong() + "/" + sug.getMessage());
                embed.setFooter(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(),
                    event.getMember().getEffectiveAvatarUrl());
                
                channel.sendMessageEmbeds(embed.build()).queue();
            });
        });
    }
}
