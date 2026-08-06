package dev.darealturtywurty.superturtybot.commands.fun;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.discord.PaginatedEmbed;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollector;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollectorRegistry;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectablePresentation;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
import java.util.*;

public class CollectablesCommand extends CoreCommand {
    private static final int FIELDS_PER_PAGE = 5;
    private static final int FIELD_VALUE_MAX_LENGTH = 1_024;

    public CollectablesCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        List<CollectableGameCollector<?>> collectors = new ArrayList<>(CollectableGameCollectorRegistry.COLLECTOR_REGISTRY.getRegistry().values());
        if (collectors.isEmpty())
            return List.of();

        var option = new OptionData(OptionType.STRING, "collection", "The collection you want to view", true);
        for (var collector : collectors) {
            option.addChoice(collector.getDisplayName(), collector.getName());
        }

        return Collections.singletonList(option);
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.FUN;
    }

    @Override
    public String getDescription() {
        return "View all the collectables you have!";
    }

    @Override
    public String getName() {
        return "collectables";
    }

    @Override
    public String getRichName() {
        return "Collectables";
    }

    @Override
    public boolean isServerOnly() {
        return true;
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            reply(event, "❌ You can only use this command in a server!");
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            reply(event, "❌ An error occurred while trying to get the guild!");
            return;
        }

        event.deferReply().queue();

        UserCollectables userCollectables = Database.getDatabase().userCollectables.find(Filters.eq("user", event.getUser().getIdLong())).first();
        if (userCollectables == null) {
            event.getHook().sendMessage("❌ You do not have any collectables!").queue();
            return;
        }

        String collection = event.getOption("collection", null, OptionMapping::getAsString);
        if (collection == null) {
            event.getHook().sendMessage("❌ You need to specify a collection!").queue();
            return;
        }

        CollectableGameCollector<?> collector = CollectableGameCollectorRegistry.COLLECTOR_REGISTRY.get(collection);
        if (collector == null) {
            event.getHook().sendMessage("❌ That collection does not exist!").queue();
            return;
        }

        UserCollectables.Collectables userCollection = userCollectables.getCollectables(collector);

        Map<CollectableRarity, List<Collectable>> collectables = new HashMap<>();
        Registry<? extends Collectable> registry = collector.getRegistry();
        for (String collectableName : userCollection.getCollectables()) {
            Collectable collectable = registry.get(collectableName);
            if (collectable == null)
                continue;

            collectables.computeIfAbsent(collectable.getRarity(), rarity -> new ArrayList<>()).add(collectable);
        }

        if (collectables.isEmpty()) {
            event.getHook().sendMessage("❌ You do not have any collectables in that collection!").queue();
            return;
        }

        var contents = new PaginatedEmbed.ContentsBuilder();
        for (CollectableRarity rarity : CollectableRarity.values()) {
            List<Collectable> rares = collectables.get(rarity);
            if (rares == null)
                continue;

            List<String> formattedCollectables = new ArrayList<>(rares.size());
            for (Collectable collectable : rares) {
                var formatted = new StringBuilder();
                if (collector.getPresentation() == CollectablePresentation.EMOJI)
                    formatted.append(collectable.getEmoji()).append(" ");

                formatted.append(collectable.getRichName());
                formattedCollectables.add(formatted.toString());
            }

            List<String> fieldValues = createFieldValues(formattedCollectables);
            for (int index = 0; index < fieldValues.size(); index++) {
                String fieldName = rarity.getName() + " (" + rares.size() + ")";
                if (index > 0)
                    fieldName += " (continued)";

                contents.field(fieldName, fieldValues.get(index));
            }
        }

        int highestOrdinal = 0;
        for (var rarity : collectables.keySet()) {
            if (rarity.ordinal() > highestOrdinal)
                highestOrdinal = rarity.ordinal();
        }

        PaginatedEmbed embed = new PaginatedEmbed.Builder(FIELDS_PER_PAGE, contents)
                .title(event.getUser().getEffectiveName() + "'s " + collector.getDisplayName() + " Collection (" + userCollection.getCollectables().size() + ")")
                .timestamp(Instant.now())
                .footer("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                .color(CollectableRarity.values()[highestOrdinal].getColor())
                .authorOnly(event.getUser().getIdLong())
                .build(event.getJDA());

        embed.send(event.getHook());
    }

    static List<String> createFieldValues(List<String> collectables) {
        List<String> fields = new ArrayList<>();
        var current = new StringBuilder();

        for (String collectable : collectables) {
            if (collectable.length() > FIELD_VALUE_MAX_LENGTH) {
                if (!current.isEmpty()) {
                    fields.add(current.toString());
                    current.setLength(0);
                }

                int start = 0;
                while (collectable.length() - start > FIELD_VALUE_MAX_LENGTH) {
                    int end = start + FIELD_VALUE_MAX_LENGTH;
                    if (Character.isHighSurrogate(collectable.charAt(end - 1))
                            && Character.isLowSurrogate(collectable.charAt(end))) {
                        end--;
                    }

                    fields.add(collectable.substring(start, end));
                    start = end;
                }

                current.append(collectable, start, collectable.length());
                continue;
            }

            int requiredLength = collectable.length() + (current.isEmpty() ? 0 : 2);
            if (!current.isEmpty() && current.length() + requiredLength > FIELD_VALUE_MAX_LENGTH) {
                fields.add(current.toString());
                current.setLength(0);
            }

            if (!current.isEmpty())
                current.append(", ");

            current.append(collectable);
        }

        if (!current.isEmpty())
            fields.add(current.toString());

        return fields;
    }
}
