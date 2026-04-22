"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import {useDashboardUnsavedChanges} from "@/components/DashboardNavigationGuard";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import type {DashboardOptInChannelsSettings} from "@/lib/dashboard-api";

const ALLOWED_CHANNEL_TYPES = ["text", "voice", "announcement", "stage", "forum", "media"];

interface OptInChannelsSettingsFormProps {
    guildId: string;
    initialSettings: DashboardOptInChannelsSettings;
}

export default function OptInChannelsSettingsForm({guildId, initialSettings}: OptInChannelsSettingsFormProps) {
    const [settings, setSettings] = useState(initialSettings);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();
    const {markSaved} = useDashboardUnsavedChanges(settings);

    function updateChannels(optInChannelIds: string[]) {
        setSettings(current => ({
            ...current,
            optInChannelIds
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/opt-in-channels`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    optInChannelIds: settings.optInChannelIds
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save opt-in channels.");
                return;
            }

            const updated = await response.json() as DashboardOptInChannelsSettings;
            setSettings(updated);
            markSaved(updated);
            setSuccess("Opt-in channels saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildChannelSelect
            id="opt-in-channels"
            guildId={guildId}
            value=""
            onChange={() => undefined}
            values={settings.optInChannelIds}
            onValuesChange={updateChannels}
            multiple
            allowTypes={ALLOWED_CHANNEL_TYPES}
            label="Opt-In Channels"
            description="Members can use /opt in and /opt out for any channel selected here."
            placeholder="Select channels"
        />

        <div id="selected-channels" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Selected channels</p>
            <p className="mt-2 text-2xl font-bold text-white">{settings.optInChannelIds.length}</p>
            <p className="mt-2 text-sm text-slate-400">
                Only visible guild channels can be selected. Categories and threads stay excluded.
            </p>
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
                {isPending ? "Saving..." : "Save Opt-In Channels"}
            </button>
        </div>
    </form>;
}
