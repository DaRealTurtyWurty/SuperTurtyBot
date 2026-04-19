"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardLoggingSettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface LoggingSettingsFormProps {
    guildId: string;
    initialSettings: DashboardLoggingSettings;
}

interface LoggingToggleDefinition {
    key: keyof DashboardLoggingSettings;
    label: string;
    description: string;
}

interface LoggingToggleGroup {
    title: string;
    items: LoggingToggleDefinition[];
}

function toFragment(value: string) {
    const withoutLogPrefix = value.replace(/^log([A-Z])/, "$1");
    return withoutLogPrefix
        .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
        .replace(/[\s_]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .toLowerCase();
}

const TOGGLE_GROUPS: LoggingToggleGroup[] = [
    {
        title: "Server",
        items: [
            {key: "logChannelCreate", label: "Channel Create", description: "Log newly created channels."},
            {key: "logChannelDelete", label: "Channel Delete", description: "Log deleted channels."},
            {key: "logChannelUpdate", label: "Channel Update", description: "Log channel edits and changes."},
            {key: "logGuildUpdate", label: "Guild Update", description: "Log guild-level setting changes."},
            {key: "logRoleCreate", label: "Role Create", description: "Log new roles."},
            {key: "logRoleDelete", label: "Role Delete", description: "Log deleted roles."},
            {key: "logRoleUpdate", label: "Role Update", description: "Log role edits."}
        ]
    },
    {
        title: "Members",
        items: [
            {key: "logMemberJoin", label: "Member Join", description: "Log member joins."},
            {key: "logMemberRemove", label: "Member Leave", description: "Log member leaves."},
            {key: "logBan", label: "Ban", description: "Log bans."},
            {key: "logUnban", label: "Unban", description: "Log unbans."},
            {key: "logTimeout", label: "Timeout", description: "Log member timeouts."},
            {key: "logInviteCreate", label: "Invite Create", description: "Log created invites."},
            {key: "logInviteDelete", label: "Invite Delete", description: "Log deleted invites."}
        ]
    },
    {
        title: "Messages",
        items: [
            {key: "logMessageDelete", label: "Message Delete", description: "Log deleted messages."},
            {key: "logMessageBulkDelete", label: "Bulk Delete", description: "Log bulk message deletions."},
            {key: "logMessageUpdate", label: "Message Edit", description: "Log message edits."}
        ]
    },
    {
        title: "Assets",
        items: [
            {key: "logEmojiAdded", label: "Emoji Added", description: "Log new emojis."},
            {key: "logEmojiRemoved", label: "Emoji Removed", description: "Log deleted emojis."},
            {key: "logEmojiUpdate", label: "Emoji Update", description: "Log emoji edits."},
            {key: "logStickerAdded", label: "Sticker Added", description: "Log new stickers."},
            {key: "logStickerRemove", label: "Sticker Removed", description: "Log removed stickers."},
            {key: "logStickerUpdate", label: "Sticker Update", description: "Log sticker edits."},
            {key: "logForumTagUpdate", label: "Forum Tag Update", description: "Log forum tag changes."}
        ]
    }
];

export default function LoggingSettingsForm({guildId, initialSettings}: LoggingSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        loggingChannelId: initialSettings.loggingChannelId ?? "",
        modLoggingChannelId: initialSettings.modLoggingChannelId ?? ""
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateToggle(key: keyof DashboardLoggingSettings, value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateChannel(value: string) {
        setSettings(current => ({
            ...current,
            loggingChannelId: value
        }));
    }

    function updateModChannel(value: string) {
        setSettings(current => ({
            ...current,
            modLoggingChannelId: value
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/logging`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    ...settings,
                    loggingChannelId: settings.loggingChannelId.trim() || null,
                    modLoggingChannelId: settings.modLoggingChannelId.trim() || null
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save logging settings.");
                return;
            }

            const updated = await response.json() as DashboardLoggingSettings;
            setSettings({
                ...updated,
                loggingChannelId: updated.loggingChannelId ?? "",
                modLoggingChannelId: updated.modLoggingChannelId ?? ""
            });
            setSuccess("Logging settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <div className="grid gap-4 lg:grid-cols-2">
            <GuildChannelSelect
                id="logging-channel"
                guildId={guildId}
                value={settings.loggingChannelId}
                onChange={updateChannel}
                label="Logging Channel"
                description="Leave this blank to disable the main logging destination."
                placeholder="Select a channel"
            />

            <GuildChannelSelect
                id="moderation-logging-channel"
                guildId={guildId}
                value={settings.modLoggingChannelId}
                onChange={updateModChannel}
                label="Moderation Logging Channel"
                description="Used for ban and report logging."
                placeholder="Select a channel"
            />
        </div>

        <div className="grid gap-4 xl:grid-cols-2">
            {TOGGLE_GROUPS.map(group => <section key={group.title} id={toFragment(group.title)} className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <h3 className="text-lg font-semibold text-white">{group.title}</h3>
                <div className="mt-4 space-y-3">
                    {group.items.map(item => <label key={item.key} id={toFragment(item.key)} className="flex items-center justify-between gap-4 border border-slate-800/80 bg-slate-950/50 px-4 py-3 scroll-mt-24">
                        <div>
                            <p className="text-sm font-semibold text-white">{item.label}</p>
                            <p className="mt-1 text-sm text-slate-400">{item.description}</p>
                        </div>
                        <input
                            type="checkbox"
                            checked={Boolean(settings[item.key])}
                            onChange={event => updateToggle(item.key, event.target.checked)}
                            className="h-5 w-5 accent-sky-400"
                        />
                    </label>)}
                </div>
            </section>)}
        </div>

        {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
            {error}
        </p> : null}

        {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">
            {success}
        </p> : null}

        <div className="flex items-center justify-between gap-4">
            <button
                type="submit"
                disabled={isPending}
                className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60">
                {isPending ? "Saving..." : "Save Logging"}
            </button>
        </div>
    </form>;
}
