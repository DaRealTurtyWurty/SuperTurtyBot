package dev.darealturtywurty.superturtybot.commands.fun;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserCollectables;
import dev.darealturtywurty.superturtybot.modules.collectable.CollectableRarity;
import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobCollectable;
import dev.darealturtywurty.superturtybot.modules.collectable.minecraft.MinecraftMobRegistry;
import dev.darealturtywurty.superturtybot.registry.Registry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectablesCommand extends CoreCommand {
    public CollectablesCommand() {
        super(new Types(true, false, false, false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "collection", "The collection you want to view", true)
                        .addChoice("Minecraft Mobs", "minecraft_mobs")
        );
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
        if(!event.isFromGuild()) {
            reply(event, "❌ You can only use this command in a server!");
            return;
        }

        Guild guild = event.getGuild();
        if(guild == null) {
            reply(event, "❌ An error occurred while trying to get the guild!");
            return;
        }

        event.deferReply().queue();

        UserCollectables userCollectables = Database.getDatabase().userCollectables.find(Filters.eq("user", event.getUser().getIdLong())).first();
        if(userCollectables == null) {
            event.getHook().sendMessage("❌ You do not have any collectables!").queue();
            return;
        }

        String collection = event.getOption("collection", OptionMapping::getAsString);
        if(collection == null) {
            event.getHook().sendMessage("❌ You need to specify a collection!").queue();
            return;
        }

        if (collection.equals("minecraft_mobs")) {
            Registry<MinecraftMobCollectable> minecraftMobRegistry = MinecraftMobRegistry.MOB_REGISTRY;
            UserCollectables.Collectables minecraftMobs = userCollectables.getCollectables(UserCollectables.CollectionType.MINECRAFT_MOBS);
            var embed = new EmbedBuilder()
                    .setTitle(event.getUser().getEffectiveName() + "'s Minecraft Mob Collection (" + minecraftMobs.getCollectables().size() + ")")
                    .setTimestamp(Instant.now())
                    .setFooter("Requested by " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl());

            Map<CollectableRarity, List<MinecraftMobCollectable>> collectables = new HashMap<>();
            for (String collectable : minecraftMobs.getCollectables()) {
                MinecraftMobCollectable mob = minecraftMobRegistry.getRegistry().get(collectable);
                if (mob == null)
                    continue;

                collectables.computeIfAbsent(mob.getRarity(), rarity -> new ArrayList<>()).add(mob);
            }

            for (var rarity : CollectableRarity.values()) {
                List<MinecraftMobCollectable> mobs = collectables.get(rarity);
                if (mobs == null)
                    continue;

                var builder = new StringBuilder();
                for (MinecraftMobCollectable mob : mobs) {
                    builder.append(mob.getEmoji()).append(" ").append(mob.getRichName()).append(", ");
                }

                embed.addField(rarity.getName() + " (" + mobs.size() + ")", builder.substring(0, builder.length() - 2), false);
            }

            int highestOrdinal = 0;
            for (var rarity : collectables.keySet()) {
                if (rarity.ordinal() > highestOrdinal)
                    highestOrdinal = rarity.ordinal();
            }

            embed.setColor(CollectableRarity.values()[highestOrdinal].getColor());

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } else {
            event.getHook().sendMessage("❌ That collection does not exist!").queue();
        }
    }
}
