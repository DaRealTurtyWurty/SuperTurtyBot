"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import {useDashboardUnsavedChanges} from "@/components/DashboardNavigationGuard";
import type {DashboardWelcomeSettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface WelcomeSettingsFormProps {
    guildId: string;
    initialSettings: DashboardWelcomeSettings;
}

export default function WelcomeSettingsForm({guildId, initialSettings}: WelcomeSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        welcomeChannelId: initialSettings.welcomeChannelId ?? ""
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();
    const {markSaved} = useDashboardUnsavedChanges(settings);

    function updateBoolean(key: "shouldAnnounceJoins" | "shouldAnnounceLeaves", value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateChannel(value: string) {
        setSettings(current => ({
            ...current,
            welcomeChannelId: value
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/welcome`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    welcomeChannelId: settings.welcomeChannelId.trim() || null,
                    shouldAnnounceJoins: settings.shouldAnnounceJoins,
                    shouldAnnounceLeaves: settings.shouldAnnounceLeaves
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save welcome settings.");
                return;
            }

            const updated = await response.json() as DashboardWelcomeSettings;
            const nextSettings = {
                ...updated,
                welcomeChannelId: updated.welcomeChannelId ?? ""
            };
            setSettings(nextSettings);
            markSaved(nextSettings);
            setSuccess("Welcome settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildChannelSelect
            id="welcome-channel"
            guildId={guildId}
            value={settings.welcomeChannelId}
            onChange={updateChannel}
            label="Welcome Channel"
            description="Join and leave messages will be sent to this text channel."
            placeholder="Select a channel"
        />

        <div className="grid gap-4 md:grid-cols-2">
            <label id="announce-joins" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Announce Joins</p>
                        <p className="mt-1 text-sm text-slate-400">Post a random welcome message when someone joins.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.shouldAnnounceJoins}
                        onChange={event => updateBoolean("shouldAnnounceJoins", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label id="announce-leaves" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Announce Leaves</p>
                        <p className="mt-1 text-sm text-slate-400">Post a goodbye message when someone leaves.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.shouldAnnounceLeaves}
                        onChange={event => updateBoolean("shouldAnnounceLeaves", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
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
                {isPending ? "Saving..." : "Save Welcome"}
            </button>
        </div>
    </form>;
}
