package dev.darealturtywurty.superturtybot.modules.privacy;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class OptionalDataCollection {
    private static final Set<String> WRITES = Set.of("insertOne", "insertMany", "replaceOne", "updateOne",
        "updateMany", "findOneAndUpdate", "findOneAndReplace", "bulkWrite");

    private final MongoCollection<?> delegate;
    private final String userField;
    private final boolean profile;
    private volatile Exclusion snapshot;

    private OptionalDataCollection(MongoCollection<?> delegate, String userField) {
        this.delegate = delegate;
        this.userField = userField;
        this.profile = RetentionActivity.isProfileCollection(delegate);
    }

    @SuppressWarnings("unchecked")
    public static <T> MongoCollection<T> wrap(MongoCollection<T> collection, String userField) {
        var boundary = new OptionalDataCollection(collection, userField);
        return (MongoCollection<T>) Proxy.newProxyInstance(MongoCollection.class.getClassLoader(),
            new Class<?>[]{MongoCollection.class},
            (_, method, arguments) -> boundary.coordinate(method, arguments));
    }

    private Object coordinate(Method method, Object[] arguments) {
        String name = method.getName();
        if (!WRITES.contains(name) && !name.equals("find") && !name.equals("countDocuments"))
            return invoke(method, arguments, null);

        Set<Long> owners = boundedOwners(method, arguments);
        if (owners == null)
            return PrivacyPreferences.optionalOperation(() -> invoke(method, arguments, null));
        return PrivacyPreferences.optionalOperation(owners, () -> invoke(method, arguments, owners));
    }

    private Set<Long> boundedFilter(BsonDocument filter) {
        Set<Long> result = null;
        for (var entry : filter.entrySet()) {
            Set<Long> part = null;
            if (entry.getKey().equals(userField)) {
                BsonValue value = entry.getValue();
                if (value.isString() || value.isNumber()) {
                    part = new HashSet<>();
                    addIdentity(part, value);
                } else if (value.isDocument() && value.asDocument().containsKey("$eq")
                    && (value.asDocument().get("$eq").isString() || value.asDocument().get("$eq").isNumber())) {
                    part = new HashSet<>();
                    addIdentity(part, value.asDocument().get("$eq"));
                } else if (value.isDocument() && value.asDocument().containsKey("$in")) {
                    part = new HashSet<>();
                    for (BsonValue identity : value.asDocument().getArray("$in")) {
                        if (!identity.isString() && !identity.isNumber())
                            return null;
                        addIdentity(part, identity);
                    }
                }
            } else if ((entry.getKey().equals("$and") || entry.getKey().equals("$or"))
                && entry.getValue().isArray()) {
                boolean union = entry.getKey().equals("$or");
                for (BsonValue clause : entry.getValue().asArray()) {
                    Set<Long> nested = clause.isDocument() ? boundedFilter(clause.asDocument()) : null;
                    if (union && nested == null) {
                        part = null;
                        break;
                    }
                    if (nested != null) {
                        if (part == null) {
                            part = new HashSet<>();
                        }
                        part.addAll(nested);
                    }
                }
            }
            if (part != null) {
                if (result == null) {
                    result = new HashSet<>();
                }
                result.addAll(part);
            }
        }
        return result;
    }

    private Set<Long> boundedOwners(Method method, Object[] supplied) {
        Object[] arguments = supplied == null ? new Object[0] : supplied;
        String name = method.getName();
        int index = arguments.length > 0 && arguments[0] instanceof ClientSession ? 1 : 0;
        Set<Long> owners;
        if (name.equals("insertOne") || name.equals("insertMany")) {
            owners = new HashSet<>();
            collectRecord(owners, arguments[index]);
            return owners.isEmpty() ? null : owners;
        }
        if (arguments.length <= index || !(arguments[index] instanceof Bson filter))
            return null;
        owners = boundedFilter(filter.toBsonDocument(Document.class, delegate.getCodecRegistry()));
        if (owners == null)
            return null;
        if (name.equals("replaceOne") || name.equals("findOneAndReplace")) {
            collectRecord(owners, arguments[index + 1]);
        } else if (WRITES.contains(name)) {
            for (int position = index + 1; position < arguments.length; position++) {
                Object update = arguments[position];
                if (update instanceof Bson bson) {
                    collectFilter(owners, bson.toBsonDocument(Document.class, delegate.getCodecRegistry()));
                } else if (update instanceof List<?> pipeline) {
                    pipeline.stream().filter(Bson.class::isInstance).map(Bson.class::cast)
                        .forEach(stage -> collectFilter(owners,
                            stage.toBsonDocument(Document.class, delegate.getCodecRegistry())));
                }
            }
        }
        return owners;
    }

    private Object invoke(Method method, Object[] supplied, Set<Long> owners) {
        Object[] arguments = supplied == null ? new Object[0] : supplied.clone();
        String name = method.getName();
        boolean writing = WRITES.contains(name);
        if (writing) {
            for (Object argument : arguments) {
                if (argument instanceof Bson bson) {
                    checkFilter(bson.toBsonDocument(Document.class, delegate.getCodecRegistry()));
                } else if (argument instanceof List<?> pipeline) {
                    pipeline.stream().filter(Bson.class::isInstance).map(Bson.class::cast)
                        .forEach(
                            stage -> checkFilter(stage.toBsonDocument(Document.class, delegate.getCodecRegistry())));
                }
            }
            if (name.equals("insertOne") || name.equals("insertMany") || name.equals("replaceOne")
                || name.equals("findOneAndReplace")) {
                int index = arguments.length > 0 && arguments[0] instanceof ClientSession ? 1 : 0;
                if (name.equals("replaceOne") || name.equals("findOneAndReplace")) {
                    index++;
                }

                checkRecord(arguments[index]);
            }

            if (name.equals("bulkWrite") && (!PrivacyPreferences.excludedUsers().isEmpty()
                || RetentionActivity.isProfileCollection(delegate) && !RetentionActivity.excludedUsers().isEmpty()))
                throw new IllegalStateException("Optional-data bulk writes require per-account privacy checks");
        }

        if (writing || name.equals("find") || name.equals("countDocuments")) {
            Bson exclusion;
            if (owners == null) {
                exclusion = exclusion();
            } else {
                List<Object> blocked = new ArrayList<>();
                for (long owner : owners) {
                    if (PrivacyPreferences.isOptedOut(owner) || profile && RetentionActivity.isExpired(owner)) {
                        blocked.add(owner);
                        blocked.add(Long.toString(owner));
                    }
                }
                exclusion = blocked.isEmpty() ? null : Filters.nin(userField, blocked);
            }
            if (exclusion != null) {
                Class<?>[] types = method.getParameterTypes();
                boolean hasFilter = false;
                for (int i = 0; i < types.length; i++) {
                    if (types[i] == Bson.class) {
                        arguments[i] = Filters.and((Bson) arguments[i], exclusion);
                        hasFilter = true;
                        break;
                    }
                }

                if (!hasFilter && (name.equals("find") || name.equals("countDocuments"))) {
                    int index = types.length > 0 && types[0] == ClientSession.class ? 1 : 0;
                    Object[] expanded = new Object[arguments.length + 1];
                    Class<?>[] expandedTypes = new Class<?>[types.length + 1];
                    System.arraycopy(arguments, 0, expanded, 0, index);
                    System.arraycopy(types, 0, expandedTypes, 0, index);
                    expanded[index] = exclusion;
                    expandedTypes[index] = Bson.class;
                    System.arraycopy(arguments, index, expanded, index + 1, arguments.length - index);
                    System.arraycopy(types, index, expandedTypes, index + 1, types.length - index);
                    try {
                        method = MongoCollection.class.getMethod(name, expandedTypes);
                        arguments = expanded;
                    } catch (NoSuchMethodException exception) {
                        throw new IllegalStateException("Unsupported optional-data query overload", exception);
                    }
                }
            }
        }

        try {
            Object result = method.invoke(delegate, arguments);
            if (result instanceof MongoCollection<?> collection)
                return wrap(collection, userField);

            return result;
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException failure)
                throw failure;

            if (exception.getCause() instanceof Error failure)
                throw failure;

            throw new IllegalStateException(exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Bson exclusion() {
        List<Long> optedOut = PrivacyPreferences.excludedUsers();
        Set<Long> expired = profile ? RetentionActivity.excludedUsers() : Set.of();
        Exclusion current = snapshot;
        if (current == null || current.optedOut() != optedOut || current.expired() != expired) {
            Set<Long> users = new HashSet<>(optedOut);
            users.addAll(expired);
            List<Object> values = new ArrayList<>(users.size() * 2);
            for (long id : users) {
                values.add(id);
                values.add(Long.toString(id));
            }
            current = new Exclusion(optedOut, expired, values.isEmpty() ? null : Filters.nin(userField, values));
            snapshot = current;
        }
        return current.filter();
    }

    private record Exclusion(List<Long> optedOut, Set<Long> expired, Bson filter) {
    }

    private void checkRecord(Object record) {
        Set<Long> owners = new HashSet<>();
        collectRecord(owners, record);
        owners.forEach(this::checkIdentity);
    }

    private void collectRecord(Set<Long> owners, Object record) {
        if (record instanceof List<?> records) {
            records.forEach(value -> collectRecord(owners, value));
            return;
        }

        Object value = record;
        for (String part : userField.split("\\.")) {
            if (value == null)
                return;

            if (value instanceof Document document) {
                value = document.get(part);
            } else {
                try {
                    try {
                        value = value.getClass()
                            .getMethod("get" + Character.toUpperCase(part.charAt(0)) + part.substring(1))
                            .invoke(value);
                    } catch (NoSuchMethodException exception) {
                        value = value.getClass().getField(part).get(value);
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Cannot determine the owner of optional data", exception);
                }
            }
        }

        addIdentity(owners, value);
    }

    private void checkFilter(BsonDocument filter) {
        Set<Long> owners = new HashSet<>();
        collectFilter(owners, filter);
        owners.forEach(this::checkIdentity);
    }

    private void collectFilter(Set<Long> owners, BsonDocument filter) {
        for (var entry : filter.entrySet()) {
            if (entry.getKey().equals(userField)) {
                BsonValue value = entry.getValue();
                if (value.isDocument()) {
                    BsonDocument condition = value.asDocument();
                    if (condition.containsKey("$eq")) {
                        addIdentity(owners, condition.get("$eq"));
                    }

                    if (condition.containsKey("$in")) {
                        condition.getArray("$in").forEach(identity -> addIdentity(owners, identity));
                    }
                } else {
                    addIdentity(owners, value);
                }
            } else if (Set.of("$and", "$or", "$set", "$setOnInsert").contains(entry.getKey())) {
                BsonValue value = entry.getValue();
                if (value.isDocument()) {
                    collectFilter(owners, value.asDocument());
                } else if (value.isArray()) {
                    value.asArray().stream()
                        .filter(BsonValue::isDocument)
                        .forEach(part -> collectFilter(owners, part.asDocument()));
                }
            }
        }
    }

    private void checkIdentity(long userId) {
        PrivacyPreferences.requireOptionalData(userId);
        RetentionActivity.register(delegate, userId);
    }

    private static void addIdentity(Set<Long> owners, Object value) {
        if (value instanceof BsonValue bson) {
            if (bson.isString()) {
                value = bson.asString().getValue();
            } else if (bson.isNumber()) {
                value = bson.asNumber().longValue();
            } else
                return;
        }

        if (value == null)
            return;

        try {
            long userId = Long.parseLong(value.toString());
            owners.add(userId);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid optional-data owner", exception);
        }
    }
}
