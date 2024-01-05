package dev.darealturtywurty.superturtybot.commands.minigames.utility;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Optional;

public class AmazonUtility {
    private static final String RANDOM_UPC_URL = "https://www.upcdatabase.com/random_item.asp";
    private static final String AMAZON_CATALOG_SEARCH_URL = "https://sellingpartnerapi-na.amazon.com/catalog/2022-04-01/items?keywords=%s&marketplaceIds=EN&pageSize=1";

    public static UPCInformation getRandomUPC() {
        Request request = new Request.Builder().url(RANDOM_UPC_URL).build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new IllegalStateException("Unable to get random UPC!");

            ResponseBody body = response.body();
            if (body == null)
                throw new IllegalStateException("Unable to get random UPC!");

            Document document = Jsoup.parse(body.string());

            Element dataTable = document.selectFirst("#content > .data > tbody");
            if (dataTable == null)
                throw new IllegalStateException("Unable to get random UPC!");

            int nextIndex = 0;

            Optional<String> upcE = Optional.empty();
            if (dataTable.child(nextIndex).child(0).text().equalsIgnoreCase("UPC-E")) {
                upcE = Optional.of(dataTable.child(nextIndex++).selectFirst("img").attr("alt"));
            }

            Optional<String> upcA = Optional.empty();
            if (dataTable.child(nextIndex).child(0).text().equalsIgnoreCase("UPC-A")) {
                upcA = Optional.of(dataTable.child(nextIndex++).selectFirst("img").attr("alt"));
            }

            Optional<String> ucc13 = Optional.empty();
            if (dataTable.child(nextIndex).child(0).text().equalsIgnoreCase("EAN/UCC-13")) {
                ucc13 = Optional.of(dataTable.child(nextIndex++).selectFirst("img").attr("alt"));
            }

            String productTitle = dataTable.child(nextIndex++).child(2).text();

            var details = new StringBuilder();
            Element detailsElem;
            while (!(detailsElem = dataTable.child(nextIndex)).selectFirst("td").text()
                    .equalsIgnoreCase("Issuing Country")) {
                details.append(detailsElem.selectFirst("td").text()).append(":")
                        .append(detailsElem.select("td").get(2).text()).append("\n");
                nextIndex++;
            }

            String issuingCountry = dataTable.child(nextIndex++).child(2).text();
            String lastModifiedDate = dataTable.child(nextIndex++).child(2).text();
            Optional<String> lastModifiedBy = Optional.empty();
            if (dataTable.child(nextIndex).child(0).text().equalsIgnoreCase("Last Modified By")) {
                lastModifiedBy = Optional.of(dataTable.child(nextIndex++).child(2).text());
            }

            int pendingRequests = Integer.parseInt(dataTable.child(nextIndex).child(2).text());

            return new UPCInformation(upcE, upcA, ucc13, productTitle,
                    details.isEmpty() ? Optional.empty() : Optional.of(details.toString()), issuingCountry,
                    lastModifiedDate, lastModifiedBy, pendingRequests);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to get random UPC!", exception);
        }
    }

    public static JsonObject getAmazonItemInfo(UPCInformation upcInformation) {
        String keywords = upcInformation.productTitle().replace(" ", ",");
        var request = new Request.Builder().url(AMAZON_CATALOG_SEARCH_URL.formatted(keywords)).build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Unable to get Amazon item info!");
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Unable to get Amazon item info!");
            }

            return Constants.GSON.fromJson(body.string(), JsonObject.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to get Amazon item info!", exception);
        }
    }

    public static AmazonItem getRandomItem() {
        var upcInformation = getRandomUPC();
        JsonObject itemInfo = getAmazonItemInfo(upcInformation);

        System.out.println(itemInfo);

        return new AmazonItem(upcInformation);
    }

    public record AmazonItem(UPCInformation upcInformation) {
    }

    public record UPCInformation(Optional<String> upcE, Optional<String> upcA, Optional<String> ucc13,
                                 String productTitle, Optional<String> details, String issuingCountry,
                                 String lastModifiedDate, Optional<String> lastModifiedBy, int pendingRequests) {

    }
}
