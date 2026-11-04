"use client";

import {useState} from "react";
import type {DashboardNotifierEntry, DashboardNotifiersResponse} from "@/lib/notifier-types";

function timestamp(value: number | null | undefined) {
    return value ? `${new Date(value).toLocaleString("en-GB", {timeZone: "UTC"})} UTC` : "Not recorded yet";
}

export default function NotifierDiagnosticsPanel({guildId, entry}: {guildId: string; entry: DashboardNotifierEntry}) {
    const [diagnostics, setDiagnostics] = useState(entry.diagnostics);
    const [pending, setPending] = useState<"refresh" | "test" | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);

    function applyStatus(data: DashboardNotifiersResponse) {
        const updated = data.sections.flatMap(section => section.entries).find(value => value.type === entry.type
            && value.targetValue === entry.targetValue && value.channelId === entry.channelId);
        if (!updated) throw new Error("This notifier changed or was removed. Refresh the page.");
        setDiagnostics(updated.diagnostics);
    }

    async function refresh() {
        const response = await fetch(`/api/dashboard/guilds/${guildId}/notifiers`, {cache: "no-store"});
        const payload = await response.json();
        if (!response.ok) throw new Error(payload.message ?? "Diagnostics could not be refreshed.");
        applyStatus(payload);
    }

    async function run(action: "refresh" | "test") {
        setPending(action);
        setError(null);
        setNotice(null);
        try {
            if (action === "refresh") {
                await refresh();
                setNotice("Diagnostics refreshed.");
            } else {
                const response = await fetch(`/api/dashboard/guilds/${guildId}/notifiers/${encodeURIComponent(entry.type)}/test`, {
                    method: "POST", headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({target: entry.targetValue, discordChannelId: entry.channelId})
                });
                const payload = await response.json();
                if (!response.ok) {
                    await refresh().catch(() => {});
                    throw new Error(payload.message ?? "The test could not complete.");
                }
                applyStatus(payload);
                setNotice("Test delivered without pings. Wait 30 seconds before testing again in this server.");
            }
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : "Diagnostics could not be loaded.");
        } finally {
            setPending(null);
        }
    }

    const times = [
        [diagnostics?.eventDriven ? "Last live event received" : "Last check attempted", diagnostics?.lastCheckAt],
        ...diagnostics?.eventDriven ? [] : [["Last successful check", diagnostics?.lastSuccessfulCheckAt]],
        ["Last real delivery", diagnostics?.lastDeliveredAt],
        ["Last manual test", diagnostics?.lastTestAt]
    ] as [string, number | null | undefined][];

    return <section className="mt-4 space-y-3 border-t border-slate-800 pt-4" aria-label="Notifier diagnostics">
        <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 className="text-sm font-semibold text-white">Delivery diagnostics</h4>
            <span className={`text-xs ${diagnostics?.channelProblem ? "text-amber-200" : "text-slate-300"}`}>
                {diagnostics?.channelProblem ? "Channel needs attention" : diagnostics ? "Channel permissions checked" : "Awaiting diagnostics"}
            </span>
        </div>
        <dl className="grid gap-3 text-xs sm:grid-cols-2">
            {times.map(([label, value]) => <div key={label}><dt className="text-slate-400">{label}</dt>
                <dd className="mt-1 text-slate-200">{timestamp(value)}</dd></div>)}
        </dl>
        {diagnostics?.lastDeliveredItem ? <p className="break-all text-xs text-slate-400">Last delivered item: {diagnostics.lastDeliveredItem}</p> : null}
        {diagnostics?.channelProblem ? <p className="text-sm text-amber-200">{diagnostics.channelProblem}</p> : null}
        {diagnostics?.checkError ? <p className="text-sm text-amber-200">Feed check ({timestamp(diagnostics.checkErrorAt)}): {diagnostics.checkError}</p> : null}
        {diagnostics?.deliveryError ? <p className="text-sm text-amber-200">Delivery ({timestamp(diagnostics.deliveryErrorAt)}): {diagnostics.deliveryError}</p> : null}
        {diagnostics?.lastTestAt ? <p className={`text-xs ${diagnostics.testError ? "text-amber-200" : "text-emerald-300"}`}>
            Last test: {diagnostics.testError ?? "Delivered successfully"}</p> : null}
        <p className="text-xs text-slate-500">{diagnostics?.eventDriven
            ? "Twitch is event-driven. No live event yet does not mean the notifier has failed."
            : "A successful check can have no new items. Delivery times require a confirmed Discord send."}
            {" "}The test sends a fixed message here without pings and checks Discord delivery only.</p>
        <div className="flex flex-wrap gap-2">
            {(["refresh", "test"] as const).map(action => <button key={action} type="button" disabled={pending !== null} onClick={() => run(action)}
                className="border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-semibold text-slate-200 hover:bg-slate-800 disabled:opacity-50">
                {pending === action ? action === "test" ? "Sending test…" : "Refreshing…" : action === "test" ? "Send test (no pings)" : "Refresh diagnostics"}
            </button>)}
        </div>
        {error ? <p role="alert" className="text-sm text-red-200">{error}</p> : null}
        {notice ? <p role="status" className="text-sm text-emerald-200">{notice}</p> : null}
    </section>;
}
