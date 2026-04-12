package dev.darealturtywurty.superturtybot.dashboard.service.discord;

import com.mongodb.client.model.Filters;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.channels.DashboardGuildChannelInfo;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.channels.DashboardGuildChannelsResponse;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.member.DashboardGuildMemberInfo;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.member.DashboardGuildMembersResponse;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.roles.DashboardGuildRoleInfo;
import dev.darealturtywurty.superturtybot.dashboard.service.discord.roles.DashboardGuildRolesResponse;
import dev.darealturtywurty.superturtybot.dashboard.service.guild_config.DashboardGuildConfigSnapshot;
import dev.darealturtywurty.superturtybot.dashboard.service.guild_config.GuildConfigCatalogService;
import dev.darealturtywurty.superturtybot.database.Database;
import dev.darealturtywurty.superturtybot.database.pojos.collections.GuildData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

public final class GuildSettingsService {
    private final JDA jda;
    private final GuildConfigCatalogService catalogService;

    public GuildSettingsService(JDA jda, GuildConfigCatalogService catalogService) {
        this.jda = jda;
        this.catalogService = catalogService;
    }

    public DashboardGuildConfigSnapshot getGuildConfigSnapshot(long guildId) {
        GuildData guildData = Database.getDatabase().guildData.find(Filters.eq("guild", guildId)).first();
        boolean persisted = guildData != null;
        if (guildData == null) {
            guildData = new GuildData(guildId);
        }

        return new DashboardGuildConfigSnapshot(
                createGuildInfo(guildId),
                persisted,
                this.catalogService.extractValues(guildData)
        );
    }

    public DashboardGuildChannelsResponse getGuildChannels(long guildId, long userId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null)
            return new DashboardGuildChannelsResponse(List.of());

        Member member = guild.getMemberById(userId);
        if (member == null)
            return new DashboardGuildChannelsResponse(List.of());

        List<GuildChannel> visibleChannels = guild.getChannels().stream()
                .filter(channel -> channel.getType() == ChannelType.CATEGORY || member.hasAccess(channel))
                .toList();

        Set<String> visibleChannelIds = visibleChannels.stream()
                .filter(channel -> channel.getType() != ChannelType.CATEGORY)
                .map(GuildChannel::getId)
                .collect(Collectors.toSet());

        List<DashboardGuildChannelInfo> channels = new ArrayList<>();
        for (GuildChannel channel : visibleChannels) {
            if (channel.getType() == ChannelType.CATEGORY
                    && ((Category) channel).getChannels().stream().noneMatch(child -> visibleChannelIds.contains(child.getId()))) {
                continue;
            }

            channels.add(new DashboardGuildChannelInfo(
                    channel.getId(),
                    channel.getName(),
                    mapChannelType(channel),
                    channel instanceof ICategorizableChannel categorizable && categorizable.getParentCategory() != null
                            ? categorizable.getParentCategory().getId()
                            : null,
                    channels.size()
            ));
        }

        return new DashboardGuildChannelsResponse(channels);
    }

    public DashboardGuildRolesResponse getGuildRoles(long guildId, long userId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null)
            return new DashboardGuildRolesResponse(List.of());

        Member member = guild.getMemberById(userId);
        if (member == null)
            return new DashboardGuildRolesResponse(List.of());

        List<DashboardGuildRoleInfo> roles = guild.getRoles().stream()
                .filter(role -> !role.isPublicRole() && !role.isManaged())
                .filter(member::canInteract)
                .sorted(Comparator.comparingInt(Role::getPosition).reversed().thenComparing(Role::getName))
                .map(role -> new DashboardGuildRoleInfo(
                        role.getId(),
                        role.getName(),
                        role.getColorRaw(),
                        role.getPosition()
                ))
                .toList();

        return new DashboardGuildRolesResponse(roles);
    }

    public DashboardGuildMembersResponse searchGuildMembers(long guildId, long userId, String query) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null)
            return new DashboardGuildMembersResponse(List.of());

        Member member = guild.getMemberById(userId);
        if (member == null)
            return new DashboardGuildMembersResponse(List.of());

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<DashboardGuildMemberInfo> members = guild.getMembers().stream()
                .map(guildMember -> {
                    String username = guildMember.getUser().getName();
                    String displayName = guildMember.getEffectiveName();
                    return new DashboardGuildMemberInfo(
                            guildMember.getId(),
                            username,
                            displayName,
                            guildMember.getEffectiveAvatarUrl()
                    );
                })
                .filter(info -> matchesMember(info, normalizedQuery))
                .sorted(Comparator
                        .comparingInt((DashboardGuildMemberInfo info) -> scoreMember(info, normalizedQuery))
                        .thenComparing(DashboardGuildMemberInfo::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DashboardGuildMemberInfo::username, String.CASE_INSENSITIVE_ORDER))
                .limit(25)
                .toList();

        return new DashboardGuildMembersResponse(members);
    }

    private DashboardGuildInfo createGuildInfo(long guildId) {
        Guild guild = this.jda.getGuildById(guildId);
        if (guild == null)
            return new DashboardGuildInfo(Long.toString(guildId), "Unknown Guild", null, 0, false);

        return new DashboardGuildInfo(
                guild.getId(),
                guild.getName(),
                guild.getIconUrl(),
                guild.getMemberCount(),
                true
        );
    }

    private static String mapChannelType(GuildChannel channel) {
        return switch (channel.getType()) {
            case TEXT -> "text";
            case VOICE -> "voice";
            case CATEGORY -> "category";
            case NEWS -> "announcement";
            case GUILD_PUBLIC_THREAD, GUILD_PRIVATE_THREAD, GUILD_NEWS_THREAD -> "thread";
            case STAGE -> "stage";
            case FORUM -> "forum";
            case MEDIA -> "media";
            default -> "other";
        };
    }

    private static boolean matchesMember(DashboardGuildMemberInfo info, String query) {
        if (query.isBlank()) {
            return true;
        }

        String id = info.id();
        String username = info.username().toLowerCase(Locale.ROOT);
        String displayName = info.displayName().toLowerCase(Locale.ROOT);
        return id.equals(query)
                || username.contains(query)
                || displayName.contains(query);
    }

    private static int scoreMember(DashboardGuildMemberInfo info, String query) {
        if (query.isBlank()) {
            return 0;
        }

        if (info.id().equals(query)) {
            return 0;
        }

        String username = info.username().toLowerCase(Locale.ROOT);
        String displayName = info.displayName().toLowerCase(Locale.ROOT);
        if (username.startsWith(query) || displayName.startsWith(query)) {
            return 1;
        }

        if (username.contains(query) || displayName.contains(query)) {
            return 2;
        }

        return 3;
    }
}
