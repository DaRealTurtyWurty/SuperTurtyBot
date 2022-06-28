package io.github.darealturtywurty.superturtybot.commands.moderation.warnings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

// TODO: Make one per guild instead of this being a singleton
// TODO: Move to database
public class WarnManager {
    public static final WarnManager INSTANCE = new WarnManager();
    private static final Map<Long, Map<Long, Set<WarnInfo>>> USER_WARNS = new HashMap<>();
    
    private WarnManager() {
    }

    @NotNull
    public static WarnInfo addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
        @NotNull String reason) {
        return addWarn(toWarn, guild, warner, reason, System.currentTimeMillis());
    }

    // TODO: Timeout, kick and ban handling
    @NotNull
    public static WarnInfo addWarn(@NotNull User toWarn, @NotNull Guild guild, @NotNull Member warner,
        @NotNull String reason, long time) {
        final Map<Long, Set<WarnInfo>> guildWarns = internal_getOrCreate(guild);
        
        final Set<WarnInfo> warns = guildWarns.containsKey(toWarn.getIdLong()) ? guildWarns.get(toWarn.getIdLong())
            : new HashSet<>();
        final var warn = new WarnInfo(warner, time, reason);
        warns.add(warn);
        guildWarns.put(toWarn.getIdLong(), warns);
        
        return warn;
    }
    
    @NotNull
    public static Set<WarnInfo> clearWarnings(@NotNull Guild guild, @NotNull User user) {
        if (!USER_WARNS.containsKey(guild.getIdLong())
            || !USER_WARNS.get(guild.getIdLong()).containsKey(user.getIdLong()))
            return Set.of();
        
        final Set<WarnInfo> warns = getWarns(guild, user);
        USER_WARNS.get(guild.getIdLong()).remove(user.getIdLong());
        return warns;
    }
    
    @NotNull
    public static ImmutableMap<Long, Set<WarnInfo>> getOrCreate(@NotNull Guild guild) {
        return ImmutableMap.copyOf(internal_getOrCreate(guild));
    }
    
    @NotNull
    public static Set<WarnInfo> getWarns(@NotNull Guild guild, @NotNull User user) {
        return internal_getOrCreate(guild).computeIfAbsent(user.getIdLong(), id -> new HashSet<>());
    }
    
    @Nullable
    public static WarnInfo removeWarn(@NotNull User toRemoveWarn, @NotNull Guild guild, @NotNull String uuid) {
        if (!USER_WARNS.containsKey(guild.getIdLong())
            || !USER_WARNS.get(guild.getIdLong()).containsKey(toRemoveWarn.getIdLong()))
            return null;

        final Set<WarnInfo> warns = getWarns(guild, toRemoveWarn);
        final Optional<WarnInfo> warn = warns.stream().filter(w -> w.uuid().toString().equalsIgnoreCase(uuid))
            .findFirst();

        if (!warn.isPresent())
            return null;

        warns.remove(warn.get());
        return warn.get();
    }
    
    @NotNull
    private static Map<Long, Set<WarnInfo>> internal_getOrCreate(@NotNull Guild guild) {
        return USER_WARNS.computeIfAbsent(guild.getIdLong(), id -> new HashMap<>());
    }
}
