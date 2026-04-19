"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardAiSettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import GuildMemberMultiSelect from "@/components/GuildMemberMultiSelect";

interface AiSettingsFormProps {
    guildId: string;
    initialSettings: DashboardAiSettings;
}

export default function AiSettingsForm({guildId, initialSettings}: AiSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        aiChannelWhitelist: initialSettings.aiChannelWhitelist,
        aiUserBlacklist: initialSettings.aiUserBlacklist
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const aiDisabled = !settings.aiEnabled;

    function updateBoolean(value: boolean) {
        setSettings(current => ({
            ...current,
            aiEnabled: value
        }));
    }

    function updateUserBlacklist(values: string[]) {
        setSettings(current => ({
            ...current,
            aiUserBlacklist: values
        }));
    }

    function updateChannelWhitelist(values: string[]) {
        setSettings(current => ({
            ...current,
            aiChannelWhitelist: values
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/ai`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    aiEnabled: settings.aiEnabled,
                    aiChannelWhitelist: settings.aiChannelWhitelist,
                    aiUserBlacklist: settings.aiUserBlacklist
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save AI settings.");
                return;
            }

            const updated = await response.json() as DashboardAiSettings;
            setSettings({
                ...updated,
                aiChannelWhitelist: updated.aiChannelWhitelist,
                aiUserBlacklist: updated.aiUserBlacklist
            });
            setSuccess("AI settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <label id="enable-ai" className="block border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <p className="text-sm font-semibold text-white">Enable AI</p>
                    <p className="mt-1 text-sm text-slate-400">Allow the bot&apos;s AI responses in this guild.</p>
                </div>
                <input
                    type="checkbox"
                    checked={settings.aiEnabled}
                    onChange={event => updateBoolean(event.target.checked)}
                    className="h-5 w-5 accent-sky-400"
                />
            </div>
        </label>

        <div className="grid gap-4 md:grid-cols-2">
            <GuildChannelSelect
                id="channel-whitelist"
                guildId={guildId}
                value=""
                onChange={() => {}}
                multiple
                values={settings.aiChannelWhitelist}
                onValuesChange={updateChannelWhitelist}
                disabled={aiDisabled}
                label="Channel Whitelist"
                description="Only these text channels can use AI. Leave empty to avoid channel restrictions."
            />

            <GuildMemberMultiSelect
                id="user-blacklist"
                guildId={guildId}
                values={settings.aiUserBlacklist}
                onValuesChange={updateUserBlacklist}
                disabled={aiDisabled}
                label="User Blacklist"
                description="Prevent specific guild members from using AI responses."
                placeholder="Search or paste a user ID"
            />
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
                {isPending ? "Saving..." : "Save AI"}
            </button>
        </div>
    </form>;
}
