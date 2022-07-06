package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.util.HashSet;
import java.util.Set;

import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mongodb.client.model.Filters;

import io.github.darealturtywurty.superturtybot.database.Database;
import io.github.darealturtywurty.superturtybot.database.pojos.collections.Warning;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class WarnManager {
    private WarnManager() {
    }
    
    public static @NotNull Warning addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
        @NotNull String reason) {
        return addWarn(toWarn, guild, warner, reason, System.currentTimeMillis());
    }
    
    // TODO: Timeout, kick and ban handling
    public static @NotNull Warning addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
        @NotNull String reason, long time) {
        final var warn = new Warning(guild.getIdLong(), toWarn.getIdLong(), reason, warner.getIdLong());
        Database.getDatabase().warnings.insertOne(warn);
        return warn;
    }
    
    public static @NotNull Set<Warning> clearWarnings(@NotNull Guild guild, @NotNull User user) {
        final Set<Warning> warns = getWarns(guild, user);
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
        Database.getDatabase().warnings.deleteMany(filter);
        return warns;
    }
    
    public static @NotNull Set<Warning> getWarns(@NotNull Guild guild, @NotNull User user) {
        final Set<Warning> warnings = new HashSet<>();
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", user.getIdLong()));
        Database.getDatabase().warnings.find(filter).forEach(warnings::add);
        return warnings;
    }
    
    public static @Nullable Warning removeWarn(@NotNull User toRemoveWarn, @NotNull Guild guild, @NotNull String uuid) {
        final Bson filter = Filters.and(Filters.eq("guild", guild.getIdLong()),
            Filters.eq("user", toRemoveWarn.getIdLong()), Filters.eq("uuid", uuid));
        
        return Database.getDatabase().warnings.findOneAndDelete(filter);
    }
}
