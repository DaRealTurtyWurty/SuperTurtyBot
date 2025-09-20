package dev.darealturtywurty.superturtybot.commands.fun;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.modules.collectable.Collectable;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollector;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableGameCollectorRegistry;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
import java.util.*;

public class CollectablesCommand extends CoreCommand {
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

        CollectableGameCollector<?> collector = CollectableGameCollectorRegistry.COLLECTOR_REGISTRY.getRegistry().get(collection);
        if (collector == null) {
            event.getHook().sendMessage("❌ That collection does not exist!").queue();
            return;
        }

        UserCollectables.Collectables userCollection = userCollectables.getCollectables(collector);

        var embed = new EmbedBuilder()
                .setTitle(event.getUser().getEffectiveName() + "'s " + collector.getName() + " Collection (" + userCollection.getCollectables().size() + ")")
                .setTimestamp(Instant.now())
                .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

        Map<CollectableRarity, List<Collectable>> collectables = new HashMap<>();
        Registry<? extends Collectable> registry = collector.getRegistry();
        for (String collectableName : userCollection.getCollectables()) {
            Collectable collectable = registry.getRegistry().get(collectableName);
            if (collectable == null)
                continue;

            collectables.computeIfAbsent(collectable.getRarity(), rarity -> new ArrayList<>()).add(collectable);
        }

        for (var rarity : CollectableRarity.values()) {
            List<Collectable> rares = collectables.get(rarity);
            if (rares == null)
                continue;

            var builder = new StringBuilder();
            for (Collectable collectable : rares) {
                builder.append(collectable.getEmoji()).append(" ").append(collectable.getRichName()).append(", ");
            }

            embed.addField(rarity.getName() + " (" + rares.size() + ")", builder.substring(0, builder.length() - 2), false);
        }

        int highestOrdinal = 0;
        for (var rarity : collectables.keySet()) {
            if (rarity.ordinal() > highestOrdinal)
                highestOrdinal = rarity.ordinal();
        }

        embed.setColor(CollectableRarity.values()[highestOrdinal].getColor());
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
