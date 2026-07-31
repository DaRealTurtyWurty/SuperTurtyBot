package dev.darealturtywurty.superturtybot.registry;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class Registry<Type extends Registerable> {
    private final Map<String, Type> registerables = new HashMap<>();
    private final Set<Type> registeredObjects = Collections.newSetFromMap(new IdentityHashMap<>());

    @NotNull
    public Map<String, Type> getRegistry() {
        return Map.copyOf(this.registerables);
    }

    public Type get(String name) {
        return this.registerables.get(name);
    }

    public boolean containsKey(String name) {
        return this.registerables.containsKey(name);
    }

    public Collection<Type> values() {
        return Collections.unmodifiableCollection(this.registerables.values());
    }

    public Type register(String name, Type object) {
        if (this.registerables.containsKey(name))
            throw new IllegalStateException("Item with name `" + name + "` has already been registered!");
        if (this.registeredObjects.contains(object))
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
        this.registeredObjects.add(object);
        return object;
    }

    public int size() {
        return this.registerables.size();
    }
}
