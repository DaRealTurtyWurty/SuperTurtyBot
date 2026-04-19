"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import type {DashboardThreadSettings} from "@/lib/dashboard-api";

interface ThreadSettingsFormProps {
    guildId: string;
    initialSettings: DashboardThreadSettings;
}

const ALLOWED_CHANNEL_TYPES = ["text"];

export default function ThreadSettingsForm({guildId, initialSettings}: ThreadSettingsFormProps) {
    const [settings, setSettings] = useState(initialSettings);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateChannels(autoThreadChannelIds: string[]) {
        setSettings(current => ({
            ...current,
            autoThreadChannelIds
        }));
    }

    function updateBoolean(shouldModeratorsJoinThreads: boolean) {
        setSettings(current => ({
            ...current,
            shouldModeratorsJoinThreads
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/threads`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    shouldModeratorsJoinThreads: settings.shouldModeratorsJoinThreads,
                    autoThreadChannelIds: settings.autoThreadChannelIds
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save thread settings.");
                return;
            }

            const updated = await response.json() as DashboardThreadSettings;
            setSettings(updated);
            setSuccess("Thread settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <label id="moderators-join-threads" className="block border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <p className="text-sm font-semibold text-white">Moderators Join Threads</p>
                    <p className="mt-1 text-sm text-slate-400">
                        Ping server owner and thread-manage staff when a new thread is revealed.
                    </p>
                </div>
                <input
                    type="checkbox"
                    checked={settings.shouldModeratorsJoinThreads}
                    onChange={event => updateBoolean(event.target.checked)}
                    className="h-5 w-5 accent-sky-400"
                />
            </div>
        </label>

        <GuildChannelSelect
            id="auto-thread-channels"
            guildId={guildId}
            value=""
            onChange={() => undefined}
            values={settings.autoThreadChannelIds}
            onValuesChange={updateChannels}
            multiple
            allowTypes={ALLOWED_CHANNEL_TYPES}
            label="Auto Thread Channels"
            description="New messages in these text channels create a thread automatically."
            placeholder="Select channels"
        />

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
                {isPending ? "Saving..." : "Save Thread Settings"}
            </button>
        </div>
    </form>;
}
