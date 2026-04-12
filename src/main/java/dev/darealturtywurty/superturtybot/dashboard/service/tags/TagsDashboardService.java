package dev.darealturtywurty.superturtybot.dashboard.service.tags;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import dev.darealturtywurty.superturtybot.dashboard.http.DashboardApiException;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Tag;
import dev.darealturtywurty.superturtybot.database.pojos.collections.UserEmbeds;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TagsDashboardService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 25;

    private final JDA jda;

    public TagsDashboardService(JDA jda) {
        this.jda = jda;
    }

    public DashboardTagsPageResponse getTags(long guildId, int page, int pageSize) {
        Guild guild = requireGuild(guildId);
        long totalCount = Database.getDatabase().tags.countDocuments(Filters.eq("guild", guildId));
        int safePageSize = clampPageSize(pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) safePageSize));
        int safePage = clampPage(page, totalPages);
        long offset = (long) (safePage - 1) * safePageSize;

        return new DashboardTagsPageResponse(
                safePage,
                safePageSize,
                totalCount,
                totalPages,
                listTags(guild, offset, safePageSize)
        );
    }

    public DashboardTagsPageResponse deleteTag(long guildId, String name, int page, int pageSize) {
        Guild guild = requireGuild(guildId);
        Tag tag = requireTag(guildId, name);

        DeleteResult result = Database.getDatabase().tags.deleteOne(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("name", tag.getName())
        ));

        if (result.getDeletedCount() == 0) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "tag_not_found",
                    "That tag could not be found.");
        }

        return getTags(guildId, page, pageSize);
    }

    public DashboardTagsPageResponse createTag(long guildId, DashboardTagCreateRequest request, int page, int pageSize) {
        requireGuild(guildId);
        validateCreateRequest(request);

        String name = request.getName().trim();
        String content = request.getContent();
        String contentType = normalizeContentType(request.getContentType());
        long actorUserId = parseUserId(request.getActorUserId());

        if (Database.getDatabase().tags.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("name", name)
        )).first() != null) {
            throw new DashboardApiException(HttpStatus.CONFLICT, "tag_already_exists",
                    "A tag with that name already exists.");
        }

        JsonObject data = new JsonObject();
        if ("embed".equals(contentType)) {
            data.addProperty("embed", URLEncoder.encode(content.trim(), StandardCharsets.UTF_8));

            UserEmbeds userEmbeds = Database.getDatabase().userEmbeds.find(Filters.eq("user", actorUserId)).first();
            if (userEmbeds == null) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_user_embeds",
                        "You do not have any embeds. Create one first and try again.");
            }

            if (userEmbeds.getEmbed(content.trim()).isEmpty()) {
                throw new DashboardApiException(HttpStatus.BAD_REQUEST, "missing_user_embed",
                        "You do not have an embed with that name.");
            }
        } else {
            data.addProperty("message", content);
        }

        InsertOneResult result = Database.getDatabase().tags.insertOne(new Tag(guildId, actorUserId, name, data.toString()));
        if (!result.wasAcknowledged()) {
            throw new DashboardApiException(HttpStatus.INTERNAL_SERVER_ERROR, "tag_create_failed",
                    "The tag could not be created.");
        }

        return getTags(guildId, page, pageSize);
    }

    private List<DashboardTagRecord> listTags(Guild guild, long offset, int pageSize) {
        List<Tag> tags = Database.getDatabase().tags.find(Filters.eq("guild", guild.getIdLong()))
                .sort(Sorts.ascending("name"))
                .skip((int) offset)
                .limit(pageSize)
                .into(new ArrayList<>());

        List<DashboardTagRecord> records = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            records.add(toRecord(guild, tag));
        }

        return records;
    }

    private DashboardTagRecord toRecord(Guild guild, Tag tag) {
        ResolvedUser user = resolveUser(tag.getUser());
        TagContent content = parseContent(tag.getData());

        return new DashboardTagRecord(
                tag.getName(),
                Long.toString(tag.getUser()),
                user.displayName(),
                user.avatarUrl(),
                content.type(),
                content.content(),
                tag.getData()
        );
    }

    private Tag requireTag(long guildId, String name) {
        if (name == null || name.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_tag_name",
                    "The supplied tag name was missing.");
        }

        Tag tag = Database.getDatabase().tags.find(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("name", name.trim())
        )).first();
        if (tag == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "tag_not_found",
                    "That tag could not be found.");
        }

        return tag;
    }

    private ResolvedUser resolveUser(long userId) {
        User user = this.jda.getUserById(userId);
        if (user == null) {
            return new ResolvedUser("Unknown User", null);
        }

        return new ResolvedUser(user.getEffectiveName(), user.getEffectiveAvatarUrl());
    }

    private static TagContent parseContent(String data) {
        if (data == null || data.isBlank()) {
            return new TagContent("raw", "");
        }

        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            if (json.has("message")) {
                return new TagContent("message", json.get("message").getAsString());
            }

            if (json.has("embed")) {
                return new TagContent("embed", URLDecoder.decode(json.get("embed").getAsString(), StandardCharsets.UTF_8));
            }
        } catch (RuntimeException ignored) {
            return new TagContent("raw", data);
        }

        return new TagContent("raw", data);
    }

    private Guild requireGuild(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null) {
            throw new DashboardApiException(HttpStatus.NOT_FOUND, "dashboard_guild_not_connected",
                    "TurtyBot is not currently connected to that guild.");
        }

        return guild;
    }

    private static int clampPageSize(int pageSize) {
        return Math.clamp(pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize, 1, MAX_PAGE_SIZE);
    }

    private static int clampPage(int page, int totalPages) {
        int safePage = page <= 0 ? 1 : page;
        return Math.clamp(safePage, 1, Math.max(1, totalPages));
    }

    private static void validateCreateRequest(DashboardTagCreateRequest request) {
        if (request == null) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_tag_request",
                    "The supplied tag data was missing.");
        }

        String name = request.getName();
        if (name == null || name.trim().length() < 2 || name.trim().length() > 64) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_tag_name",
                    "The tag name must be between 2 and 64 characters.");
        }

        String contentType = normalizeContentType(request.getContentType());
        if (!"message".equals(contentType) && !"embed".equals(contentType)) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_tag_content_type",
                    "The tag content type must be either message or embed.");
        }

        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_tag_content",
                    "The tag content was missing.");
        }

        if (content.trim().length() > 2000) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_tag_content",
                    "The tag content must not exceed 2000 characters.");
        }

        parseUserId(request.getActorUserId());
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "message";
        }

        return contentType.trim().toLowerCase();
    }

    private static long parseUserId(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_actor_user_id",
                    "The actor user ID was missing.");
        }

        try {
            long parsed = Long.parseLong(actorUserId);
            if (parsed <= 0L) {
                throw new NumberFormatException("User ID must be positive.");
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new DashboardApiException(HttpStatus.BAD_REQUEST, "invalid_actor_user_id",
                    "The actor user ID was not valid.");
        }
    }

    private record ResolvedUser(String displayName, String avatarUrl) {
    }

    private record TagContent(String type, String content) {
    }
}
