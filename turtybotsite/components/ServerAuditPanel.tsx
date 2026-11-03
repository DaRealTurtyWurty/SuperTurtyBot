"use client";

import Link from "next/link";
import {useState} from "react";
import {FaArrowRotateRight, FaClipboardCheck} from "react-icons/fa6";
import type {DashboardServerAudit} from "@/lib/dashboard-api";

interface ServerAuditPanelProps {
    guildId: string;
    initialReport: DashboardServerAudit | null;
}

const SECTIONS = ["Permissions", "Settings", "Notifiers", "Health"];
const LINK_STYLE = "inline-flex items-center border border-slate-700 px-3 py-2 text-sm font-medium text-sky-200 transition hover:border-sky-400 hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-sky-400";

export default function ServerAuditPanel({guildId, initialReport}: ServerAuditPanelProps) {
    const [report, setReport] = useState(initialReport);
    const [running, setRunning] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function runAudit() {
        setRunning(true);
        setError(null);
        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/audit`, {method: "POST"});
            const payload = await response.json() as {report?: DashboardServerAudit; message?: string};
            if (!response.ok || !payload.report) {
                throw new Error(payload.message ?? "The audit did not complete. Try again shortly.");
            }
            setReport(payload.report);
        } catch (error) {
            setError(error instanceof Error ? error.message : "The audit did not complete. Try again shortly.");
        } finally {
            setRunning(false);
        }
    }

    const healthChecks = report?.healthChecks ?? [];
    const healthSummary = healthChecks.length === 0 ? "Check details unavailable" :
        healthChecks.some(check => check.status === "failed") ? "Needs attention" :
            healthChecks.some(check => check.status === "skipped") ? "Some checks skipped" : "All checks passed";
    const incomplete = report?.findings.some(finding => finding.message.startsWith("INCOMPLETE:"));

    return <div className="space-y-8" aria-busy={running}>
        <header className="flex flex-wrap items-start justify-between gap-4">
            <div className="max-w-2xl space-y-2">
                <h2 className="flex items-center gap-3 text-3xl font-bold tracking-tight">
                    <FaClipboardCheck className="text-sky-300" aria-hidden /> Server audit
                </h2>
                <p className="text-sm leading-6 text-slate-400">
                    Review what needs attention, open the relevant settings to fix it, then run the audit again to check your changes.
                </p>
            </div>
            <button type="button" onClick={runAudit} disabled={running}
                className="inline-flex items-center gap-2 border border-sky-400 bg-sky-400 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-400 disabled:cursor-wait disabled:opacity-60">
                <FaArrowRotateRight aria-hidden /> {running ? "Checking server..." : report ? "Run audit again" : "Run server audit"}
            </button>
        </header>

        {error && <p role="alert" className="border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-100">{error}</p>}
        <div role="status" aria-live="polite" className="sr-only">
            {running ? "Audit running. This may take a few seconds." : report ? `Audit complete. ${report.findings.length} findings.` : "No saved audit."}
        </div>

        {!report ? <section className="border border-dashed border-slate-700 p-8">
            <h3 className="text-lg font-semibold">No audit yet</h3>
            <p className="mt-2 max-w-xl text-sm leading-6 text-slate-400">
                Run an audit here or use /serveraudit in Discord. The latest completed report will appear on this page.
            </p>
        </section> : <>
            <section className={`border-l-4 px-5 py-4 ${report.findings.length ? "border-amber-400 bg-amber-400/5" : "border-sky-400 bg-sky-400/5"}`}>
                <h3 className="text-xl font-semibold">
                    {report.findings.length ? `${report.findings.length} finding${report.findings.length === 1 ? "" : "s"} to review` : "No issues found in completed checks"}
                </h3>
                <p className="mt-2 text-sm text-slate-400">
                    Last checked <time dateTime={new Date(report.generatedAtMs).toISOString()}>{new Date(report.generatedAtMs).toUTCString()}</time>.
                    {incomplete ? " Some checks could not finish. See the findings below." : " Check coverage is listed below."}
                </p>
                <p className="mt-1 text-sm text-slate-400">This is a saved report. Settings changes appear after you run the audit again.</p>
            </section>

            {SECTIONS.map(section => {
                const findings = report.findings.filter(finding => finding.section === section);
                return <section key={section} aria-label={section} className="space-y-3">
                    <h3 className="flex items-center gap-3 text-lg font-semibold">
                        {section}<span className={`text-sm font-normal ${findings.length ? "text-amber-200" : "text-slate-500"}`}>
                            {section === "Health" ? healthSummary : findings.length ? `${findings.length} to review` : "No findings"}
                        </span>
                    </h3>
                    {section === "Health" && (healthChecks.length ? <ul className="divide-y divide-slate-800 border-y border-slate-800">
                        {healthChecks.map(check => <li key={check.name} className="flex items-start justify-between gap-4 py-4">
                            <div className="min-w-0 space-y-1">
                                <p className="text-sm font-medium text-slate-200">{check.name}</p>
                                <p className="text-sm leading-6 text-slate-400">{check.detail}</p>
                            </div>
                            <span className={`shrink-0 text-sm font-medium ${check.status === "passed" ? "text-sky-300" : check.status === "failed" ? "text-red-300" : "text-amber-200"}`}>
                                {check.status === "passed" ? "Passed" : check.status === "failed" ? "Failed" : "Skipped"}
                            </span>
                        </li>)}
                    </ul> : <p className="text-sm leading-6 text-slate-400">
                        This saved report does not include individual health check results. Run the audit again with the updated bot to see which checks passed, failed, or were skipped.
                    </p>)}
                    {findings.length > 0 && <ul className="divide-y divide-slate-800 border-y border-slate-800">
                        {findings.map((finding, index) => <li key={index} className="flex flex-col items-start justify-between gap-4 py-5 sm:flex-row">
                            <div className="min-w-0 max-w-2xl flex-1 space-y-2">
                                <p className="break-words text-sm leading-6 text-slate-200">{finding.message}</p>
                                {!finding.settingsPage && <p className="text-sm text-slate-500">
                                    {section === "Permissions" ? "A server administrator can update bot permissions and role order in Discord." :
                                        section === "Health" ? "Ask the bot operator to review the service configuration and logs." :
                                            "Review this configuration with a server administrator or bot operator."}
                                </p>}
                            </div>
                            {finding.settingsPage ? <Link href={`/dashboard/${guildId}/${finding.settingsPage}`} className={LINK_STYLE}>
                                Review settings
                            </Link> : section === "Permissions" ? <a href={`https://discord.com/channels/${guildId}`} target="_blank" rel="noopener noreferrer" className={LINK_STYLE}>
                                Open Discord
                            </a> : null}
                        </li>)}
                    </ul>}
                </section>;
            })}

            <details className="border-t border-slate-800 pt-5" open>
                <summary className="cursor-pointer text-sm font-semibold text-slate-300">Check coverage and limitations</summary>
                <ul className="mt-4 list-disc space-y-2 pl-5 text-sm leading-6 text-slate-400">
                    {report.notes.map((note, index) => <li key={index}>{note}</li>)}
                </ul>
            </details>
        </>}
    </div>;
}
