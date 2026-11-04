"use client";

import {useState} from "react";
import type {DashboardConfigHistory} from "@/lib/dashboard-api";

export default function ConfigHistoryPanel({guildId, initialData}: {guildId: string; initialData: DashboardConfigHistory}) {
    const [data, setData] = useState(initialData);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function load(page: number) {
        setPending(true);
        setError(null);
        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/config-history?page=${page}`, {cache: "no-store"});
            const payload = await response.json();
            if (!response.ok) throw new Error(payload.message ?? "Settings history could not be loaded.");
            setData(payload);
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : "Settings history could not be loaded.");
        } finally {
            setPending(false);
        }
    }

    return <div className="space-y-6">
        <header className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-2">
                <h2 className="text-2xl font-semibold text-white">Settings history</h2>
                <p className="max-w-3xl text-sm text-slate-400">Review who changed server settings, when they changed, and the before/after values.
                    Includes dashboard, command, and automatic changes recorded since this feature was enabled. History is kept for 90 days.</p>
            </div>
            <button type="button" onClick={() => load(1)} disabled={pending}
                className="border border-slate-700 px-4 py-2 text-sm text-slate-200 hover:bg-slate-800 disabled:opacity-50">{pending ? "Loading…" : "Refresh history"}</button>
        </header>
        {error ? <p role="alert" className="border border-red-500/30 bg-red-500/10 p-4 text-red-100">{error}</p> : null}
        {!data.entries.length ? <p className="border border-slate-800 bg-slate-950/60 p-6 text-slate-400">No settings changes recorded on this page.</p> : null}
        <div className="space-y-4" aria-busy={pending}>
            {data.entries.map(entry => <article key={entry.id} className="border border-slate-800 bg-slate-950/60 p-5">
                <div className="flex flex-wrap justify-between gap-3 text-sm">
                    <div><p className="font-semibold text-white">{entry.area === "guildData" ? "Server settings" : entry.area.replace(/Notifier$/, " notifier")}</p>
                        <p className="mt-1 text-slate-400">{entry.actorId ? <a href={`https://discord.com/users/${entry.actorId}`} target="_blank" rel="noreferrer"
                            className="text-sky-200 hover:underline">Discord user {entry.actorId}</a> : entry.source === "system" ? "Automatic update" : "Deleted account"}
                            {" · "}{entry.source}</p></div>
                    <time dateTime={new Date(entry.createdAt).toISOString()} className="text-xs text-slate-400">
                        {new Date(entry.createdAt).toLocaleString("en-GB", {timeZone: "UTC"})} UTC
                    </time>
                </div>
                <p className="mt-2 break-all font-mono text-xs text-slate-500">{entry.action}</p>
                <div className="mt-4 overflow-x-auto">
                    <table className="w-full table-fixed text-left text-sm">
                        <thead><tr className="border-b border-slate-800 text-slate-400">
                            <th className="w-1/4 p-2 font-medium">Setting</th><th className="p-2 font-medium">Before</th><th className="p-2 font-medium">After</th>
                        </tr></thead>
                        <tbody>{entry.changes.map(change => <tr key={change.field} className="border-b border-slate-800/60 align-top">
                            <th scope="row" className="break-words p-2 font-mono text-xs font-medium text-sky-200">{change.field}</th>
                            <td className="p-2"><pre className="max-h-64 overflow-auto whitespace-pre-wrap break-all text-xs text-slate-400">{change.before}</pre></td>
                            <td className="p-2"><pre className="max-h-64 overflow-auto whitespace-pre-wrap break-all text-xs text-slate-100">{change.after}</pre></td>
                        </tr>)}</tbody>
                    </table>
                </div>
            </article>)}
        </div>
        <nav aria-label="Settings history pages" className="flex items-center justify-between gap-3 text-sm text-slate-300">
            <button type="button" disabled={pending || data.page <= 1} onClick={() => load(data.page - 1)} className="border border-slate-700 px-3 py-2 disabled:opacity-40">Previous</button>
            <span>Page {data.page}</span>
            <button type="button" disabled={pending || !data.hasMore} onClick={() => load(data.page + 1)} className="border border-slate-700 px-3 py-2 disabled:opacity-40">Next</button>
        </nav>
    </div>;
}
