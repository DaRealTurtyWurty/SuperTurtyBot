package dev.darealturtywurty.superturtybot.commands.fun;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SmashOrPassCommand extends CoreCommand {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final List<Instance> INSTANCES = new ArrayList<>();

    private static final List<Button> BUTTONS = List.of(
            Button.primary("smash", "Smash"),
            Button.danger("pass", "Pass")
    );

    public SmashOrPassCommand() {
        super(new Types(true, false, false, false));
    }

    private static void finish(Message message) {
        Instance instance = INSTANCES.stream().filter(inst -> inst.messageId == message.getIdLong()).findFirst().orElse(null);
        if (instance != null) {
            INSTANCES.remove(instance);
        } else {
            Constants.LOGGER.error("Instance was null while trying to finish the Smash or Pass command!");
            message.delete().queue();
            return;
        }

        message.editMessage("> " + message.getContentRaw() + "\nThe results are in! Smash: " + instance.smashIds.size() + " | Pass: " + instance.passIds.size())
                .setComponents()
                .queue(edited -> {
                    if (instance.smashIds.isEmpty() && instance.passIds.isEmpty()) {
                        edited.delete().queue();
                    }
                });
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "Decide whether you would smash or pass on a celebrity!";
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
        return Pair.of(TimeUnit.SECONDS, 10L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        event.deferReply().mentionRepliedUser(false).queue();

        Either<Pair<String, byte[]>, HttpStatus> result = ApiHandler.getRandomCelebrity();
        if (result.isRight()) {
            event.getHook().sendMessage("❌ An error occurred while trying to get a random celebrity!").queue();
            Constants.LOGGER.error("An error occurred while trying to get a random celebrity! Code: {}", result.getRight());
            return;
        }

        Pair<String, byte[]> pair = result.getLeft();
        String name = pair.getLeft();
        byte[] image = pair.getRight();

        try (FileUpload upload = FileUpload.fromData(image, name + ".jpg")) {
            WebhookMessageCreateAction<Message> request = event.getHook()
                    .sendMessage("Would you smash or pass on **" + name + "**?")
                    .setFiles(upload);

            if (event.isFromGuild()) {
                GuildData data = Database.getDatabase().guildData.find(Filters.eq("guild", event.getGuild().getIdLong())).first();
                if (data == null) {
                    data = new GuildData(event.getGuild().getIdLong());
                    Database.getDatabase().guildData.insertOne(data);
                }

                if (data.isAddSmashOrPassButtons()) {
                    request.setComponents(ActionRow.of(BUTTONS)).queue(message -> {
                        INSTANCES.add(new Instance(event.isFromGuild() ? event.getGuild().getIdLong() : -1L, event.getChannel().getIdLong(), message.getIdLong()));
                        EXECUTOR.schedule(() -> finish(message), 1, TimeUnit.MINUTES);
                    });

                    return;
                }
            }

            request.queue();
        } catch (IOException exception) {
            event.getHook().sendMessage("❌ An error occurred while trying to send the image!").queue();
            Constants.LOGGER.error("An error occurred while trying to send the image!", exception);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getComponentId().equalsIgnoreCase("smash") && !event.getComponentId().equalsIgnoreCase("pass"))
            return;

        event.deferEdit().queue();

        Instance instance = INSTANCES.stream().filter(inst -> inst.messageId == event.getMessageIdLong() &&
                        inst.channelId == event.getChannel().getIdLong() &&
                        inst.guildId == event.getGuild().getIdLong())
                .findFirst()
                .orElse(null);
        if (instance == null) {
            event.getHook().deleteOriginal().queue();
            Constants.LOGGER.error("Instance was null while trying to interact with a button!");
            return;
        }

        if (instance.shouldIgnore(event.getUser().getIdLong()))
            return;

        instance.interact(event);
    }

    public static class Instance {
        private final long guildId, channelId, messageId;
        private final List<Long> smashIds = new ArrayList<>(), passIds = new ArrayList<>();

        public Instance(long guildId, long channelId, long messageId) {
            this.guildId = guildId;
            this.channelId = channelId;
            this.messageId = messageId;
        }

        public void interact(ButtonInteractionEvent event) {
            if (event.getComponentId().equalsIgnoreCase("smash")) {
                this.smashIds.add(event.getUser().getIdLong());
            } else if (event.getComponentId().equalsIgnoreCase("pass")) {
                this.passIds.add(event.getUser().getIdLong());
            }
        }

        public boolean shouldIgnore(long userId) {
            return this.smashIds.contains(userId) || this.passIds.contains(userId);
        }
    }
}
