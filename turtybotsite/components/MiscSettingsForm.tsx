"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardMiscSettings} from "@/lib/dashboard-api";
import GuildRoleSelect from "@/components/GuildRoleSelect";

interface MiscSettingsFormProps {
    guildId: string;
    initialSettings: DashboardMiscSettings;
}

export default function MiscSettingsForm({guildId, initialSettings}: MiscSettingsFormProps) {
    const [settings, setSettings] = useState(initialSettings);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateBoolean(key: keyof DashboardMiscSettings, value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updatePatronRole(value: string) {
        setSettings(current => ({
            ...current,
            patronRoleId: value
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/misc`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(settings)
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save misc settings.");
                return;
            }

            const updated = await response.json() as DashboardMiscSettings;
            setSettings(updated);
            setSuccess("Misc settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildRoleSelect
            guildId={guildId}
            value={settings.patronRoleId ?? ""}
            onChange={updatePatronRole}
            label="Patron Role"
            description="Members with this role count as patrons in leaderboard styling."
            placeholder="Select a role"
        />

        <div className="grid gap-4 lg:grid-cols-3">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Create Gists</p>
                        <p className="mt-1 text-sm text-slate-400">Let bot create gists when it needs to share code.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.shouldCreateGists}
                        onChange={event => updateBoolean("shouldCreateGists", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Startup Message</p>
                        <p className="mt-1 text-sm text-slate-400">Send startup ping when bot comes online.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.shouldSendStartupMessage}
                        onChange={event => updateBoolean("shouldSendStartupMessage", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Send Changelog</p>
                        <p className="mt-1 text-sm text-slate-400">Include changelog in startup message.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.shouldSendChangelog}
                        onChange={event => updateBoolean("shouldSendChangelog", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Sticky Roles</p>
                        <p className="mt-1 text-sm text-slate-400">Restore roles when members rejoin after leaving.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.stickyRolesEnabled}
                        onChange={event => updateBoolean("stickyRolesEnabled", event.target.checked)}
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
                {isPending ? "Saving..." : "Save Misc Settings"}
            </button>
        </div>
    </form>;
}
