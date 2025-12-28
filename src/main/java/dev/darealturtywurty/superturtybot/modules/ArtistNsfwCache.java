package dev.darealturtywurty.superturtybot.modules;

import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.core.util.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArtistNsfwCache {
    private static final int CACHE_VERSION = 1;
    private static final double SAFE_THRESHOLD = 0.2;
    private static final Path DATASET_ROOT = Path.of("Real_AI_SD_LD_Dataset");
    private static final List<Path> DATASET_SPLITS = List.of(
            DATASET_ROOT.resolve("train"),
            DATASET_ROOT.resolve("test")
    );
    private static final Path CACHE_PATH = DATASET_ROOT.resolve("nsfw_cache.json");
    private static ExecutorService executor;
    private static final AtomicReference<CacheState> STATE = new AtomicReference<>(CacheState.empty());

    private ArtistNsfwCache() {
    }

    public static void initialize() {
        loadCache();
        ensureExecutor();
        CompletableFuture.runAsync(ArtistNsfwCache::refreshCache, executor)
                .exceptionally(error -> {
                    Constants.LOGGER.error("Failed to refresh artist NSFW cache.", error);
                    return null;
                });
    }

    public static Optional<Path> pickRandomSafeImage(boolean isAi) {
        CacheState state = STATE.get();
        List<Path> candidates = isAi ? state.safeAiImages() : state.safeRealImages();
        if (candidates.isEmpty())
            return Optional.empty();

        return Optional.of(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
    }

    public static boolean hasSafeImages(boolean isAi) {
        CacheState state = STATE.get();
        return isAi ? !state.safeAiImages().isEmpty() : !state.safeRealImages().isEmpty();
    }

    public static boolean datasetAvailable() {
        return DATASET_SPLITS.stream().allMatch(Files::exists);
    }

    private static void refreshCache() {
        if (!datasetAvailable()) {
            Constants.LOGGER.warn("Artist dataset not found. Skipping NSFW cache refresh.");
            return;
        }

        CachePayload existing = loadCache();
        Map<String, CacheEntry> existingEntries = existing.entries().stream()
                .collect(Collectors.toMap(CacheEntry::relativePath, Function.identity(), (first, second) -> first));

        List<CacheEntry> refreshedEntries = new ArrayList<>();
        for (Path root : DATASET_SPLITS) {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(ArtistNsfwCache::isSupportedImage)
                        .forEach(path -> refreshedEntries.add(buildEntry(existingEntries, path)));
            } catch (IOException exception) {
                Constants.LOGGER.error("Failed to scan artist dataset at {}.", root, exception);
            }
        }

        scoreUnlabeledEntries(refreshedEntries);

        CachePayload payload = new CachePayload(
                CACHE_VERSION,
                System.currentTimeMillis(),
                refreshedEntries
        );
        saveCache(payload);
        STATE.set(buildState(payload));
        Constants.LOGGER.info("Artist NSFW cache refreshed: {} entries.", refreshedEntries.size());
    }

    private static void scoreUnlabeledEntries(List<CacheEntry> entries) {
        Optional<ArtistNsfwClassifier> optionalClassifier = ArtistNsfwClassifier.create();
        if (optionalClassifier.isEmpty()) {
            Constants.LOGGER.warn("Artist NSFW classifier not available; leaving scores unset.");
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).nsfwScore() == null)
                indices.add(i);
        }

        if (indices.isEmpty())
            return;

        ArtistNsfwClassifier classifier = optionalClassifier.get();
        ExecutorService pool = null;
        try {
            int total = indices.size();
            int threads = resolveWorkerThreads();
            long startTime = System.nanoTime();
            int nextLogPercent = 5;
            pool = Executors.newFixedThreadPool(threads, runnable -> {
                var thread = new Thread(runnable);
                thread.setName("artist-nsfw-worker");
                thread.setDaemon(true);
                return thread;
            });
            CompletionService<ScoreResult> completion = new ExecutorCompletionService<>(pool);
            for (int index : indices) {
                completion.submit(() -> {
                    CacheEntry entry = entries.get(index);
                    OptionalDouble score = classifier.predictScore(DATASET_ROOT.resolve(entry.relativePath()));
                    return new ScoreResult(index, entry, score.isEmpty() ? null : score.getAsDouble());
                });
            }

            int processed = 0;
            for (int i = 0; i < indices.size(); i++) {
                ScoreResult result;
                try {
                    Future<ScoreResult> future = completion.take();
                    result = future.get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException exception) {
                    Constants.LOGGER.warn("Artist NSFW scan task failed.", exception.getCause());
                    processed++;
                    Constants.LOGGER.info("Artist NSFW scan progress: {}/{} (failed)", processed, total);
                    continue;
                }
                processed++;
                if (result.score() != null) {
                    CacheEntry entry = result.entry();
                    entries.set(result.index(), new CacheEntry(entry.relativePath(), entry.isAi(), entry.lastModified(),
                            result.score()));
                }

                if (processed % 1000 == 0) {
                    CachePayload partialPayload = new CachePayload(
                            CACHE_VERSION,
                            System.currentTimeMillis(),
                            entries
                    );
                    saveCache(partialPayload);
                }

                int percent = (int) Math.floor((processed * 100.0) / total);
                if (percent >= nextLogPercent) {
                    String eta = formatEta(startTime, processed, total);
                    Constants.LOGGER.info("Artist NSFW scan progress: {}% ({}/{}) ETA {}", percent, processed, total, eta);
                    nextLogPercent += 5;
                }
            }
        } finally {
            if (pool != null)
                pool.shutdown();
            classifier.close();
        }
    }

    private static CacheEntry buildEntry(Map<String, CacheEntry> existingEntries, Path path) {
        String relativePath = DATASET_ROOT.relativize(path).toString();
        long lastModified = getLastModified(path);
        boolean isAi = isAiPath(path);

        CacheEntry existing = existingEntries.get(relativePath);
        if (existing != null && existing.lastModified() == lastModified && existing.isAi() == isAi)
            return existing;

        return new CacheEntry(relativePath, isAi, lastModified, null);
    }

    private static long getLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static CacheState buildState(CachePayload payload) {
        List<Path> safeAi = new ArrayList<>();
        List<Path> safeReal = new ArrayList<>();

        for (CacheEntry entry : payload.entries()) {
            Double score = entry.nsfwScore();
            if (score == null || score > SAFE_THRESHOLD)
                continue;

            Path resolved = DATASET_ROOT.resolve(entry.relativePath());
            if (entry.isAi()) {
                safeAi.add(resolved);
            } else {
                safeReal.add(resolved);
            }
        }

        return new CacheState(List.copyOf(safeAi), List.copyOf(safeReal), payload.entries().size());
    }

    private static CachePayload loadCache() {
        if (Files.notExists(CACHE_PATH))
            return CachePayload.empty();

        try (BufferedReader reader = Files.newBufferedReader(CACHE_PATH)) {
            CachePayload payload = Constants.GSON.fromJson(reader, CachePayload.class);
            if (payload == null)
                return CachePayload.empty();

            STATE.set(buildState(payload));
            return payload;
        } catch (IOException exception) {
            Constants.LOGGER.warn("Failed to load artist NSFW cache.", exception);
            return CachePayload.empty();
        }
    }

    private static void saveCache(CachePayload payload) {
        try (BufferedWriter writer = Files.newBufferedWriter(CACHE_PATH)) {
            Constants.GSON.toJson(payload, writer);
        } catch (IOException exception) {
            Constants.LOGGER.warn("Failed to save artist NSFW cache.", exception);
        }
    }

    private static boolean isAiPath(Path path) {
        Path parent = path.getParent();
        if (parent == null)
            return false;

        return parent.getFileName().toString().startsWith("AI_");
    }

    private static boolean isSupportedImage(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png");
    }

    private static void ensureExecutor() {
        if (executor != null)
            return;

        executor = Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable);
            thread.setName("artist-nsfw-cache");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static int resolveWorkerThreads() {
        int fallback = Math.max(1, Runtime.getRuntime().availableProcessors());
        return Environment.INSTANCE.artistNsfwThreads()
                .filter(value -> value > 0)
                .orElse(fallback);
    }

    private static String formatEta(long startTimeNanos, int processed, int total) {
        if (processed <= 0)
            return "unknown";

        long elapsedNanos = System.nanoTime() - startTimeNanos;
        double perItemNanos = elapsedNanos / (double) processed;
        long remainingItems = Math.max(0, total - processed);
        long remainingNanos = (long) (perItemNanos * remainingItems);
        long remainingSeconds = Math.max(0, remainingNanos / 1_000_000_000L);
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;
        return "%dm %ds".formatted(minutes, seconds);
    }

    private record CacheState(List<Path> safeAiImages, List<Path> safeRealImages, int totalEntries) {
        private static CacheState empty() {
            return new CacheState(List.of(), List.of(), 0);
        }
    }

    private record CachePayload(int version, long updatedAtEpochMs, List<CacheEntry> entries) {
        private static CachePayload empty() {
            return new CachePayload(CACHE_VERSION, 0L, List.of());
        }
    }

    public record CacheEntry(String relativePath, boolean isAi, long lastModified, Double nsfwScore) {
    }

    private record ScoreResult(int index, CacheEntry entry, Double score) {
    }
}
