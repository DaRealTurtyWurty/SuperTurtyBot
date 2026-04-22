"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import {useDashboardUnsavedChanges} from "@/components/DashboardNavigationGuard";
import type {DashboardSuggestionsSettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface SuggestionsSettingsFormProps {
    guildId: string;
    initialSettings: DashboardSuggestionsSettings;
}

export default function SuggestionsSettingsForm({guildId, initialSettings}: SuggestionsSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        suggestionsChannelId: initialSettings.suggestionsChannelId ?? ""
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();
    const {markSaved} = useDashboardUnsavedChanges(settings);

    function updateChannel(value: string) {
        setSettings(current => ({
            ...current,
            suggestionsChannelId: value
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/config/suggestions`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    suggestionsChannelId: settings.suggestionsChannelId.trim() || null
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save suggestions settings.");
                return;
            }

            const updated = await response.json() as DashboardSuggestionsSettings;
            const nextSettings = {
                ...updated,
                suggestionsChannelId: updated.suggestionsChannelId ?? ""
            };
            setSettings(nextSettings);
            markSaved(nextSettings);
            setSuccess("Suggestions settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildChannelSelect
            id="suggestions-channel"
            guildId={guildId}
            value={settings.suggestionsChannelId}
            onChange={updateChannel}
            label="Suggestions Channel"
            description="Members can send suggestions in this text channel. Leave blank to disable."
            placeholder="Select a channel"
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
                {isPending ? "Saving..." : "Save Suggestions"}
            </button>
        </div>
    </form>;
}
