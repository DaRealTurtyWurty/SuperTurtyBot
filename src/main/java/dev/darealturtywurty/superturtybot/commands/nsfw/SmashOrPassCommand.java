package dev.darealturtywurty.superturtybot.commands.nsfw;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.api.pojo.Pornstar;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SmashOrPassCommand extends CoreCommand {
    public SmashOrPassCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.NSFW;
    }

    @Override
    public String getDescription() {
        return "Play a game of smash or pass with a random pornstar!";
    }

    @Override
    public String getName() {
        return "smashorpass";
    }

    @Override
    public String getRichName() {
        return "Smash or Pass";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 15L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!NSFWCommand.isValidChannel(event.getChannel())) {
            event.deferReply(true).setContent("❌ This command can only be used in NSFW channels!").queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild != null) {
            GuildData config = Database.getDatabase().guildData.find(Filters.eq("guild", guild.getIdLong()))
                    .first();
            if (config == null) {
                event.deferReply(true).setContent("❌ This server has not been configured yet!").queue();
                return;
            }

            List<Long> enabledChannels = GuildData.getChannels(config.getNsfwChannels());
            if (enabledChannels.isEmpty()) {
                event.deferReply(true).setContent("❌ This server has no NSFW channels configured!").queue();
                return;
            }

            if (!enabledChannels.contains(event.getChannel().getIdLong())) {
                event.deferReply(true).setContent("❌ This channel is not configured as an NSFW channel!").queue();
                return;
            }
        }

        event.deferReply().queue();

        Either<Pornstar, HttpStatus> response = ApiHandler.getPornstar();
        if (response.isRight()) {
            event.getHook().sendMessage("❌ Failed to get pornstar!").queue();
            return;
        }

        Pornstar pornstar = response.getLeft();
        List<String> photos = pornstar.getPhotos();
        while (photos.isEmpty()) {
            response = ApiHandler.getPornstar();
            if (response.isRight()) {
                event.getHook().sendMessage("❌ Failed to get pornstar!").queue();
                return;
            }

            pornstar = response.getLeft();
            photos = pornstar.getPhotos();
        }

        final String photo = photos.get(ThreadLocalRandom.current().nextInt(photos.size()));
        final String name = pornstar.getName();

        var embed = new EmbedBuilder()
                .setTitle("Smash or Pass?")
                .setDescription("Would you smash or pass " + name + "?")
                .setImage(photo);

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
