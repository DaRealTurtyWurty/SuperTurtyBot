package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.Data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class PublicShop {
    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

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
        this.featuredItems.addAll(List.of(ShopItemRegistry.APPLE, ShopItemRegistry.ORANGE));
        this.newItems.add(ShopItemRegistry.BANANA);
        this.discountItems.add(ShopItemRegistry.CHERRY);
    }

    public boolean isEmpty() {
        return this.featuredItems.isEmpty() && this.newItems.isEmpty() && this.discountItems.isEmpty();
    }

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void run() {
        if (isRunning()) return;

        IS_RUNNING.set(true);

        // every midnight reload the shop
        INSTANCE.reloadShop();
        EXECUTOR.scheduleAtFixedRate(INSTANCE::reloadShop, getInitialDelay(), 24, TimeUnit.HOURS);
    }

    private static long getInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = (Calendar) now.clone();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        if (now.after(midnight)) {
            midnight.add(Calendar.DAY_OF_MONTH, 1);
        }

        return midnight.getTimeInMillis() - now.getTimeInMillis();
    }
}
