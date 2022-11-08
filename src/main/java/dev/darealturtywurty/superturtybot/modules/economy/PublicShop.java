package dev.darealturtywurty.superturtybot.modules.economy;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.ShutdownHooks;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class PublicShop {
    public static final PublicShop INSTANCE = new PublicShop();
    private List<ShopItem> featuredItems;
    private List<ShopItem> newItems;
    private List<ShopItem> discountItems;

    public PublicShop() {
        this.featuredItems = new ArrayList<>();
        this.newItems = new ArrayList<>();
        this.discountItems = new ArrayList<>();
    }

    public void reloadShop() {

    }

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static JsonObject readShutdownVariables() {
        try {
            var toRead = Path.of("../");
            InputStream content = Files.newInputStream(toRead, StandardOpenOption.READ);
            JsonObject json = Constants.GSON.fromJson(new InputStreamReader(content), JsonObject.class);
            content.close();
            return json;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read ");
        }
    }

    private static int getInitialDelay() {
        JsonObject shutdown = readShutdownVariables();
        int timeUntilRefresh = shutdown.get("PublicShopRefreshTime").getAsLong();
    }

    public static void runShop() {
        if (isRunning()) return;

        IS_RUNNING.set(true);


        EXECUTOR.scheduleAtFixedRate(PublicShop.INSTANCE::reloadShop, 0, 24, TimeUnit.HOURS);
        ShutdownHooks.register(() -> {

        });
    }
}
