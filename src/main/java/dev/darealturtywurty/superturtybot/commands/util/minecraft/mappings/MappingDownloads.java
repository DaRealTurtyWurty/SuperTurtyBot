package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

public final class MappingDownloads {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private MappingDownloads() {
    }

    public static JsonElement json(String url) throws IOException {
        return JsonParser.parseString(new String(download(url), StandardCharsets.UTF_8));
    }

    public static byte[] download(String url) throws IOException {
        var request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .GET()
            .build();
        try {
            var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200)
                throw new IOException("HTTP " + response.statusCode() + " downloading " + url);

            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Mapping download interrupted", exception);
        }
    }

    public static Path cached(String url, Path path, String sha1) throws IOException {
        if (Files.isRegularFile(path) && (sha1 == null || matches(Files.readAllBytes(path), sha1)))
            return path;

        byte[] bytes = download(url);
        if (sha1 != null && !matches(bytes, sha1))
            throw new IOException("Checksum mismatch downloading " + url);

        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "mappings-", ".tmp");
        try {
            Files.write(temporary, bytes);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }

        return path;
    }

    private static boolean matches(byte[] bytes, String sha1) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes)).equalsIgnoreCase(sha1);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
