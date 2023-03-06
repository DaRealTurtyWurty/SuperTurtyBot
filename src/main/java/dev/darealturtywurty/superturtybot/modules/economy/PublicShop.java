package dev.darealturtywurty.superturtybot.modules.economy;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        this.featuredItems.addAll(List.of(ShopItemRegistry.APPLE, ShopItemRegistry.ORANGE));
        this.newItems.add(ShopItemRegistry.BANANA);
        this.discountItems.add(ShopItemRegistry.CHERRY);
    }

    public boolean isEmpty() {
        return this.featuredItems.isEmpty() && this.newItems.isEmpty() && this.discountItems.isEmpty();
    }

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static boolean isRunning() {
        return IS_RUNNING.get();
    }

    public static void run() {
        if (isRunning()) return;

        IS_RUNNING.set(true);

        EXECUTOR.execute(() -> {
            while(true) {
                LocalDateTime time = LocalDateTime.now();
                if((time.getHour() == 12 && time.getSecond() == 0 && time.getMinute() == 0 && time.getNano() < 5) || PublicShop.INSTANCE.isEmpty()) {
                    PublicShop.INSTANCE.reloadShop();
                }
            }
        });
    }
}
