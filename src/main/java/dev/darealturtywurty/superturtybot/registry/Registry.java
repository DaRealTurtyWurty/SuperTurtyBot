package dev.darealturtywurty.superturtybot.registry;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Registry<Type extends Registerable> {
    private final Map<String, Type> registerables = new HashMap<>();

    @NotNull
    public Map<String, Type> getRegistry() {
        return Map.copyOf(this.registerables);
    }

    public Type register(String name, Type object) {
        if (this.registerables.containsKey(name))
            throw new IllegalStateException("Item with name `" + name + "` has already been registered!");
        if (this.registerables.containsValue(object))
            throw new IllegalStateException(object + " has already been registered to "
                    + this.registerables.get(this.registerables.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() == object)
                    .findFirst()
                    .orElseThrow()
                    .getKey())
                    + "` and cannot be re-registered to another key!");

        object.setName(name);
        this.registerables.put(name, object);
        return object;
    }
}
