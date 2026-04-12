"use client";

import {useMemo, useState} from "react";
import GuildMemberMultiSelect from "@/components/GuildMemberMultiSelect";
import GuildRoleSelect from "@/components/GuildRoleSelect";
import NotifierPingPreview from "@/components/NotifierPingPreview";

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

function buildMentionPreview(roleIds: string[], memberIds: string[]) {
    return [...roleIds.map(id => `<@&${id}>`), ...memberIds.map(id => `<@${id}>`)].join(" ");
}

export default function NotifierPingSelect({
    guildId,
    value,
    onChange,
    disabled
}: {
    guildId: string;
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
}) {
    const parsed = useMemo(() => parseMentionTargets(value), [value]);
    const [resolvedMembers, setResolvedMembers] = useState<{
        id: string;
        username: string;
        displayName: string;
        avatarUrl: string | null;
    }[]>([]);
    const preview = useMemo(
        () => buildMentionPreview(parsed.roleIds, parsed.memberIds),
        [parsed.memberIds, parsed.roleIds]
    );

    function updateRoles(roleIds: string[]) {
        onChange(buildMentionPreview(roleIds, parsed.memberIds));
    }

    function updateMembers(memberIds: string[]) {
        onChange(buildMentionPreview(parsed.roleIds, memberIds));
    }

    return <section className="border border-slate-800/80 bg-slate-950/60 p-5">
        <div className="space-y-2">
            <h3 className="text-sm font-semibold text-white">Who to ping</h3>
            <p className="text-sm text-slate-400">Pick any mix of roles and users. The resolved mention string is shown below.</p>
        </div>

        <div className="mt-4 grid gap-4 xl:grid-cols-2">
            <GuildRoleSelect
                guildId={guildId}
                multiple
                values={parsed.roleIds}
                onValuesChange={updateRoles}
                disabled={disabled}
                label="Roles"
                description="Select one or more roles to mention."
                placeholder="Select roles"
            />

            <GuildMemberMultiSelect
                guildId={guildId}
                values={parsed.memberIds}
                onValuesChange={updateMembers}
                onResolvedValuesChange={setResolvedMembers}
                disabled={disabled}
                label="Users"
                description="Select one or more members to mention."
                placeholder="Search members"
            />
        </div>

        <label className="mt-4 block">
            <span className="text-sm font-semibold text-white">Resolved ping</span>
            <p className="mt-1 text-sm text-slate-400">This resolves to names and role labels while still storing Discord mentions underneath.</p>
            <div className="mt-3">
                <NotifierPingPreview guildId={guildId} value={preview} resolvedMembers={resolvedMembers} />
            </div>
        </label>
    </section>;
}
