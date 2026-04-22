"use client";

import type {FormEvent} from "react";
import {useEffect, useRef, useState, useTransition} from "react";
import Image from "next/image";
import Link from "next/link";
import {useDashboardUnsavedChanges} from "@/components/DashboardNavigationGuard";
import DashboardNumberInput from "@/components/DashboardNumberInput";
import type {
    DashboardWarningRecord,
    DashboardWarningSanction,
    DashboardWarningsResponse,
    DashboardWarningsSettings
} from "@/lib/dashboard-api";

interface WarningsSettingsFormProps {
    guildId: string;
    initialData: DashboardWarningsResponse;
}

function formatTimestamp(value: number) {
    if (!value) {
        return "Unknown";
    }

    return new Date(value).toLocaleString();
}

function formatExpiryStatus(warning: DashboardWarningRecord) {
    if (warning.active) {
        if (!warning.expiresAt) {
            return "Active forever";
        }

        return `Active until ${formatTimestamp(warning.expiresAt)}`;
    }

    if (!warning.expiresAt) {
        return "Expired";
    }

    return `Expired ${formatTimestamp(warning.expiresAt)}`;
}

function WarningAvatar({name, avatarUrl}: {name: string; avatarUrl: string | null}) {
    const fallback = name.trim().charAt(0).toUpperCase() || "?";

    if (avatarUrl) {
        return <Image
            src={avatarUrl}
            alt={name}
            width={48}
            height={48}
            className="h-12 w-12 rounded-full border border-slate-700 object-cover"
        />;
    }

    return <div className="flex h-12 w-12 items-center justify-center rounded-full border border-slate-700 bg-slate-900 text-sm font-semibold text-slate-200">
        {fallback}
    </div>;
}

function WarningList({
    guildId,
    warnings,
    onRequestRemoveWarning,
    removingWarningUuid
}: {
    guildId: string;
    warnings: DashboardWarningRecord[];
    onRequestRemoveWarning: (warning: DashboardWarningRecord) => void;
    removingWarningUuid: string | null;
}) {
    if (!warnings.length) {
        return <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
            No warnings have been recorded in this guild yet.
        </div>;
    }

    return <div className="space-y-3">
        {warnings.map(warning => <article key={warning.uuid} className="border border-slate-800/80 bg-slate-950/60 p-4">
            <div className="flex gap-3">
                <WarningAvatar name={warning.userDisplayName} avatarUrl={warning.userAvatarUrl} />
                <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="min-w-0">
                            <p className="truncate text-sm font-semibold text-white">
                                <Link
                                    href={`/dashboard/${guildId}/warnings/user/${warning.userId}`}
                                    className="transition hover:text-sky-300"
                                >
                                    {warning.userDisplayName}
                                </Link>
                            </p>
                            <p className="mt-1 text-xs text-slate-500">
                                {warning.userId} · Warned by {warning.warnerDisplayName} ({warning.warnerId}) · {formatTimestamp(warning.warnedAt)}
                            </p>
                            <p className={`mt-2 text-xs font-semibold uppercase tracking-[0.16em] ${warning.active ? "text-amber-300" : "text-slate-500"}`}>
                                {formatExpiryStatus(warning)}
                            </p>
                        </div>
                        <Link
                            href={`/dashboard/${guildId}/warnings/${warning.uuid}`}
                            className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] text-slate-300 transition hover:border-slate-500 hover:text-white"
                        >
                            {warning.uuid}
                        </Link>
                    </div>
                    <p className="mt-3 whitespace-pre-wrap text-sm text-slate-200">
                        {warning.reason?.trim() ? warning.reason : "No reason provided."}
                    </p>
                    <div className="mt-4 flex items-center justify-end">
                        <button
                            type="button"
                            onClick={() => onRequestRemoveWarning(warning)}
                            disabled={removingWarningUuid === warning.uuid}
                            className="border border-red-500/40 bg-red-500/10 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-red-100 transition hover:border-red-400 hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {removingWarningUuid === warning.uuid ? "Removing..." : "Remove warning"}
                        </button>
                    </div>
                </div>
            </div>
        </article>)}
    </div>;
}

