"use client";

import Image from "next/image";
import {useEffect, useMemo, useState} from "react";
import {useGuildRoles, type DashboardGuildRoleOption} from "@/components/GuildRoleSelect";

interface DashboardGuildMemberInfo {
    id: string;
    username: string;
    displayName: string;
    avatarUrl: string | null;
}

function parseMentionTargets(value: string) {
    const roleIds: string[] = [];
    const memberIds: string[] = [];
    const seenRoles = new Set<string>();
    const seenMembers = new Set<string>();

    const regex = /<@&(\d{17,20})>|<@!?(\d{17,20})>/g;
    for (const match of value.matchAll(regex)) {
        const roleId = match[1];
        const memberId = match[2];

        if (roleId && !seenRoles.has(roleId)) {
            seenRoles.add(roleId);
            roleIds.push(roleId);
        }

        if (memberId && !seenMembers.has(memberId)) {
            seenMembers.add(memberId);
            memberIds.push(memberId);
        }
    }

    return {roleIds, memberIds};
}

function RoleChip({role}: {role: DashboardGuildRoleOption}) {
    const color = role.color === 0 ? "#64748b" : `#${role.color.toString(16).padStart(6, "0")}`;

    return <span className="inline-flex items-center gap-2 border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-sm text-slate-100">
        <span className="h-3 w-3 rounded-full" style={{backgroundColor: color}} aria-hidden="true" />
        <span className="font-medium">@{role.name}</span>
    </span>;
}

function MemberChip({member}: {member: DashboardGuildMemberInfo}) {
    return <span className="inline-flex items-center gap-2 border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-sm text-slate-100">
        {member.avatarUrl ? <Image src={member.avatarUrl} alt="" width={24} height={24} className="h-6 w-6 rounded-full object-cover" /> : <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-800 text-xs font-semibold uppercase text-slate-200">{member.displayName.trim().slice(0, 1).toUpperCase() || "?"}</span>}
        <span className="font-medium">@{member.displayName}</span>
    </span>;
}

function UnknownChip({label}: {label: string}) {
    return <span className="inline-flex items-center border border-slate-700 bg-slate-900 px-2.5 py-1.5 text-sm text-slate-400">
        {label}
    </span>;
}

export default function NotifierPingPreview({
    guildId,
    value,
    resolvedMembers = []
}: {
    guildId: string;
    value: string;
    resolvedMembers?: DashboardGuildMemberInfo[];
}) {
    const {roleIds, memberIds} = useMemo(() => parseMentionTargets(value), [value]);
    const {roles} = useGuildRoles(guildId);
    const [members, setMembers] = useState<Record<string, DashboardGuildMemberInfo>>({});
    const resolvedMemberMap = useMemo(() => {
        const map = new Map<string, DashboardGuildMemberInfo>();

        for (const member of resolvedMembers) {
            map.set(member.id, member);
        }

        return map;
    }, [resolvedMembers]);

    useEffect(() => {
        let isCancelled = false;
        const missingIds = memberIds.filter(id => !members[id]);
        if (missingIds.length === 0) {
            return;
        }

        Promise.all(missingIds.map(async memberId => {
            const response = await fetch(`/api/discord/guilds/${guildId}/members?query=${encodeURIComponent(memberId)}`, {
                cache: "no-store"
            });

            if (!response.ok) {
                return null;
            }

            const payload = await response.json() as {members?: DashboardGuildMemberInfo[]};
            return payload.members?.find(member => member.id === memberId) ?? null;
        }))
            .then(results => {
                if (isCancelled) {
                    return;
                }

                setMembers(current => {
                    const next = {...current};
                    for (const member of results) {
                        if (member) {
                            next[member.id] = member;
                        }
                    }

                    return next;
                });
            })
            .catch(() => null);

        return () => {
            isCancelled = true;
        };
    }, [guildId, memberIds, members]);

    const roleMap = useMemo(() => new Map(roles.map(role => [role.id, role])), [roles]);

    const resolvedRoles = roleIds.map(id => roleMap.get(id) ?? null);
    const resolvedMentions = memberIds.map(id => resolvedMemberMap.get(id) ?? members[id] ?? null);

    return <div className="border border-slate-700 bg-slate-900 px-4 py-3">
        {resolvedRoles.length === 0 && resolvedMentions.length === 0 ? <p className="text-sm text-slate-500">No users or roles selected.</p> : <div className="flex flex-wrap gap-2">
            {resolvedRoles.map((role, index) => role ? <RoleChip key={role.id} role={role} /> : <UnknownChip key={`role-${roleIds[index]}`} label={`@Role ${roleIds[index]}`} />)}
            {resolvedMentions.map((member, index) => member ? <MemberChip key={member.id} member={member} /> : <UnknownChip key={`member-${memberIds[index]}`} label={`@User ${memberIds[index]}`} />)}
        </div>}
    </div>;
}
