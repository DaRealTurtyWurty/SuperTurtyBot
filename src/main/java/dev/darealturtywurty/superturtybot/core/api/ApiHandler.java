package dev.darealturtywurty.superturtybot.core.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.Environment;
import dev.darealturtywurty.superturtybot.commands.economy.JobCommand;
import dev.darealturtywurty.superturtybot.core.api.pojo.*;
import dev.darealturtywurty.superturtybot.core.api.request.*;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.core.util.object.CoupledPair;
import io.javalin.http.HttpStatus;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ApiHandler {
    private static final String BASE_URL = "https://api.turtywurty.dev/";

    static {
        Environment.INSTANCE.turtyApiKey().ifPresentOrElse(key -> {
            if (key.isBlank()) {
                throw new IllegalStateException("Turty API Key is blank!");
            }
        }, () -> {
            throw new IllegalStateException("Turty API Key is not present!");
        });
    }

    public static Either<BufferedImage, HttpStatus> getFlag(String cca3) {
        try (Response response = makeRequest("geo/flag?apiKey=%s&cca3=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), cca3))) {
            return respondWithImage(response);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Pair<BufferedImage, Region>, HttpStatus> getFlag(RegionExcludeRequestData requestData) {
        StringBuilder path = new StringBuilder("geo/flag/random?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()));
        if (requestData.hasExclusions()) {
            path.append("&exclude=");
            for (String exclusionType : requestData.getExclusions()) {
                path.append(exclusionType).append(",");
            }

            path = new StringBuilder(path.substring(0, path.length() - 1));
        }

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            Region region = Constants.GSON.fromJson(object.get("region"), Region.class);

            String base64 = object.get("image").getAsString();
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            return Either.left(Pair.of(image, region));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Pair<BufferedImage, Region>, HttpStatus> getFlag() {
        return getFlag(RegionExcludeRequestData.empty());
    }

    public static Either<BufferedImage, HttpStatus> getOutline(String cca3) {
        try (Response response = makeRequest("geo/outline?apiKey=%s&cca3=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), cca3))) {
            return respondWithImage(response);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Pair<BufferedImage, Region>, HttpStatus> getOutline(RegionExcludeRequestData requestData) {
        StringBuilder path = new StringBuilder("geo/outline/random?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()));
        if (requestData.hasExclusions()) {
            path.append("&exclude=");
            for (String exclusionType : requestData.getExclusions()) {
                path.append(exclusionType).append(",");
            }

            path = new StringBuilder(path.substring(0, path.length() - 1));
        }

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            Region region = Constants.GSON.fromJson(object.get("region"), Region.class);

            String base64 = object.get("image").getAsString();
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            return Either.left(Pair.of(image, region));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Pair<BufferedImage, Region>, HttpStatus> getOutline() {
        return getOutline(RegionExcludeRequestData.empty());
    }

    public static Either<Region, HttpStatus> getTerritoryData(String cca3) {
        try (Response response = makeRequest("geo/data?apiKey=%s&cca3=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), cca3))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            if (body.contentLength() == 0)
                return Either.right(HttpStatus.NOT_FOUND);

            return Either.left(Constants.GSON.fromJson(body.string(), Region.class));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Region, HttpStatus> getTerritoryData(RegionExcludeRequestData requestData) {
        StringBuilder path = new StringBuilder("geo/data/random?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()));
        if (requestData.hasExclusions()) {
            path.append("&exclude=");
            for (String exclusionType : requestData.getExclusions()) {
                path.append(exclusionType).append(",");
            }

            path = new StringBuilder(path.substring(0, path.length() - 1));
        }

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            if (body.contentLength() == 0)
                return Either.right(HttpStatus.NOT_FOUND);

            return Either.left(Constants.GSON.fromJson(body.string(), Region.class));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Region, HttpStatus> getTerritoryData() {
        return getTerritoryData(RegionExcludeRequestData.empty());
    }

    public static Either<List<Region>, HttpStatus> getAllRegions(RegionExcludeRequestData requestData) {
        StringBuilder path = new StringBuilder("geo/data/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()));
        if (requestData.hasExclusions()) {
            path.append("&exclude=");
            for (String exclusionType : requestData.getExclusions()) {
                path.append(exclusionType).append(",");
            }

            path = new StringBuilder(path.substring(0, path.length() - 1));
        }

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<Region> territories = new ArrayList<>();
            for (JsonElement element : array) {
                territories.add(Constants.GSON.fromJson(element, Region.class));
            }

            return Either.left(territories);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<List<Region>, HttpStatus> getAllRegions() {
        return getAllRegions(RegionExcludeRequestData.empty());
    }

    public static Either<List<String>, HttpStatus> getWords(WordRequest requestData) {
        StringBuilder path = new StringBuilder("words?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()));
        requestData.getLength().ifPresent(length ->
                path.append("&length=").append(length));

        requestData.getStartsWith().ifPresent(startsWith ->
                path.append("&startsWith=").append(startsWith));

        requestData.getAmount().ifPresent(amount ->
                path.append("&amount=").append(amount));

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<String> words = array.asList().stream().map(JsonElement::getAsString).toList();
            return Either.left(words);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<List<String>, HttpStatus> getWords(RandomWordRequestData requestData) {
        StringBuilder path = new StringBuilder("words/random?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()));
        requestData.getLength().ifPresent(length ->
                path.append("&length=").append(length));

        requestData.getMinLength().ifPresent(minLength ->
                path.append("&minLength=").append(minLength));

        requestData.getMaxLength().ifPresent(maxLength ->
                path.append("&maxLength=").append(maxLength));

        requestData.getStartsWith().ifPresent(startsWith ->
                path.append("&startsWith=").append(startsWith));

        requestData.getAmount().ifPresent(amount ->
                path.append("&amount=").append(amount));

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<String> words = array.asList().stream().map(JsonElement::getAsString).toList();
            return Either.left(words);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Boolean, HttpStatus> isWord(String word) {
        try (Response response = makeRequest("words/validate?apiKey=%s&word=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), word))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            return Either.left(object.get("valid").getAsBoolean());
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<CoupledPair<MinecraftVersion>, HttpStatus> getLatestMinecraft() {
        try (Response response = makeRequest("minecraft/latest?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String release = object.get("release").getAsString();
            String snapshot = object.get("snapshot").getAsString();
            return Either.left(new CoupledPair<>(new MinecraftVersion(release, true), new MinecraftVersion(snapshot, false)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<List<MinecraftVersion>, HttpStatus> getAllMinecraft() {
        try (Response response = makeRequest("minecraft/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<MinecraftVersion> versions = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(obj -> new MinecraftVersion(obj.get("version").getAsString(), obj.get("isRelease").getAsBoolean()))
                    .toList();
            return Either.left(versions);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<CoupledPair<ForgeVersion>, HttpStatus> getLatestForge() {
        try (Response response = makeRequest("minecraft/forge/latest?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String stable = object.get("stable").getAsString();
            String latest = object.get("latest").getAsString();
            return Either.left(new CoupledPair<>(new ForgeVersion(stable, true), new ForgeVersion(latest, false)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<List<ForgeVersion>, HttpStatus> getAllForge() {
        try (Response response = makeRequest("minecraft/forge/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<ForgeVersion> versions = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(obj -> new ForgeVersion(obj.get("version").getAsString(), obj.get("isStable").getAsBoolean()))
                    .toList();
            return Either.left(versions);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<CoupledPair<NeoforgeVersion>, HttpStatus> getLatestNeoforge() {
        try (Response response = makeRequest("minecraft/neoforge/latest?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String stable = object.get("stable").getAsString();
            String latest = object.get("latest").getAsString();
            return Either.left(new CoupledPair<>(new NeoforgeVersion(stable, true), new NeoforgeVersion(latest, false)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<List<NeoforgeVersion>, HttpStatus> getAllNeoforge() {
        try (Response response = makeRequest("minecraft/neoforge/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<NeoforgeVersion> versions = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(obj -> new NeoforgeVersion(obj.get("version").getAsString(), obj.get("isStable").getAsBoolean()))
                    .toList();
            return Either.left(versions);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiStatus.Experimental
    public static Either<CoupledPair<FabricVersion>, HttpStatus> getLatestFabric() {
        try (Response response = makeRequest("minecraft/fabric/latest?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String stable = object.get("stable").getAsString();
            String unstable = object.get("unstable").getAsString();
            return Either.left(new CoupledPair<>(new FabricVersion(stable, true), new FabricVersion(unstable, false)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiStatus.Experimental
    public static Either<List<FabricVersion>, HttpStatus> getAllFabric() {
        try (Response response = makeRequest("minecraft/fabric/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<FabricVersion> versions = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(obj -> new FabricVersion(obj.get("version").getAsString(), obj.get("isStable").getAsBoolean()))
                    .toList();
            return Either.left(versions);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiStatus.Experimental
    public static Either<CoupledPair<QuiltVersion>, HttpStatus> getLatestQuilt() {
        try (Response response = makeRequest("minecraft/quilt/latest?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String stable = object.get("stable").getAsString();
            String unstable = object.get("unstable").getAsString();
            return Either.left(new CoupledPair<>(new QuiltVersion(stable, true), new QuiltVersion(unstable, false)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiStatus.Experimental
    public static Either<List<QuiltVersion>, HttpStatus> getAllQuilt() {
        try (Response response = makeRequest("minecraft/quilt/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<QuiltVersion> versions = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(obj -> new QuiltVersion(obj.get("version").getAsString(), obj.get("isRelease").getAsBoolean()))
                    .toList();
            return Either.left(versions);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<ParchmentVersion, HttpStatus> getLatestParchment() {
        try (Response response = makeRequest("minecraft/parchment/latest?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String version = object.get("version").getAsString();
            return Either.left(new ParchmentVersion(version));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<List<ParchmentVersion>, HttpStatus> getAllParchment() {
        try (Response response = makeRequest("minecraft/parchment/all?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonArray array = Constants.GSON.fromJson(json, JsonArray.class);
            List<ParchmentVersion> versions = array.asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(obj -> new ParchmentVersion(obj.get("version").getAsString()))
                    .toList();
            return Either.left(versions);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<ParchmentVersion, HttpStatus> getParchment(String version) {
        try (Response response = makeRequest("minecraft/parchment/" + version + "?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String parchmentVersion = object.get("version").getAsString();
            return Either.left(new ParchmentVersion(parchmentVersion));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<BufferedImage, HttpStatus> resize(ImageResizeRequestData requestData) {
        if (requestData.getWidth().orElse(1) < 1) {
            return Either.right(HttpStatus.BAD_REQUEST);
        }

        if (requestData.getHeight().orElse(1) < 1) {
            return Either.right(HttpStatus.BAD_REQUEST);
        }

        String imgUrl = requestData.getUrl();

        String pathStr = "image/resize?apiKey=%s&url=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), imgUrl);
        var path = new StringBuilder(pathStr);
        requestData.getWidth().ifPresent(width ->
                path.append("&length=").append(width));

        requestData.getHeight().ifPresent(height ->
                path.append("&height=").append(height));

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String base64Img = body.string();
            if (base64Img.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            byte[] bytes = Base64.getDecoder().decode(base64Img);
            var stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);
            return Either.left(image);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<BufferedImage, HttpStatus> rotate(ImageRotateRequestData requestData) {
        if (requestData.getAngle().orElse(0) % 90 != 0)
            return Either.right(HttpStatus.BAD_REQUEST);

        String imgUrl = requestData.getUrl();

        String pathStr = "image/rotate?apiKey=%s&url=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), imgUrl);
        var path = new StringBuilder(pathStr);
        requestData.getAngle().ifPresent(angle ->
                path.append("&angle=").append(angle));

        try (Response response = makeRequest(path.toString())) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String base64Img = body.string();
            if (base64Img.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            byte[] bytes = Base64.getDecoder().decode(base64Img);
            var stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);
            return Either.left(image);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<BufferedImage, HttpStatus> flip(String imgUrl, FlipType flipType) {
        try (Response response = makeRequest("image/flip?apiKey=%s&url=%s&flipType=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), imgUrl, flipType.name()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String base64Img = body.string();
            if (base64Img.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            byte[] bytes = Base64.getDecoder().decode(base64Img);
            var stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);
            return Either.left(image);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<BufferedImage, HttpStatus> flagify(ImageFlagifyRequestData requestData) {
        try (Response response = makeRequest("image/flag?apiKey=%s&url=%s&colors=%d".formatted(Environment.INSTANCE.turtyApiKey().get(), requestData.getUrl(), requestData.getColors()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String base64Img = body.string();
            if (base64Img.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            byte[] bytes = Base64.getDecoder().decode(base64Img);
            var stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);
            return Either.left(image);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<BufferedImage, HttpStatus> lgbtify(String imgUrl) {
        try (Response response = makeRequest("image/lgbt?apiKey=%s&url=%s".formatted(Environment.INSTANCE.turtyApiKey().get(), imgUrl))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String base64Img = body.string();
            if (base64Img.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            byte[] bytes = Base64.getDecoder().decode(base64Img);
            var stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);
            return Either.left(image);
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Geoguesser, HttpStatus> geoguesser() {
        try (Response response = makeRequest("geo/guesser?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);

            String country = object.get("country").getAsString();

            String base64 = object.get("image").getAsString();
            byte[] bytes = Base64.getDecoder().decode(base64);
            var stream = new ByteArrayInputStream(bytes);
            BufferedImage image = ImageIO.read(stream);

            return Either.left(new Geoguesser(country, image));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Pornstar, HttpStatus> getPornstar() {
        try (Response response = makeRequest("nsfw/pornstar?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            return Either.left(Constants.GSON.fromJson(json, Pornstar.class));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<WouldYouRather, HttpStatus> getRandomWouldYouRather(WouldYouRatherRequest request) {
        String urlPath = "fun/wyr/random?apiKey=%s&includeNSFW=%s&nsfw=%s"
                .formatted(Environment.INSTANCE.turtyApiKey().get(), request.isIncludeNsfw(), request.isNsfw());
        try (Response response = makeRequest(urlPath)) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            String optionA = object.get("optionA").getAsString();
            String optionB = object.get("optionB").getAsString();

            return Either.left(new WouldYouRather(optionA, optionB));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<JobCommand.Code, HttpStatus> findCode() {
        try (Response response = makeRequest("code/guesser?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            String code = URLDecoder.decode(object.get("code").getAsString(), StandardCharsets.UTF_8);
            JsonObject languageObj = object.get("language").getAsJsonObject();
            String language = languageObj.get("name").getAsString();
            String extension = languageObj.get("extension").getAsString();

            return Either.left(new JobCommand.Code(code, new JobCommand.Code.Language(language, extension)));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Either<Pair<String, byte[]>, HttpStatus> getRandomCelebrity() {
        try (Response response = makeRequest("fun/celebrity/random?apiKey=%s".formatted(Environment.INSTANCE.turtyApiKey().get()))) {
            if (response.code() != HttpStatus.OK.getCode())
                return Either.right(HttpStatus.forStatus(response.code()));

            ResponseBody body = response.body();
            if (body == null)
                return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

            String json = body.string();
            if (json.isBlank())
                return Either.right(HttpStatus.NOT_FOUND);

            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            String name = object.get("name").getAsString();
            String base64 = object.get("image").getAsString();
            byte[] bytes = Base64.getDecoder().decode(base64);

            return Either.left(Pair.of(name, bytes));
        } catch (IOException exception) {
            Constants.LOGGER.error("Something went wrong making this request", exception);
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static Response makeRequest(String path) throws IOException {
        try {
            return Constants.HTTP_CLIENT.newCall(new Request.Builder().url(BASE_URL + path).build()).execute();
        } catch (IOException exception) {
            throw new IOException("Failed to make request!", exception);
        }
    }

    @NotNull
    private static Either<BufferedImage, HttpStatus> respondWithImage(Response response) throws IOException {
        if (response.code() != HttpStatus.OK.getCode())
            return Either.right(HttpStatus.forStatus(response.code()));

        ResponseBody body = response.body();
        if (body == null)
            return Either.right(HttpStatus.INTERNAL_SERVER_ERROR);

        return Either.left(ImageIO.read(body.byteStream()));
    }
}
