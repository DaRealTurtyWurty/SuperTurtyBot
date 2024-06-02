package dev.darealturtywurty.superturtybot.commands.fun.relationship;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public record FamilyGraph(long guildId, Person rootPerson) {
    public void traverse(Consumer<Person> consumer) {
        Set<Person> visited = new HashSet<>();
        traverse(rootPerson, visited, consumer);
    }

    public void traverse(Person person, Set<Person> visited, Consumer<Person> consumer) {
        if (visited.contains(person))
            return;

        consumer.accept(person);
        visited.add(person);

        // go through all parents, children and partners
        Set<Person> connections = person.getConnections();
        for (Person connection : connections) {
            traverse(connection, visited, consumer);
        }
    }

    public boolean containsPerson(long id) {
        var contains = new AtomicBoolean(false);
        traverse(person -> {
            if (person.getId() == id) {
                contains.set(true);
            }
        });

        return contains.get();
    }
}
