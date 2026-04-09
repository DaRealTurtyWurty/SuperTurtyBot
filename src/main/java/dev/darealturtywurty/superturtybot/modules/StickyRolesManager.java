package dev.darealturtywurty.superturtybot.modules;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import dev.darealturtywurty.superturtybot.database.pojos.collections.StickyRoles;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class StickyRolesManager extends ListenerAdapter {
    public static final StickyRolesManager INSTANCE = new StickyRolesManager();

    private StickyRolesManager() {
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        if (!guildData.isStickyRolesEnabled())
            return;

        Member member = event.getMember();
        if (!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
            return;

        StickyRoles stickyRoles = Database.getDatabase().stickyRoles.find(Filters.and(
                Filters.eq("guild", guild.getIdLong()),
                Filters.eq("user", member.getIdLong())
        )).first();
        if (stickyRoles == null || stickyRoles.getRoles().isEmpty())
            return;

        List<Long> validRoleIds = stickyRoles.getRoles().stream()
                .map(guild::getRoleById)
                .filter(role -> role != null && !role.isPublicRole() && !role.isManaged())
                .map(Role::getIdLong)
                .distinct()
                .toList();

        if (validRoleIds.size() != stickyRoles.getRoles().size()) {
            if (validRoleIds.isEmpty()) {
                clearStoredRoles(guild.getIdLong(), member.getIdLong());
            } else {
                stickyRoles.setRoles(validRoleIds);
                stickyRoles.setSavedAt(System.currentTimeMillis());
                Database.getDatabase().stickyRoles.replaceOne(
                        Filters.and(Filters.eq("guild", guild.getIdLong()), Filters.eq("user", member.getIdLong())),
                        stickyRoles,
                        new ReplaceOptions().upsert(true)
                );
            }
        }

        List<Role> rolesToRestore = validRoleIds.stream()
                .map(guild::getRoleById)
                .filter(role -> role != null && guild.getSelfMember().canInteract(role))
                .toList();
        if (rolesToRestore.isEmpty())
            return;

        guild.modifyMemberRoles(member, rolesToRestore, List.of()).queue(
                _ -> Constants.LOGGER.info("Restored {} sticky roles for user {} in guild {}",
                        rolesToRestore.size(), member.getIdLong(), guild.getIdLong()),
                throwable -> Constants.LOGGER.error("Failed to restore sticky roles for user {} in guild {}",
                        member.getIdLong(), guild.getIdLong(), throwable)
        );
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        GuildData guildData = GuildData.getOrCreateGuildData(guild);
        if (!guildData.isStickyRolesEnabled())
            return;

        Member member = event.getMember();
        if (member == null)
            return;

        if(member.getRoles().isEmpty()) {
            clearStoredRoles(guild.getIdLong(), member.getIdLong());
            return;
        }

        User user = event.getUser();
        if (isLikelyDeletedAccount(user))
            return;

        guild.retrieveBan(user).queue(
                _ -> clearStoredRoles(guild.getIdLong(), user.getIdLong()),
                failure -> {
                    if (failure instanceof ErrorResponseException exception
                            && exception.getErrorResponse() == ErrorResponse.UNKNOWN_BAN) {
                        persistStickyRoles(guild, member);
                        return;
                    }

                    Constants.LOGGER.error("Failed to determine whether user {} was banned in guild {}. Sticky roles were not updated.",
                            user.getIdLong(), guild.getIdLong(), failure);
                }
        );
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        clearStoredRoles(event.getGuild().getIdLong(), event.getUser().getIdLong());
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        long guildId = event.getGuild().getIdLong();
        long roleId = event.getRole().getIdLong();

        List<StickyRoles> entries = Database.getDatabase().stickyRoles.find(Filters.eq("guild", guildId))
                .into(new ArrayList<>());
        for (StickyRoles stickyRoles : entries) {
            List<Long> updatedRoles = stickyRoles.getRoles().stream()
                    .filter(storedRoleId -> storedRoleId != roleId)
                    .toList();

            if (updatedRoles.size() == stickyRoles.getRoles().size())
                continue;

            if (updatedRoles.isEmpty()) {
                clearStoredRoles(guildId, stickyRoles.getUser());
                continue;
            }

            stickyRoles.setRoles(updatedRoles);
            stickyRoles.setSavedAt(System.currentTimeMillis());
            Database.getDatabase().stickyRoles.replaceOne(
                    Filters.and(Filters.eq("guild", guildId), Filters.eq("user", stickyRoles.getUser())),
                    stickyRoles,
                    new ReplaceOptions().upsert(true)
            );
        }
    }

    private static void persistStickyRoles(Guild guild, Member member) {
        List<Long> roleIds = member.getRoles().stream()
                .filter(role -> !role.isPublicRole() && !role.isManaged())
                .map(Role::getIdLong)
                .toList();
        if (roleIds.isEmpty()) {
            clearStoredRoles(guild.getIdLong(), member.getIdLong());
            return;
        }

        var stickyRoles = new StickyRoles(guild.getIdLong(), member.getIdLong(), roleIds, System.currentTimeMillis());
        Database.getDatabase().stickyRoles.replaceOne(
                Filters.and(Filters.eq("guild", guild.getIdLong()),
                        Filters.eq("user", member.getIdLong())),
                stickyRoles,
                new ReplaceOptions().upsert(true)
        );
    }

    private static void clearStoredRoles(long guildId, long userId) {
        Database.getDatabase().stickyRoles.deleteOne(Filters.and(
                Filters.eq("guild", guildId),
                Filters.eq("user", userId)
        ));
    }

    private static boolean isLikelyDeletedAccount(User user) {
        if (user == null)
            return true;

        String name = user.getName();
        if (name.isBlank())
            return false;

        String normalized = name.trim().toLowerCase();
        return normalized.equals("deleted user")
                || normalized.startsWith("deleted user ")
                || normalized.equals("deleted_user")
                || normalized.startsWith("deleted_user_");
    }
}
