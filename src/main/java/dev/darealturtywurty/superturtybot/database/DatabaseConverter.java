package dev.darealturtywurty.superturtybot.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import org.bson.Document;

import java.util.List;
import java.util.function.BiConsumer;

public class DatabaseConverter {
    public static void main(String[] args) {
        MongoDatabase database = Database.getDatabase().mongoDatabase;
        BiConsumer<MongoCollection<Document>, Document> replace = (collection, document) ->
                collection.replaceOne(new Document("_id", document.get("_id")), document);

        MongoCollection<Document> guildData = database.getCollection("guildData");
        for (Document document : guildData.find()) {
            longToStringIfExists(document, "defaultEconomyBalance");
            replace.accept(guildData, document);
        }

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
                    for (String colorField : colorFields) {
                        if (rankCard.get(colorField) instanceof Document colorDocument) {
                            double alpha = colorDocument.get("alpha", Double.class);
                            double red = colorDocument.get("red", Double.class);
                            double green = colorDocument.get("green", Double.class);
                            double blue = colorDocument.get("blue", Double.class);
                            int colorAsInt = (int) (((Math.round(alpha * 255)) << 24) | ((Math.round(red * 255)) << 16) | ((Math.round(green * 255)) << 8) | ((Math.round(blue * 255)) << 0));
                            rankCard.put(colorField, colorAsInt);
                        }
                    }
                }
            }
            replace.accept(levelling, document);
        }

        MongoCollection<Document> steamNotifier = database.getCollection("steamNotifier");
        for (Document document : steamNotifier.find()) {
            if (document.get("previousData") instanceof Document previousData) {
                previousData.remove("additionalProperties");
                Object externalUrl = previousData.get("externalUrl");
                previousData.remove("externalUrl");
                previousData.put("is_external_url", externalUrl);
            }
            replace.accept(steamNotifier, document);
        }

        MongoCollection<Document> economy = database.getCollection("economy");
        for (Document document : economy.find()) {
            for (String fieldName : List.of("wallet", "bank", "totalBetWin", "totalBetLoss")) {
                longToStringIfExists(document, fieldName);
            }

            for (Document loan : document.getList("loans", Document.class)) {
                longToStringIfExists(loan, "amount");
                longToStringIfExists(loan, "amountPaid");
                if (loan.get("interestRate") instanceof Double interestRate) {
                    loan.put("interestRate", interestRate.toString());
                }

                for (Document payment : loan.getList("payments", Document.class)) {
                    longToStringIfExists(payment, "amount");
                }
            }

            for (Document transaction : document.getList("transactions", Document.class)) {
                longToStringIfExists(transaction, "amount");
            }

            for (Document shopItem : document.getList("shopItems", Document.class)) {
                longToStringIfExists(shopItem, "originalPrice");
                longToStringIfExists(shopItem, "price");
            }

            for (Document property : document.getList("properties", Document.class)) {
                longToStringIfExists(property, "originalPrice");
                longToStringIfExists(property, "estateTax");
                if (property.get("mortgage") instanceof Document mortgage) {
                    longToStringIfExists(mortgage, "amount");
                    longToStringIfExists(mortgage, "amountPaid");
                    if (mortgage.get("interestRate") instanceof Double interestRate) {
                        mortgage.put("interestRate", interestRate.toString());
                    }

                    for (Document payment : mortgage.getList("payments", Document.class)) {
                        longToStringIfExists(payment, "amount");
                    }
                }

                List<Long> upgradePrices = property.getList("upgradePrices", Long.class);
                property.put("upgradePrices", upgradePrices.stream().map(Object::toString));

                if (property.get("rent") instanceof Document rent) {
                    longToStringIfExists(rent, "baseRent");
                    longToStringIfExists(rent, "currentRent");
                    List<Long> previousRents = property.getList("previousRents", Long.class);
                    property.put("previousRents", previousRents.stream().map(Object::toString));
                }
            }

            replace.accept(economy, document);
        }
    }

    private static void longToStringIfExists(Document document, String fieldName) {
        if (document.get(fieldName) instanceof Long fieldAsLong) {
            document.put(fieldName, fieldAsLong.toString());
        }
    }
}
