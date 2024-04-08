package dev.darealturtywurty.superturtybot.database.codec;

import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.Inventory;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.Item;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.ItemRegistry;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.ItemStack;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class InventoryCodec implements Codec<Inventory> {
    @Override
    public void encode(BsonWriter writer, Inventory value, EncoderContext encoderContext) {
        writer.writeStartArray();
        for (ItemStack stack : value.getItems()) {
            if (stack == null || stack.isEmpty())
                continue;

            writer.writeStartDocument();
            writer.writeString("item", stack.getItem().getName());
            writer.writeInt32("amount", stack.getAmount());
            writer.writeEndDocument();
        }
        writer.writeEndArray();
    }

    @Override
    public Inventory decode(BsonReader reader, DecoderContext decoderContext) {
        Inventory inventory = new Inventory();
        reader.readStartArray();

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            reader.readStartDocument();
            Item item = ItemRegistry.ITEMS.getValue(reader.readString("item"));
            int amount = reader.readInt32("amount");
            inventory.addItem(new ItemStack(item, amount));
            reader.readEndDocument();
        }

        reader.readEndArray();
        return inventory;
    }

    @Override
    public Class<Inventory> getEncoderClass() {
        return null;
    }
}
