package io.github.darealturtywurty.superturtybot.commands.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.math3.util.Pair;

import io.github.darealturtywurty.superturtybot.core.command.CommandCategory;
import io.github.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PurgeCommand extends CoreCommand {
    public PurgeCommand() {
        super(new Types(true, false, false, false));
    }
    
    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.INTEGER, "amount", "The number of messages to delete.", false)
            .setRequiredRange(1, 500),
            new OptionData(OptionType.USER, "user", "The user to delete messages from.", false));
    }
    
    @Override
    public String getAccess() {
        return "Moderators (Manage Messages Permission)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }
    
    @Override
    public String getDescription() {
        return "Purges a channel of n messages";
    }
    
    @Override
    public String getHowToUse() {
        return "/purge\n/purge [amount]\n/purge [user]\n/purge [amount] [user]";
    }
    
    public CompletableFuture<List<Message>> getMessages(MessageChannel channel, int amount) {
        return channel.getIterableHistory().takeAsync(amount) // Collect 'amount' messages
            .thenApply(list -> list);
    }
    
    public CompletableFuture<List<Message>> getMessagesByUser(MessageChannel channel, int amount, User user) {
        return channel.getIterableHistory().takeAsync(amount) // Collect 'amount' messages
            .thenApply(list -> list.stream().filter(m -> m.getAuthor().equals(user)) // Filter messages by author
                .toList());
    }
    
    @Override
    public String getName() {
        return "purge";
    }
    
    @Override
    public boolean isServerOnly() {
        return true;
    }
    
    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.deferReply().setContent("I can only perform this action inside of servers!").mentionRepliedUser(false)
                .queue();
            return;
        }
        
        if (!event.getMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
            event.deferReply(true).setContent("You require the `Manage Messages` permission to perform this action!")
                .mentionRepliedUser(false).queue();
            return;
        }
        
        final int amount = event.getOption("amount", 100, OptionMapping::getAsInt);
        if (amount < 1 || amount > 500) {
            event.getMember().ban(0, "Hacking TurtyBot").queue();
        }
        
        final OptionMapping userOption = event.getOption("user");
        
        CompletableFuture<List<Message>> messages = null;
        if (userOption != null) {
            messages = getMessagesByUser(event.getMessageChannel(), amount, userOption.getAsUser());
        } else {
            messages = getMessages(event.getMessageChannel(), amount);
        }
        
        messages.thenAccept(msgs -> {
            event.deferReply(true).setContent("I have found " + msgs.size() + " messages, I will now start purging!")
                .mentionRepliedUser(false).queue();
            
            final List<AtomicBoolean> complete = new ArrayList<>();
            msgs.forEach(msg -> complete.add(new AtomicBoolean(false)));
            CompletableFuture.allOf(event.getGuildChannel().purgeMessages(msgs).toArray(new CompletableFuture[0]))
                .thenAccept(action -> event.getMessageChannel().sendMessage(
                    event.getMember().getAsMention() + " ✅ I have successfully purged " + msgs.size() + " messages!")
                    .queue(success -> {
                        final Pair<Boolean, TextChannel> logging = BanCommand.canLog(event.getGuild());
                        if (Boolean.TRUE.equals(logging.getKey())) {
                            BanCommand.log(logging.getValue(),
                                event.getMember().getAsMention() + " has purged " + msgs.size() + " messages"
                                    + (userOption != null ? " from user " + userOption.getAsUser().getAsMention() : "")
                                    + "!",
                                false);
                        }
                    }));
        });
    }
}
