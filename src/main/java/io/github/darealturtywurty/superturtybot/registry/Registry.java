package io.github.darealturtywurty.superturtybot.registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.NotNull;

public class Registry<Type extends Registerable> {
    private final Map<String, Type> registerables = new HashMap<>();

    @NotNull
    public Map<String, Type> getRegistry() {
        return Map.copyOf(this.registerables);
    }

    public Entry<String, Type> random() {
        final List<Entry<String, Type>> entries = getRegistry().entrySet().stream().toList();
        return entries.get(ThreadLocalRandom.current().nextInt(entries.size()));
    }
    
    public Type register(String name, Type object) {
        if (this.registerables.containsKey(name))
            throw new IllegalStateException("Item with name `" + name + "` has already been registered!");
        if (this.registerables.containsValue(object))
            throw new IllegalStateException(object + " has already been registered to "
                + this.registerables.get(this.registerables.entrySet().stream()
                    .filter(entry -> entry.getValue() == object).findFirst().get().getKey())
                + "` and cannot be re-registered to another key!");

        object.setName(name);
        this.registerables.put(name, object);
        return object;
    }
}
