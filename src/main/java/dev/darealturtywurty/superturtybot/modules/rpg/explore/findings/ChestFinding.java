package dev.darealturtywurty.superturtybot.modules.rpg.explore.findings;

import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.RPGPlayer;
import dev.darealturtywurty.superturtybot.modules.rpg.RPGManager;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Finding;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.Outcome;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.Inventory;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.Item;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.ItemRegistry;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.ItemStack;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.response.ResponseBuilder;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.List;

public class ChestFinding extends Finding {
    public ChestFinding() {
        super(Outcome.POSITIVE, "You found a chest! What could be inside?");
    }

    @Override
    public ResponseBuilder getResponse(RPGPlayer player, JDA jda, long guild, long channel) {
        return ResponseBuilder.start(player, jda, guild, channel).first(() -> {
            Inventory inventory = player.getInventory();

            int numberOfItems = MathUtils.weightedRandomInt(1, 10, 50);
            List<ItemStack> toAdd = new ArrayList<>();
            for (int i = 0; i < numberOfItems; i++) {
                Item item = ItemRegistry.pickRandomWeighted();
                int amount = MathUtils.weightedRandomInt(1, 3, 10);
                toAdd.add(new ItemStack(item, amount));
            }

            for (ItemStack stack : toAdd) {
                ItemStack remainder = inventory.addItem(stack);
                System.out.println("Added " + (stack.getAmount() - remainder.getAmount()) + " " + stack.getItem().getName() + " to " + player.getUser());
            }

            RPGManager.updatePlayer(player);
        });
    }
}
