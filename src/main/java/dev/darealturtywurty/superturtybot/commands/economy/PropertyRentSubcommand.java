package dev.darealturtywurty.superturtybot.commands.economy;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.StringUtils;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import dev.darealturtywurty.superturtybot.modules.economy.Property;
import dev.darealturtywurty.superturtybot.modules.economy.RenterOffer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PropertyRentSubcommand extends PropertySubcommand {
    private static final String RENTER_NAME_API_URL = "https://websonic.co.uk/names/api.php?type=random&with_surname";

    public PropertyRentSubcommand() {
        super("rent", "View renter offers for your property");
        addOption(OptionType.STRING, "property", "The property to rent out", true, true);
    }

    @Override
    protected void execute(SlashCommandInteractionEvent event, Guild guild, Economy account, GuildData config) {
        String propertyName = event.getOption("property", OptionMapping::getAsString);
        if (propertyName == null) {
            PropertyCommand.hookReply(event, "❌ You must specify a property!");
            return;
        }

        Property property = PropertyCommand.getOwnedProperty(account, propertyName);
        if (property == null) {
            PropertyCommand.hookReply(event, "❌ You do not own that property!");
            return;
        }

        PropertyCommand.normalizeRent(property);
        if (property.getRent() == null || property.getRent().isPaused() || PropertyCommand.calculateRent(property).signum() <= 0) {
            PropertyCommand.hookReply(event, "❌ This property is not available to rent out.");
            return;
        }

        if (property.isRentActive()) {
            PropertyCommand.hookReply(event, "❌ This property is already rented to %s until %s."
                    .formatted(PropertyCommand.formatRenter(property, guild), TimeFormat.RELATIVE.format(property.getRentEndsAt())));
            return;
        }

        boolean updated = false;
        long now = System.currentTimeMillis();
        if (property.getNextBestRenterAt() == 0L) {
            property.setNextBestRenterAt(now + TimeUnit.HOURS.toMillis(24));
            updated = true;
        }

        List<RenterOffer> offers = property.getRenterOffers();
        if (offers == null || offers.isEmpty() || now >= property.getNextBestRenterAt()) {
            offers = generateRenterOffers(property);
            property.setRenterOffers(offers);
            property.setNextBestRenterAt(now + TimeUnit.HOURS.toMillis(24));
            updated = true;
        }

        if (updated) {
            EconomyManager.updateAccount(account);
        }

        var builder = new StringBuilder();
        builder.append("**Renter Offers for ").append(property.getName()).append("**\n");
        for (int i = 0; i < offers.size(); i++) {
            RenterOffer offer = offers.get(i);
            builder.append("`").append(i + 1).append("` ")
                    .append(offer.getName())
                    .append(" — ")
                    .append(StringUtils.numberFormat(offer.getOffer(), config))
                    .append('\n');
        }

        var rerollCost = PropertyCommand.calculateRerollCost(property);
        builder.append("\nOffers refresh ")
                .append(TimeFormat.RELATIVE.format(property.getNextBestRenterAt()))
                .append(".\n");

        builder.append("Re-roll cost: ")
                .append(StringUtils.numberFormat(rerollCost, config))
                .append(". Use `/property rent-reroll`.\n")
                .append("Choose an offer with `/property rent-choose <property> <offer>`.");

        PropertyCommand.hookReply(event, builder.toString());
    }

    static List<RenterOffer> generateRenterOffers(Property property) {
        List<RenterOffer> offers = new ArrayList<>();
        BigInteger baseRent = PropertyCommand.calculateRent(property);
        for (int i = 0; i < 3; i++) {
            int percent = ThreadLocalRandom.current().nextInt(80, 121);
            BigInteger offerAmount = baseRent
                    .multiply(BigInteger.valueOf(percent))
                    .divide(BigInteger.valueOf(100));
            offers.add(new RenterOffer(generateRenterName(), offerAmount));
        }

        return offers;
    }

    static String generateRenterName() {
        String apiName = fetchRemoteRenterName();
        if (apiName != null)
            return apiName;

        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "Renter#" + suffix;
    }

    static String fetchRemoteRenterName() {
        Request request = new Request.Builder()
                .url(RENTER_NAME_API_URL)
                .get()
                .header("User-Agent", "SuperTurtyBot")
                .build();

        try (Response response = Constants.HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful())
                return null;

            ResponseBody body = response.body();
            if (body == null)
                return null;

            String json = body.string();
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            if (object == null || !object.has("status") || !object.has("name"))
                return null;

            if (!"success".equalsIgnoreCase(object.get("status").getAsString()))
                return null;

            String name = object.get("name").getAsString();
            return name == null || name.isBlank() ? null : name.trim();
        } catch (JsonSyntaxException | IllegalStateException | IOException ignored) {
            return null;
        }
    }
}
