"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardModmailSettings} from "@/lib/dashboard-api";
import GuildRoleSelect from "@/components/GuildRoleSelect";

interface ModmailSettingsFormProps {
    guildId: string;
    initialSettings: DashboardModmailSettings;
}

export default function ModmailSettingsForm({guildId, initialSettings}: ModmailSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        moderatorRoleIds: initialSettings.moderatorRoleIds
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateRoles(values: string[]) {
        setSettings(current => ({
            ...current,
            moderatorRoleIds: values
        }));
    }

    function updateMessage(value: string) {
        setSettings(current => ({
            ...current,
            ticketCreatedMessage: value
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/modmail`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    moderatorRoleIds: settings.moderatorRoleIds,
                    ticketCreatedMessage: settings.ticketCreatedMessage
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save modmail settings.");
                return;
            }

            const updated = await response.json() as DashboardModmailSettings;
            setSettings({
                ...updated,
                moderatorRoleIds: updated.moderatorRoleIds
            });
            setSuccess("Modmail settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <GuildRoleSelect
                id="moderator-roles"
                guildId={guildId}
                value=""
                onChange={() => {}}
                multiple
                values={settings.moderatorRoleIds}
                onValuesChange={updateRoles}
                label="Moderator Roles"
                description="Members with any of these roles can manage modmail tickets."
            />
        </section>

        <label id="ticket-created-message" className="block border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <p className="text-sm font-semibold text-white">Ticket Created Message</p>
            <p className="mt-1 text-sm text-slate-400">Optional extra message sent when a ticket is opened.</p>
            <textarea
                value={settings.ticketCreatedMessage}
                onChange={event => updateMessage(event.target.value)}
                rows={5}
                className="mt-4 w-full resize-y border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-400"
                placeholder="Leave blank to disable the extra message."
            />
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
                {isPending ? "Saving..." : "Save Modmail"}
            </button>
        </div>
    </form>;
}
