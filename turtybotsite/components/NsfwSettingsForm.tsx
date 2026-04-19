"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import type {DashboardNsfwSettings} from "@/lib/dashboard-api";

interface NsfwSettingsFormProps {
    guildId: string;
    initialSettings: DashboardNsfwSettings;
}

const ALLOWED_CHANNEL_TYPES = ["text"];

export default function NsfwSettingsForm({guildId, initialSettings}: NsfwSettingsFormProps) {
    const [settings, setSettings] = useState(initialSettings);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateChannels(nsfwChannelIds: string[]) {
        setSettings(current => ({
            ...current,
            nsfwChannelIds
        }));
    }

    function updateBoolean(artistNsfwFilterEnabled: boolean) {
        setSettings(current => ({
            ...current,
            artistNsfwFilterEnabled
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/nsfw`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    nsfwChannelIds: settings.nsfwChannelIds,
                    artistNsfwFilterEnabled: settings.artistNsfwFilterEnabled
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save NSFW settings.");
                return;
            }

            const updated = await response.json() as DashboardNsfwSettings;
            setSettings(updated);
            setSuccess("NSFW settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildChannelSelect
            id="nsfw-channels"
            guildId={guildId}
            value=""
            onChange={() => undefined}
            values={settings.nsfwChannelIds}
            onValuesChange={updateChannels}
            multiple
            allowTypes={ALLOWED_CHANNEL_TYPES}
            label="NSFW Channels"
            description="Text channels here can run NSFW commands. Threads follow their parent channel."
            placeholder="Select channels"
        />

        <label id="artist-nsfw-filter" className="block border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <p className="text-sm font-semibold text-white">Artist NSFW Filter</p>
                    <p className="mt-1 text-sm text-slate-400">
                        Block artist content when NSFW classifier says it should be filtered.
                    </p>
                </div>
                <input
                    type="checkbox"
                    checked={settings.artistNsfwFilterEnabled}
                    onChange={event => updateBoolean(event.target.checked)}
                    className="h-5 w-5 accent-sky-400"
                />
            </div>
        </label>

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
                {isPending ? "Saving..." : "Save NSFW Settings"}
            </button>
        </div>
    </form>;
}
