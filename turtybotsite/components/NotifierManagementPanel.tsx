"use client";

import type {FormEvent} from "react";
import {useMemo, useState, useTransition} from "react";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import NotifierPingSelect from "@/components/NotifierPingSelect";
import NotifierEntryCard from "@/components/NotifierEntryCard";
import type {
    DashboardNotifierEntry,
    DashboardNotifiersResponse,
    DashboardNotifierMutationRequest
} from "@/lib/notifier-types";
import {getNotifierSectionDefinition, getNotifierTypeDefinition} from "@/lib/notifiers";

export default function NotifierManagementPanel({
    guildId,
    type,
    initialData
}: {
    guildId: string;
    type: string;
    initialData: DashboardNotifiersResponse;
}) {
    const typeInfo = getNotifierTypeDefinition(type);
    const sectionInfo = typeInfo ? getNotifierSectionDefinition(typeInfo.section) : null;
    const [data, setData] = useState(initialData);
    const [target, setTarget] = useState("");
    const [channelId, setChannelId] = useState("");
    const [mention, setMention] = useState("");
    const [editingEntry, setEditingEntry] = useState<DashboardNotifierEntry | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [pendingAction, startTransition] = useTransition();
    const [removingTarget, setRemovingTarget] = useState<string | null>(null);

    const entries = useMemo(
        () => data.sections.flatMap(section => section.entries).filter(entry => entry.type === type),
        [data, type]
    );

    function resetForm() {
        setTarget("");
        setChannelId("");
        setMention("");
    }

    function startEdit(entry: DashboardNotifierEntry) {
        setEditingEntry(entry);
        setTarget(entry.targetValue ?? "");
        setChannelId(entry.channelId);
        setMention(entry.mention ?? "");
        setError(null);
        setSuccess(null);
    }

    function cancelEdit() {
        setEditingEntry(null);
        resetForm();
        setError(null);
        setSuccess(null);
    }

    function getRequestBody(): DashboardNotifierMutationRequest {
        return {
            originalTarget: editingEntry?.targetValue ?? null,
            target: typeInfo?.requiresTarget ? target.trim() : null,
            discordChannelId: channelId ? Number(channelId) : null,
            mention: mention.trim() || null
        };
    }

    async function mutateNotifier(method: "POST" | "PUT" | "DELETE", body: DashboardNotifierMutationRequest) {
        const response = await fetch(`/api/dashboard/guilds/${guildId}/notifiers/${encodeURIComponent(type)}`, {
            method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            const payload = await response.json().catch(() => null) as {message?: string} | null;
            const action = method === "POST" ? "create" : method === "PUT" ? "update" : "remove";
            throw new Error(payload?.message ?? `Failed to ${action} notifier.`);
        }

        return response.json() as Promise<DashboardNotifiersResponse>;
    }

    function onCreate(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (!typeInfo) {
            return;
        }

        const requestBody = getRequestBody();
        if (!requestBody.discordChannelId) {
            setError("Select a Discord channel.");
            return;
        }

        if (!requestBody.mention?.trim()) {
            setError("Select at least one user or role to ping.");
            return;
        }

        setError(null);
        setSuccess(null);
        const isEditing = Boolean(editingEntry);

        startTransition(async () => {
            try {
                const updated = await mutateNotifier(isEditing ? "PUT" : "POST", requestBody);
                setData(updated);
                resetForm();
                setEditingEntry(null);
                setSuccess(isEditing ? `${typeInfo.label} notifier updated.` : `${typeInfo.label} notifier created.`);
            } catch (mutationError) {
                setError(mutationError instanceof Error ? mutationError.message : `Failed to ${isEditing ? "update" : "create"} notifier.`);
            }
        });
    }

    function removeNotifier(entry: DashboardNotifierEntry) {
        setError(null);
        setSuccess(null);
        setRemovingTarget(entry.targetValue);

        startTransition(async () => {
            try {
                const updated = await mutateNotifier("DELETE", {
                    target: entry.targetValue,
                    discordChannelId: null,
                    mention: null
                });
                setData(updated);
                setSuccess(`${typeInfo?.label ?? "Notifier"} removed.`);
            } catch (mutationError) {
                setError(mutationError instanceof Error ? mutationError.message : "Failed to remove notifier.");
            } finally {
                setRemovingTarget(null);
            }
        });
    }

    if (!typeInfo) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Unknown notifier type.
        </div>;
    }

    return <div className="space-y-6">
        <section id="add-notifier" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="space-y-2">
                <h3 className="text-xl font-semibold text-white">Add notifier</h3>
                {editingEntry ? <p className="text-sm font-medium text-sky-200">Editing existing {typeInfo.label.toLowerCase()} notifier.</p> : null}
                <p className="text-sm text-slate-400">{sectionInfo?.description ?? typeInfo.description}</p>
            </div>

            <form onSubmit={onCreate} className="mt-5 space-y-4">
                {typeInfo.requiresTarget ? <label className="block">
                    <span className="text-sm font-semibold text-white">{typeInfo.targetLabel}</span>
                    {typeInfo.targetHelp ? <span className="mt-1 block text-sm text-slate-400">{typeInfo.targetHelp}</span> : null}
                    <input
                        value={target}
                        onChange={event => setTarget(event.target.value)}
                        className="mt-3 w-full border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-400"
                        placeholder={typeInfo.targetPlaceholder ?? ""}
                        required
                    />
                    </label> : null}

                <GuildChannelSelect
                    id="discord-channel"
                    guildId={guildId}
                    value={channelId}
                    onChange={setChannelId}
                    label="Discord channel"
                    description="Where notifications should be posted."
                    placeholder="Select a Discord channel"
                />

                <NotifierPingSelect
                    id="who-to-ping"
                    guildId={guildId}
                    value={mention}
                    onChange={setMention}
                />

                {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">{error}</p> : null}
                {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">{success}</p> : null}

                <div className="flex flex-wrap gap-3">
                    <button
                        type="submit"
                        disabled={pendingAction}
                        className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {pendingAction ? "Saving..." : editingEntry ? "Save changes" : "Add notifier"}
                    </button>
                    {editingEntry ? <button
                        type="button"
                        onClick={cancelEdit}
                        disabled={pendingAction}
                        className="border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-100 transition hover:border-slate-500 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        Cancel edit
                    </button> : null}
                </div>
            </form>
        </section>

        <section id="current-notifiers" className="space-y-3 scroll-mt-24">
            <div className="flex items-end justify-between gap-3">
                <div>
                    <h3 className="text-xl font-semibold text-white">Current notifiers</h3>
                    <p className="text-sm text-slate-400">{entries.length} configured for this type.</p>
                </div>
            </div>

            {entries.length ? <div className="grid gap-4">
                {entries.map(entry => <NotifierEntryCard
                    key={`${entry.type}:${entry.targetValue ?? entry.channelId}:${entry.channelId}`}
                    guildId={guildId}
                    entry={entry}
                    onEdit={() => startEdit(entry)}
                    onRemove={() => removeNotifier(entry)}
                    removeDisabled={pendingAction && removingTarget === entry.targetValue}
                />)}
            </div> : <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No notifiers of this type yet.
            </div>}
        </section>
    </div>;
}
