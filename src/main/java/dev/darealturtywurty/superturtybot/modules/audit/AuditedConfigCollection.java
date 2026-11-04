package dev.darealturtywurty.superturtybot.modules.audit;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import org.bson.Document;
import org.bson.BsonDocumentWrapper;
import org.bson.conversions.Bson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class AuditedConfigCollection {
    private static final Set<String> WRITES = Set.of("insertOne", "replaceOne", "updateOne", "updateMany", "deleteOne",
        "deleteMany",
        "findOneAndUpdate", "findOneAndReplace", "findOneAndDelete");

    private AuditedConfigCollection() {
    }

    @SuppressWarnings("unchecked")
    public static <T> MongoCollection<T> wrap(MongoCollection<T> delegate) {
        var lock = new Object();
        return wrap(delegate, lock);
    }

    @SuppressWarnings("unchecked")
    private static <T> MongoCollection<T> wrap(MongoCollection<T> delegate, Object lock) {
        return (MongoCollection<T>) Proxy.newProxyInstance(MongoCollection.class.getClassLoader(),
            new Class<?>[]{MongoCollection.class}, (_, method, args) -> {
                if (method.getName().equals("updateOne") || method.getName().equals("updateMany")) {
                    if (args[1] instanceof Bson update) {
                        var encoded = update.toBsonDocument(Document.class, delegate.getCodecRegistry());
                        if (!encoded.containsKey("$rename") && encoded.values().stream()
                            .allMatch(value -> value.isDocument()
                                && value.asDocument().keySet().stream().noneMatch(field -> ConfigHistory
                                    .auditsField(delegate.getNamespace().getCollectionName(), field))))
                            return invoke(delegate, method, args);
                    }
                }
                if (!WRITES.contains(method.getName())) {
                    Object result = invoke(delegate, method, args);
                    return result instanceof MongoCollection<?> collection ? wrap(collection, lock) : result;
                }
                synchronized (lock) {
                    MongoCollection<Document> documents = delegate.withDocumentClass(Document.class);
                    boolean inserting = method.getName().equals("insertOne");
                    Bson filter = inserting ? null : (Bson) args[0];
                    List<Document> before = inserting ? List.of() : documents.find(filter).into(new ArrayList<>());
                    Object result = invoke(delegate, method, args);
                    try {
                        if (inserting) {
                            // POJO insertions need not expose the generated MongoDB ID.
                            if (!delegate.getNamespace().getCollectionName().equals("guildData")) {
                                ConfigHistory.record(delegate.getNamespace().getCollectionName(), null,
                                    toDocument(delegate, args[0]));
                            }
                        } else {
                            for (Document previous : before) {
                                Document current = documents.find(Filters.eq("_id", previous.get("_id"))).first();
                                ConfigHistory.record(delegate.getNamespace().getCollectionName(), previous, current);
                            }
                        }
                    } catch (RuntimeException exception) {
                        Constants.LOGGER.error("Configuration was saved but its history could not be recorded",
                            exception);
                        throw new ConfigHistory.RecordingException(exception);
                    }
                    return result;
                }
            });
    }

    private static <T> Document toDocument(MongoCollection<T> collection, Object value) {
        T record = collection.getDocumentClass().cast(value);
        return Document.parse(new BsonDocumentWrapper<>(record,
            collection.getCodecRegistry().get(collection.getDocumentClass())).toJson());
    }

    private static Object invoke(MongoCollection<?> delegate, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
