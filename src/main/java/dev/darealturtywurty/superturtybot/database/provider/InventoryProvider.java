package dev.darealturtywurty.superturtybot.database.provider;

import dev.darealturtywurty.superturtybot.database.codec.InventoryCodec;
import dev.darealturtywurty.superturtybot.modules.rpg.explore.item.Inventory;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class InventoryProvider implements CodecProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(Inventory.class)) {
            return (Codec<T>) new InventoryCodec();
        }

        return null;
    }

    @Override
    public String toString() {
        return "InventoryProvider{}";
    }
}
