package dev.darealturtywurty.superturtybot.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;
import java.util.function.BiConsumer;

public class DatabaseConverter {
    public static final String CONNECTION_STING = "mongodb+srv://TurtyBot:Np6Rp3Lbl4kZP9cF@turtybot.omb6j.mongodb.net/myFirstDatabase?retryWrites=true&w=majority";

    public static void main(String[] args) {
        System.out.println("Hello world!");

        final CodecRegistry pojoRegistry = CodecRegistries
                .fromProviders(PojoCodecProvider.builder().automatic(true).build());
        final CodecRegistry codecRegistry = CodecRegistries
                .fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoRegistry);

        final MongoClient client = connect(codecRegistry);
        final MongoDatabase database = client.getDatabase("TurtyBot");
        upgradeData(database);

        client.close();
        System.out.println("data types converted");
    }

    private static void upgradeData(MongoDatabase database) {
        BiConsumer<MongoCollection<Document>, Document> replace = (collection, document) -> collection.replaceOne(Filters.eq("_id", document.get("_id")), document);

        MongoCollection<Document> guildData = database.getCollection("guildData");
        for (Document document : guildData.find()) {
            if(numberToStringIfExists(document, "defaultEconomyBalance")) {
                replace.accept(guildData, document);
            }
        }

        System.out.println("Guild data done");

        MongoCollection<Document> levelling = database.getCollection("levelling");
        for (Document document : levelling.find()) {
            if (document.containsKey("rankCard")) {
                if (document.get("rankCard") instanceof Document rankCard) {
                    List<String> colorFields = List.of(
                            "backgroundColor",
                            "outlineColor",
                            "rankTextColor",
                            "levelTextColor",
                            "xpOutlineColor",
                            "xpEmptyColor",
                            "xpFillColor",
                            "avatarOutlineColor",
                            "percentTextColor",
                            "xpTextColor",
                            "nameTextColor");
                    boolean changed = false;
                    for (String colorField : colorFields) {
                        if (rankCard.get(colorField) instanceof Document colorDocument) {
                            double alpha = colorDocument.get("alpha", Double.class);
                            double red = colorDocument.get("red", Double.class);
                            double green = colorDocument.get("green", Double.class);
                            double blue = colorDocument.get("blue", Double.class);
                            int colorAsInt = (int) (((Math.round(alpha * 255)) << 24) | ((Math.round(red * 255)) << 16) | ((Math.round(green * 255)) << 8) | Math.round(blue * 255));
                            rankCard.put(colorField, colorAsInt);
                            changed = true;
                        }
                    }

                    if (changed) {
                        replace.accept(levelling, document);
                    }
                }
            }
        }

        System.out.println("Levelling done");

        MongoCollection<Document> steamNotifier = database.getCollection("steamNotifier");
        for (Document document : steamNotifier.find()) {
            if (document.get("previousData") instanceof Document previousData) {
                previousData.remove("additionalProperties");
                Object externalUrl = previousData.get("externalUrl");
                previousData.remove("externalUrl");
                previousData.put("is_external_url", externalUrl);

                replace.accept(steamNotifier, document);
            }
        }

        System.out.println("Steam notifier done");

        MongoCollection<Document> economy = database.getCollection("economy");
        economy.deleteMany(Filters.not(Filters.eq("guild", 1017111640023506974L)));

        System.out.println("Economy deletion done");

        for (Document document : economy.find()) {
            boolean changed = false;
            for (String fieldName : List.of("wallet", "bank", "totalBetWin", "totalBetLoss")) {
                if (numberToStringIfExists(document, fieldName)) {
                    changed = true;
                }
            }

            for (String fieldName : List.of("totalBetWin", "totalBetLoss")) {
                if (document.get(fieldName) instanceof String fieldAsString && fieldAsString.charAt(0) == '-') {
                    document.put(fieldName, fieldAsString.substring(1));
                }
            }

            for (Document loan : getList(document, "loans", Document.class)) {
                if (numberToStringIfExists(loan, "amount")) {
                    changed = true;
                }

                if(numberToStringIfExists(loan, "amountPaid")) {
                    changed = true;
                }

                if (loan.get("interestRate") instanceof Double interestRate) {
                    loan.put("interestRate", interestRate.toString());
                    changed = true;
                }

                for (Document payment : getList(loan, "payments", Document.class)) {
                    if(numberToStringIfExists(payment, "amount")) {
                        changed = true;
                    }
                }
            }

            for (Document transaction : getList(document, "transactions", Document.class)) {
                if(numberToStringIfExists(transaction, "amount")) {
                    changed = true;
                }
            }

            for (Document shopItem : getList(document, "shopItems", Document.class)) {
                if(numberToStringIfExists(shopItem, "originalPrice")) {
                    changed = true;
                }

                if(numberToStringIfExists(shopItem, "price")) {
                    changed = true;
                }
            }

            if (changed) {
                replace.accept(economy, document);
            }
        }

        System.out.println("Economy done");
    }

    private static <T> List<T> getList(Document document, String fieldName, Class<T> clazz) {
        List<T> list = document.getList(fieldName, clazz);
        return list == null ? List.of() : list;
    }

    private static boolean numberToStringIfExists(Document document, String fieldName) {
        if (document.get(fieldName) instanceof Number fieldAsLong) {
            document.remove(fieldName);
            document.put(fieldName, fieldAsLong.toString());
            return true;
        }

        return false;
    }

    private static MongoClient connect(CodecRegistry codec) {
        final ConnectionString connectionString = new ConnectionString(CONNECTION_STING);
        final MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
                .applicationName("TurtyBot").codecRegistry(codec).build();
        return MongoClients.create(settings);
    }
}