export default function WarningsSettingsForm({guildId, initialData}: WarningsSettingsFormProps) {
    const [settings, setSettings] = useState<DashboardWarningsSettings>(initialData.settings);
    const [warnings, setWarnings] = useState<DashboardWarningRecord[]>(initialData.warnings);
    const [warningXpPercentage, setWarningXpPercentage] = useState(initialData.settings.warningXpPercentage.toString());
    const [warningEconomyPercentage, setWarningEconomyPercentage] = useState(initialData.settings.warningEconomyPercentage.toString());
    const [warningExpiryDays, setWarningExpiryDays] = useState(initialData.settings.warningExpiryDays.toString());
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [removingWarningUuid, setRemovingWarningUuid] = useState<string | null>(null);
    const [warningPendingRemoval, setWarningPendingRemoval] = useState<DashboardWarningRecord | null>(null);
    const [isPending, startTransition] = useTransition();
    const cancelRemovalButtonRef = useRef<HTMLButtonElement | null>(null);
    const confirmRemovalButtonRef = useRef<HTMLButtonElement | null>(null);
    const modalLastFocusedElementRef = useRef<HTMLElement | null>(null);
    const {markSaved} = useDashboardUnsavedChanges({
        settings,
        warningExpiryDays,
        warningEconomyPercentage,
        warningXpPercentage
    });

    function updateBoolean(value: boolean) {
        setSettings(current => ({
            ...current,
            warningsModeratorOnly: value
        }));
    }

    function updateSanction(id: string, patch: Partial<DashboardWarningSanction>) {
        setSettings(current => ({
            ...current,
            sanctions: current.sanctions.map(sanction => sanction.id === id ? {
                ...sanction,
                ...patch
            } : sanction)
        }));
    }

    function addSanction() {
        setSettings(current => ({
            ...current,
            sanctions: [
                ...current.sanctions,
                {
                    id: crypto.randomUUID(),
                    type: "timeout",
                    warningCount: Math.max(1, current.sanctions.length + 1),
                    durationMinutes: 60,
                    deleteMessageDays: 0
                }
            ]
        }));
    }

    function removeSanction(id: string) {
        setSettings(current => ({
            ...current,
            sanctions: current.sanctions.filter(sanction => sanction.id !== id)
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/warnings`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    warningsModeratorOnly: settings.warningsModeratorOnly,
                    warningExpiryDays: Number.parseInt(warningExpiryDays, 10) || 0,
                    warningXpPercentage: Number.parseFloat(warningXpPercentage) || 0,
                    warningEconomyPercentage: Number.parseFloat(warningEconomyPercentage) || 0,
                    sanctions: settings.sanctions.map(sanction => ({
                        ...sanction,
                        warningCount: Number.isFinite(sanction.warningCount) ? sanction.warningCount : 1,
                        durationMinutes: Number.isFinite(sanction.durationMinutes) ? sanction.durationMinutes : 0,
                        deleteMessageDays: Number.isFinite(sanction.deleteMessageDays) ? sanction.deleteMessageDays : 0
                    }))
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save warnings settings.");
                return;
            }

            const updated = await response.json() as DashboardWarningsResponse;
            setSettings(updated.settings);
            setWarnings(updated.warnings);
            setWarningExpiryDays(updated.settings.warningExpiryDays.toString());
            setWarningXpPercentage(updated.settings.warningXpPercentage.toString());
            setWarningEconomyPercentage(updated.settings.warningEconomyPercentage.toString());
            markSaved({
                settings: updated.settings,
                warningExpiryDays: updated.settings.warningExpiryDays.toString(),
                warningEconomyPercentage: updated.settings.warningEconomyPercentage.toString(),
                warningXpPercentage: updated.settings.warningXpPercentage.toString()
            });
            setSuccess("Warnings settings saved.");
        });
    }

    async function removeWarning(warning: DashboardWarningRecord) {
        setError(null);
        setSuccess(null);
        setRemovingWarningUuid(warning.uuid);
        setWarningPendingRemoval(null);

        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/warnings/${encodeURIComponent(warning.uuid)}`, {
                method: "DELETE"
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to remove warning.");
                return;
            }

            const updated = await response.json() as DashboardWarningsResponse;
            setSettings(updated.settings);
            setWarnings(updated.warnings);
            setSuccess("Warning removed.");
        } finally {
            setRemovingWarningUuid(null);
        }
    }

    function requestRemoveWarning(warning: DashboardWarningRecord) {
        setError(null);
        setSuccess(null);
        modalLastFocusedElementRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        setWarningPendingRemoval(warning);
    }

    useEffect(() => {
        if (!warningPendingRemoval) {
            modalLastFocusedElementRef.current?.focus();
            modalLastFocusedElementRef.current = null;
            return;
        }

        const timeout = window.setTimeout(() => {
            cancelRemovalButtonRef.current?.focus();
        }, 0);

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === "Escape") {
                event.preventDefault();
                setWarningPendingRemoval(null);
                return;
            }

            if (event.key !== "Tab") {
                return;
            }

            const focusable = [cancelRemovalButtonRef.current, confirmRemovalButtonRef.current].filter(
                (element): element is HTMLButtonElement => Boolean(element)
            );
            if (!focusable.length) {
                return;
            }

            const currentIndex = focusable.indexOf(document.activeElement as HTMLButtonElement);
            const nextIndex = event.shiftKey
                ? (currentIndex <= 0 ? focusable.length - 1 : currentIndex - 1)
                : (currentIndex === focusable.length - 1 ? 0 : currentIndex + 1);

            event.preventDefault();
            focusable[nextIndex]?.focus();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.clearTimeout(timeout);
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [warningPendingRemoval]);

    return <div className="space-y-6">
        <form onSubmit={onSubmit} className="space-y-5">
            <section id="moderator-only" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Moderator Only</p>
                        <p className="mt-1 text-sm text-slate-400">If enabled, only members with ban permissions can view warning commands.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.warningsModeratorOnly}
                        onChange={event => updateBoolean(event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>

                <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <label id="warning-expiry-days" className="border border-slate-800/80 bg-slate-950/50 p-4 scroll-mt-24">
                        <p className="text-sm font-semibold text-white">Rolling Warning Window (Days)</p>
                        <p className="mt-1 text-sm text-slate-400">
                            How many days warnings should count toward future sanction thresholds. Existing sanctions are not removed when warnings expire. Use <code className="rounded border border-slate-700 bg-slate-900 px-1 py-0.5 text-[0.85em] text-slate-200">0</code> to disable expiry and keep all warnings active.
                        </p>
                        <DashboardNumberInput
                            min={0}
                            max={3650}
                            step={1}
                            value={warningExpiryDays}
                            onChange={setWarningExpiryDays}
                            placeholder="0 days"
                        />
                    </label>

                    <label id="xp-loss-percentage" className="border border-slate-800/80 bg-slate-950/50 p-4 scroll-mt-24">
                        <p className="text-sm font-semibold text-white">XP Loss Percentage</p>
                        <p className="mt-1 text-sm text-slate-400">
                            How much XP to remove when a warning is added. Use <code className="rounded border border-slate-700 bg-slate-900 px-1 py-0.5 text-[0.85em] text-slate-200">0</code> to disable.
                        </p>
                        <DashboardNumberInput
                            min={0}
                            max={100}
                            step={0.1}
                            value={warningXpPercentage}
                            onChange={setWarningXpPercentage}
                            placeholder="0"
                        />
                    </label>

                    <label id="economy-loss-percentage" className="border border-slate-800/80 bg-slate-950/50 p-4 scroll-mt-24">
                        <p className="text-sm font-semibold text-white">Economy Loss Percentage</p>
                        <p className="mt-1 text-sm text-slate-400">
                            How much money to remove when a warning is added. Use <code className="rounded border border-slate-700 bg-slate-900 px-1 py-0.5 text-[0.85em] text-slate-200">0</code> to disable.
                        </p>
                        <DashboardNumberInput
                            min={0}
                            max={100}
                            step={0.1}
                            value={warningEconomyPercentage}
                            onChange={setWarningEconomyPercentage}
                            placeholder="0"
                        />
                    </label>
                </div>

                <div id="sanctions" className="mt-4 border border-slate-800/80 bg-slate-950/50 p-4 scroll-mt-24">
                    <div className="flex flex-wrap items-end justify-between gap-3">
                        <div>
                            <p className="text-sm font-semibold text-white">Warning Sanctions</p>
                            <p className="mt-1 text-sm text-slate-400">
                                Configure what happens when members hit specific warning counts.
                            </p>
                        </div>
                        <button
                            type="button"
                            onClick={addSanction}
                            className="border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-slate-100 transition hover:border-slate-500 hover:bg-slate-800"
                        >
                            Add sanction
                        </button>
                    </div>

                    <div className="mt-4 space-y-3">
                        {settings.sanctions.map((sanction, index) => <div
                            key={sanction.id}
                            className="grid gap-3 border border-slate-800/80 bg-slate-950/70 p-4 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
                        >
                            <label className="space-y-2">
                                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">At warnings</span>
                                <DashboardNumberInput
                                    min={1}
                                    max={100}
                                    step={1}
                                    value={sanction.warningCount.toString()}
                                    onChange={value => updateSanction(sanction.id, {
                                        warningCount: Number.parseInt(value, 10) || 1
                                    })}
                                    placeholder="1"
                                />
                            </label>

                            <label className="space-y-2">
                                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Action</span>
                                <select
                                    value={sanction.type}
                                    onChange={event => updateSanction(sanction.id, {
                                        type: event.target.value as DashboardWarningSanction["type"],
                                        durationMinutes: event.target.value === "timeout" || event.target.value === "tempban"
                                            ? Math.max(1, sanction.durationMinutes || 60)
                                            : 0,
                                        deleteMessageDays: event.target.value === "ban" || event.target.value === "tempban"
                                            ? sanction.deleteMessageDays
                                            : 0
                                    })}
                                    className="w-full border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 outline-none transition focus:border-sky-400"
                                >
                                    <option value="timeout">Timeout</option>
                                    <option value="kick">Kick</option>
                                    <option value="tempban">Temporary ban</option>
                                    <option value="ban">Ban</option>
                                </select>
                            </label>

                            {sanction.type === "timeout" || sanction.type === "tempban" ? <label className="space-y-2">
                                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Duration (minutes)</span>
                                <DashboardNumberInput
                                    min={1}
                                    max={40320}
                                    step={1}
                                    value={sanction.durationMinutes.toString()}
                                    onChange={value => updateSanction(sanction.id, {
                                        durationMinutes: Number.parseInt(value, 10) || 1
                                    })}
                                    placeholder="60"
                                />
                            </label> : sanction.type === "ban" ? <label className="space-y-2">
                                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Delete days</span>
                                <DashboardNumberInput
                                    min={0}
                                    max={7}
                                    step={1}
                                    value={sanction.deleteMessageDays.toString()}
                                    onChange={value => updateSanction(sanction.id, {
                                        deleteMessageDays: Math.max(0, Number.parseInt(value, 10) || 0)
                                    })}
                                    placeholder="0"
                                />
                            </label> : <div className="flex items-end">
                                <p className="text-sm text-slate-500">
                                    Members are removed immediately when this threshold is reached.
                                </p>
                            </div>}

                            {sanction.type === "tempban" ? <label className="space-y-2">
                                <span className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Delete days</span>
                                <DashboardNumberInput
                                    min={0}
                                    max={7}
                                    step={1}
                                    value={sanction.deleteMessageDays.toString()}
                                    onChange={value => updateSanction(sanction.id, {
                                        deleteMessageDays: Math.max(0, Number.parseInt(value, 10) || 0)
                                    })}
                                    placeholder="0"
                                />
                            </label> : null}

                            <div className="flex items-end justify-end">
                                <button
                                    type="button"
                                    onClick={() => removeSanction(sanction.id)}
                                    disabled={settings.sanctions.length <= 1}
                                    className="border border-red-500/40 bg-red-500/10 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-red-100 transition hover:border-red-400 hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    Remove
                                </button>
                            </div>

                            <p className="md:col-span-4 text-xs text-slate-500">
                                Rule {index + 1}: {sanction.type === "timeout"
                                    ? `timeout for ${sanction.durationMinutes} minute${sanction.durationMinutes === 1 ? "" : "s"}`
                                    : sanction.type === "tempban"
                                        ? `temporary ban for ${sanction.durationMinutes} minute${sanction.durationMinutes === 1 ? "" : "s"} and delete ${sanction.deleteMessageDays} day${sanction.deleteMessageDays === 1 ? "" : "s"} of messages`
                                    : sanction.type === "ban"
                                        ? `ban and delete ${sanction.deleteMessageDays} day${sanction.deleteMessageDays === 1 ? "" : "s"} of messages`
                                        : "kick"} at {sanction.warningCount} warning{sanction.warningCount === 1 ? "" : "s"}.
                            </p>
                        </div>)}
                    </div>
                </div>
            </section>

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
                    {isPending ? "Saving..." : "Save Warnings"}
                </button>
            </div>
        </form>

        <section id="recent-warnings" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-sm font-semibold text-white">Recent Warnings</p>
                    <p className="mt-1 text-sm text-slate-400">
                        Showing the latest {warnings.length} recorded warnings. Expired warnings stay visible and stop counting toward future sanction thresholds.
                    </p>
                </div>
                <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Recent history</p>
            </div>

            <div className="mt-4">
                <WarningList guildId={guildId} warnings={warnings} onRequestRemoveWarning={requestRemoveWarning} removingWarningUuid={removingWarningUuid} />
            </div>
        </section>

        {warningPendingRemoval ? <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="warning-remove-title"
            className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 px-4 py-6 backdrop-blur-sm"
            onClick={() => setWarningPendingRemoval(null)}
        >
            <div
                className="w-full max-w-lg border border-slate-700 bg-slate-950 p-5 shadow-2xl shadow-black/40"
                onClick={event => event.stopPropagation()}
            >
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-red-300">Confirm removal</p>
                <h3 id="warning-remove-title" className="mt-2 text-xl font-bold text-white">
                    Remove this warning?
                </h3>
                <p className="mt-3 text-sm text-slate-300">
                    This removes warning <span className="font-mono text-slate-100">{warningPendingRemoval.uuid}</span> from{" "}
                    <span className="font-semibold text-white">{warningPendingRemoval.userDisplayName}</span>.
                </p>
                <p className="mt-2 text-sm text-slate-400">
                    Action cannot be undone.
                </p>
                <div className="mt-6 flex flex-wrap justify-end gap-3">
                    <button
                        type="button"
                        ref={cancelRemovalButtonRef}
                        onClick={() => setWarningPendingRemoval(null)}
                        className="border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-slate-500 hover:bg-slate-800"
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        ref={confirmRemovalButtonRef}
                        disabled={removingWarningUuid === warningPendingRemoval.uuid}
                        onClick={() => void removeWarning(warningPendingRemoval)}
                        className="border border-red-500/40 bg-red-500/10 px-4 py-2 text-sm font-semibold text-red-100 transition hover:border-red-400 hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {removingWarningUuid === warningPendingRemoval.uuid ? "Removing..." : "Remove warning"}
                    </button>
                </div>
            </div>
        </div> : null}
    </div>;
}
