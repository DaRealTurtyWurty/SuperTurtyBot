"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardBirthdaySettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface BirthdaySettingsFormProps {
    guildId: string;
    initialSettings: DashboardBirthdaySettings;
}

export default function BirthdaySettingsForm({guildId, initialSettings}: BirthdaySettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        birthdayChannelId: initialSettings.birthdayChannelId ?? ""
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateBoolean(value: boolean) {
        setSettings(current => ({
            ...current,
            announceBirthdays: value
        }));
    }

    function updateChannel(value: string) {
        setSettings(current => ({
            ...current,
            birthdayChannelId: value
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/birthday`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    birthdayChannelId: settings.birthdayChannelId.trim() || null,
                    announceBirthdays: settings.announceBirthdays
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save birthday settings.");
                return;
            }

            const updated = await response.json() as DashboardBirthdaySettings;
            setSettings({
                ...updated,
                birthdayChannelId: updated.birthdayChannelId ?? ""
            });
            setSuccess("Birthday settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildChannelSelect
            guildId={guildId}
            value={settings.birthdayChannelId}
            onChange={updateChannel}
            label="Birthday Channel"
            description="Birthday announcements will be sent here."
            placeholder="Select a channel"
        />

        <label className="block w-full border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex items-center justify-between gap-4">
                <div className="min-w-0">
                    <p className="text-sm font-semibold text-white">Announce Birthdays</p>
                    <p className="mt-1 text-sm text-slate-400">Turn birthday announcements on or off for this server.</p>
                </div>
                <input
                    type="checkbox"
                    checked={settings.announceBirthdays}
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
                {isPending ? "Saving..." : "Save Birthday"}
            </button>
        </div>
    </form>;
}
