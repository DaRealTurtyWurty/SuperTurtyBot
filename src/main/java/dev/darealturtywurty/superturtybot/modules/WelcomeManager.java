package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WelcomeManager extends ListenerAdapter {
    public static final WelcomeManager INSTANCE = new WelcomeManager();

    private static final List<String> WELCOME_MESSAGES = List.of(
            "Hey there %s, welcome to the server! Prepare for illogical memes and questionable life choices.",
            "A wild %s appears! Get your pokeballs ready, everyone!",
            "Buckle up, %s! This server is a roller coaster of fun, puns, and the occasional existential crisis.",
            "%s has entered the chat! Now brace yourselves for an avalanche of bad jokes.",
            "Welcome, %s! We're glad you're here. Just try not to break anything, and we'll get along just fine.",
            "Welcome aboard, %s! Now that you're here, you can't escape... (Just kidding! ...Mostly.)",
            "*Insert obligatory \"Hi %s\" here.* (Just kidding, we're actually really excited you're here!)",
            "Welcome, %s! We're so glad you could join us. Now, let's get this party started!",
            "We interrupt your regularly scheduled program to welcome %s to the server!",
            "On a scale of 1 to meow, how excited are you to be here, %s?",
            "Welcome, %s! We hope you brought snacks, because we're all out of them.",
            "%s, welcome to the party! We were just about to order pizza. Did you want pineapple on yours? (Wrong answers only.)",
            "*Confused Gandalf voice* You... shall not... pass... (Psych! Welcome, %s!)",
            "%s, welcome to the server! We hope you like puns, because we have a lot of them. We mean a LOT. (Like this one.)",
            "*Insert deafening silence here.* (Just kidding, welcome %s! We were just practicing our invisibility skills.)",
            "Hey %s, welcome! Just a heads up, we take our tea breaks very seriously around here. :flag_gb: :tea:",
            "*Whispers* Psst, %s, you haven't seen a talking cat around here, have you? (We promise we're totally normal...)",
            "*Inserts welcome mat that says \"Welcome %s!\"* (We may not be the most creative bunch, but we're happy to have you here!)",
            "*Sprinkles confetti everywhere* Welcome, %s! (We may have gotten a little carried away with the welcome committee.)",
            "Welcome, %s! Just remember, the only rule here is: there are no rules. (Except maybe don't set the server on fire. Please. We just got it redecorated.)",
            "Welcome, %s! We're so glad you could join us. Just a heads up, we're a little... quirky around here. (But we promise it's all in good fun!)",
            "Welcome, %s! We're so excited you're here. Just a heads up, we have a strict \"no boring people allowed\" policy. (Just kidding! ...Mostly.)"
    );

    private static final List<String> GOODBYE_MESSAGES = List.of(
            "*Sad trombone sound effect* Looks like %s has flown the coop. Don't be a stranger... unless you are a stranger, then definitely be a stranger.",
            "%s has left the chat. *crickets chirp* Anyone else hear that deafening silence?",
            "Farewell, %s! May your future endeavors be filled with more memes and fewer existential dread spirals than you found here.",
            "*Casually whistles* So long, %s! Don't let the door hit you on the way out... unless you're into that sort of thing.",
            "Goodbye, %s! We'll miss your puns, your memes, and your general aura of chaos. (But mostly the puns.)",
            "And just like that, %s is gone. *Cue dramatic music* We'll never forget the time you... um... well, we'll never forget you, %s!",
            "Farewell, %s! We'll miss your presence in the server. (But don't worry, we'll still be here when you decide to come back!)",
            "Goodbye, %s! We'll miss your unique brand of chaos around here. (But don't worry, we'll keep the chaos going in your honor!)",
            "%s was last seen running away from a never-ending gif war. We'll miss you... maybe.",
            "%s has decided to pursue other digital adventures. We wish them luck, and hope they find a server with even worse puns. (Impossible!)",
            "Plays \"*The Final Countdown*\" The clock is ticking, %s! You have 10 seconds to reconsider leaving this amazing server... 9... 8... (Just kidding, we miss you already!)",
            "*Points to a door marked \"Exit\"* That way out, %s? But seriously, take care!",
            "*Inserts \"Directed by Robert B. Weide\" meme* So %s decided to leave the server. Let's hear what the folks at home have to say!",
            "*Puts a \"For Sale\" sign on %s's former role* Slightly used server member slot for sale! Must love bad jokes and existential dread. (Inquiries welcome, %s not included.)",
            "*Plays \"The Funeral March\"* RIP, %s's time in this server. (They may still be alive, but their server presence is deceased.)");

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();

        GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (data == null) {
            data = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(data);
        }

        if (data.getWelcomeChannel() != 0) {
            Member member = event.getMember();
            String welcomeMessage = WELCOME_MESSAGES.get((int) (Math.random() * WELCOME_MESSAGES.size()));
            TextChannel welcomeChannel = guild.getTextChannelById(data.getWelcomeChannel());
            if (welcomeChannel != null)
                welcomeChannel.sendMessageFormat(welcomeMessage, member.getAsMention()).queue();
            else {
                data.setWelcomeChannel(0);
                Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guild.getIdLong()), data);
            }
        }
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();

        GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong())).first();
        if (data == null) {
            data = new GuildData(guild.getIdLong());
            Database.getDatabase().guildData.insertOne(data);
        }

        if (data.getWelcomeChannel() != 0) {
            User user = event.getUser();
            String goodbyeMessage = GOODBYE_MESSAGES.get((int) (Math.random() * GOODBYE_MESSAGES.size()));
            TextChannel welcomeChannel = guild.getTextChannelById(data.getWelcomeChannel());
            if (welcomeChannel != null)
                welcomeChannel.sendMessageFormat(goodbyeMessage, user.getAsMention() + "(" + user.getEffectiveName().replace("%", "%%") + ")").queue();
            else {
                data.setWelcomeChannel(0);
                Database.getDatabase().guildData.replaceOne(Filters.eq("guild", guild.getIdLong()), data);
            }
        }
    }
}
