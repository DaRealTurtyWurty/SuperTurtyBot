package dev.darealturtywurty.superturtybot.commands.moderation;

import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddRoleToThreadCommand extends CoreCommand {
    public AddRoleToThreadCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.ROLE, "role", "The role to add to the channel.", false),
                new OptionData(OptionType.CHANNEL, "channel", "The channel to add the role to.", false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MODERATION;
    }

    @Override
    public String getDescription() {
        return "Adds a role to a thread.";
    }

    @Override
    public String getName() {
        return "addroletothread";
    }

    @Override
    public String getRichName() {
        return "Add Role To Thread";
    }

    @Override
    public String getAccess() {
        return "Manage Server";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 5L);
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if(guild == null || event.getMember() == null) {
            reply(event, "❌ This command can only be used in a server!", false, true);
            return;
        }

        if(!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            reply(event, "❌ You do not have permission to use this command!", false, true);
            return;
        }


        GuildChannel guildChannel = event.getOption("channel", event.getGuildChannel(), OptionMapping::getAsChannel);
        if(guildChannel == null) {
            reply(event, "❌ You must provide a channel!", false, true);
            return;
        }

        if(!(guildChannel instanceof ThreadChannel threadChannel)) {
            reply(event, "❌ You must provide a thread!", false, true);
            return;
        }

        Role role = event.getOption("role", guild.getPublicRole(), OptionMapping::getAsRole);
        if(role == null) {
            reply(event, "❌ You must provide a role!", false, true);
            return;
        }

        if(!guild.getRoles().contains(role)) {
            reply(event, "❌ That role does not exist in this server!", false, true);
            return;
        }

        Member selfMember = guild.getSelfMember();
        if(!selfMember.canInteract(role)) {
            reply(event, "❌ I cannot interact with that role!", false, true);
            return;
        }

        if(!threadChannel.canTalk(selfMember)) {
            reply(event, "❌ I cannot interact with that thread!", false, true);
            return;
        }

        if(threadChannel.isLocked()) {
            reply(event, "❌ That thread is locked!", false, true);
            return;
        }

        event.deferReply().queue();

        guild.findMembersWithRoles(role).onSuccess(members -> {
            if(members.isEmpty()) {
                event.getHook().editOriginal("❌ That role has no members!").queue();
                return;
            }

            List<String> messages = new ArrayList<>();
            var strBuilder = new StringBuilder();
            for(Member member : members) {
                if(strBuilder.length() + member.getAsMention().length() + 2 > 2000) {
                    messages.add(strBuilder.toString());
                    strBuilder = new StringBuilder();
                }

                strBuilder.append(member.getAsMention()).append(" ");
            }

            if(!strBuilder.isEmpty()) {
                messages.add(strBuilder.toString());
            }

            for(String message : messages) {
                threadChannel.sendMessageEmbeds(
                        new EmbedBuilder()
                                .setDescription("Added " + role.getAsMention() + " to " + message)
                                .build())
                        .queue(sentMessage ->
                                sentMessage.editMessage(message).queue(ignored ->
                                        ignored.delete().queueAfter(1, TimeUnit.SECONDS)));
            }

            event.getHook()
                    .editOriginal("✅ Added " + role.getAsMention() + " to " + threadChannel.getAsMention() + "!")
                    .setAllowedMentions(List.of())
                    .queue();
        });
    }
}
