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

    private OptionalDataCollection(MongoCollection<?> delegate, String userField) {
        this.delegate = delegate;
        this.userField = userField;
    }

    @SuppressWarnings("unchecked")
    public static <T> MongoCollection<T> wrap(MongoCollection<T> collection, String userField) {
        var boundary = new OptionalDataCollection(collection, userField);
        return (MongoCollection<T>) Proxy.newProxyInstance(MongoCollection.class.getClassLoader(),
            new Class<?>[]{MongoCollection.class},
            (_, method, arguments) -> PrivacyPreferences.optionalOperation(
                () -> RetentionActivity.mutation(() -> boundary.invoke(method, arguments))));
    }

    private Object invoke(Method method, Object[] supplied) {
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

        List<Object> excluded = new ArrayList<>();
        Set<Long> excludedUsers = new HashSet<>(PrivacyPreferences.excludedUsers());
        if (RetentionActivity.isProfileCollection(delegate)) {
            excludedUsers.addAll(RetentionActivity.excludedUsers());
        }
        for (long id : excludedUsers) {
            excluded.add(id);
            excluded.add(Long.toString(id));
        }

        if (!excluded.isEmpty() && (writing || name.equals("find") || name.equals("countDocuments"))) {
            Bson exclusion = Filters.nin(userField, excluded);
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

    private void checkRecord(Object record) {
        if (record instanceof List<?> records) {
            records.forEach(this::checkRecord);
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

        checkIdentity(value);
    }

    private void checkFilter(BsonDocument filter) {
        for (var entry : filter.entrySet()) {
            if (entry.getKey().equals(userField)) {
                BsonValue value = entry.getValue();
                if (value.isDocument()) {
                    BsonDocument condition = value.asDocument();
                    if (condition.containsKey("$eq")) {
                        checkIdentity(condition.get("$eq"));
                    }

                    if (condition.containsKey("$in")) {
                        condition.getArray("$in").forEach(this::checkIdentity);
                    }
                } else {
                    checkIdentity(value);
                }
            } else if (Set.of("$and", "$or", "$set", "$setOnInsert").contains(entry.getKey())) {
                BsonValue value = entry.getValue();
                if (value.isDocument()) {
                    checkFilter(value.asDocument());
                } else if (value.isArray()) {
                    value.asArray().stream()
                        .filter(BsonValue::isDocument)
                        .forEach(part -> checkFilter(part.asDocument()));
                }
            }
        }
    }

    private void checkIdentity(Object value) {
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
            PrivacyPreferences.requireOptionalData(userId);
            RetentionActivity.register(delegate, userId);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid optional-data owner", exception);
        }
    }
}
