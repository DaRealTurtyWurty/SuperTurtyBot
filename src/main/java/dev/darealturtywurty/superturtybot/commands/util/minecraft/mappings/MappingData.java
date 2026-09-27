package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MappingTreeView.ElementMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MemberMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;

import java.util.*;

public record MappingData(MappingTreeView tree, String version, String side, Map<MappingChannel, String> builds) {
    public MappingData {
        var copy = new EnumMap<MappingChannel, String>(MappingChannel.class);
        copy.putAll(builds);
        builds = Collections.unmodifiableMap(copy);
    }

    private static void addResult(List<Result> results, ElementMappingView element, String owner,
                                  String query, int namespace) {
        String name = element.getName(namespace);
        if (name == null)
            return;

        boolean member = element instanceof MemberMappingView;
        String qualified = member ? owner + "#" + name : name;
        String simpleOwner = owner.substring(owner.lastIndexOf('/') + 1);
        String simple = member ? simpleOwner + "#" + name : simpleOwner;
        String descriptor = element instanceof MemberMappingView mapping ? Objects.toString(mapping.getDesc(namespace), "") : "";
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int rank;
        if (query.equals(name) || query.equals(qualified) || query.equals(simple)
                || query.equals(qualified + descriptor) || query.equals(simple + descriptor)
                || query.equals(name + descriptor)) {
            rank = 0;
        } else if (name.equalsIgnoreCase(query) || qualified.equalsIgnoreCase(query) || simple.equalsIgnoreCase(query)) {
            rank = 1;
        } else if (qualified.toLowerCase(Locale.ROOT).contains(lowerQuery)
                || simple.toLowerCase(Locale.ROOT).contains(lowerQuery)
                || (qualified + descriptor).toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            rank = 2;
        } else {
            return;
        }

        results.add(new Result(element, rank));
    }

    private static String normalize(String query) {
        int signature = query.indexOf('(');
        String name = signature < 0 ? query : query.substring(0, signature);
        return name.replace('.', '/') + (signature < 0 ? "" : query.substring(signature).replace('.', '/'));
    }

    public List<Result> search(String query, MappingChannel channel, boolean classes, boolean methods, boolean fields) {
        int namespace = namespace(channel);
        String normalized = normalize(query.trim());
        if (normalized.isEmpty())
            throw new IllegalArgumentException("Please provide a class, method, or field name.");

        var results = new ArrayList<Result>();
        for (var owner : this.tree.getClasses()) {
            String ownerName = owner.getName(namespace);
            if (ownerName == null)
                continue;

            if (classes) {
                addResult(results, owner, ownerName, normalized, namespace);
            }

            if (methods) {
                for (var method : owner.getMethods()) {
                    addResult(results, method, ownerName, normalized, namespace);
                }
            }

            if (fields) {
                for (var field : owner.getFields()) {
                    addResult(results, field, ownerName, normalized, namespace);
                }
            }
        }

        results.sort(Comparator.comparingInt(Result::rank).thenComparing(result -> result.format(namespace)));
        return results;
    }

    public List<String> suggest(String query, MappingChannel channel, boolean classes, boolean methods, boolean fields) {
        int namespace = namespace(channel);
        var suggestions = new TreeSet<String>();
        for (Result result : search(query.isBlank() ? "net.minecraft" : query, channel, classes, methods, fields)) {
            String value = result.qualifiedName(namespace);
            if (result.element() instanceof MethodMappingView method) {
                value += method.getDesc(namespace);
            }

            if (value.length() <= 100) {
                suggestions.add(value);
            }
            if (suggestions.size() == 25)
                break;
        }

        return List.copyOf(suggestions);
    }

    public int namespace(MappingChannel channel) {
        int namespace = this.tree.getNamespaceId(channel.namespace());
        if (namespace == MappingTreeView.NULL_NAMESPACE_ID)
            throw new IllegalArgumentException(channel.displayName() + " mappings are not available for this version.");

        return namespace;
    }

    public record Result(ElementMappingView element, int rank) {
        public boolean hasName(int namespace) {
            return this.element.getName(namespace) != null
                    && (!(this.element instanceof MemberMappingView member) || member.getOwner().getName(namespace) != null);
        }

        public String qualifiedName(int namespace) {
            if (this.element instanceof MemberMappingView member)
                return member.getOwner().getName(namespace).replace('/', '.') + "#" + member.getName(namespace);

            return this.element.getName(namespace).replace('/', '.');
        }

        public String format(int namespace) {
            String kind = this.element instanceof MethodMappingView ? "Method"
                    : this.element instanceof MemberMappingView ? "Field" : "Class";
            String descriptor = this.element instanceof MemberMappingView member && member.getDesc(namespace) != null
                    ? " " + member.getDesc(namespace) : "";
            return kind + ": " + qualifiedName(namespace) + descriptor;
        }
    }
}
