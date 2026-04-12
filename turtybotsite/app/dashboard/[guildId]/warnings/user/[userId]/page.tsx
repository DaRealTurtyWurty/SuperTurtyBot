import Link from "next/link";
import {redirect} from "next/navigation";
import WarningRecordCard from "@/components/WarningRecordCard";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardUserWarnings, isDashboardApiError} from "@/lib/dashboard-api";

export default async function WarningHistoryPage({
    params
}: {
    params: Promise<{guildId: string; userId: string}>;
}) {
    const session = await requireCurrentSession();
    const {guildId, userId} = await params;
    if (!session.guilds.some(entry => entry.id === guildId)) {
        redirect("/dashboard");
    }

    const history = await fetchDashboardUserWarnings(guildId, userId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!history) {
        return <section className="space-y-4">
            <div>
                <p className="text-sm uppercase tracking-[0.18em] text-slate-500">Warnings</p>
                <h2 className="mt-2 text-3xl font-bold tracking-tight">User warning history</h2>
            </div>
            <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
                The dashboard API could not load this user warning history.
            </div>
            <Link
                href={`/dashboard/${guildId}/warnings`}
                className="inline-flex border border-slate-700 px-4 py-2 font-medium transition hover:border-slate-500 hover:bg-slate-800"
            >
                Back to warnings
            </Link>
        </section>;
    }

    return <section className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
                <p className="text-sm uppercase tracking-[0.18em] text-slate-500">Warnings</p>
                <h2 className="mt-2 text-3xl font-bold tracking-tight">User warning history</h2>
                <p className="mt-2 text-sm text-slate-400">
                    All warnings recorded for {history.user.displayName}.
                </p>
            </div>
            <div className="flex flex-wrap gap-3">
                <Link
                    href={`/dashboard/${guildId}/warnings`}
                    className="border border-slate-700 px-4 py-2 font-medium transition hover:border-slate-500 hover:bg-slate-800"
                >
                    Back to warnings
                </Link>
                {history.warnings.length ? <Link
                    href={`/dashboard/${guildId}/warnings/${history.warnings[0].uuid}`}
                    className="border border-sky-400 bg-sky-400 px-4 py-2 font-medium text-slate-950 transition hover:bg-sky-300"
                >
                    Open latest warning
                </Link> : null}
            </div>
        </div>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex flex-wrap items-center gap-4">
                {history.user.avatarUrl ? <img
                    src={history.user.avatarUrl}
                    alt={history.user.displayName}
                    className="h-16 w-16 rounded-full border border-slate-700 object-cover"
                /> : <div className="flex h-16 w-16 items-center justify-center rounded-full border border-slate-700 bg-slate-900 text-xl font-semibold text-slate-200">
                    {history.user.displayName.trim().charAt(0).toUpperCase() || "?"}
                </div>}
                <div>
                    <p className="text-lg font-semibold text-white">{history.user.displayName}</p>
                    <p className="mt-1 text-sm text-slate-500">
                        <span className="font-mono text-slate-300">{history.user.id}</span>
                    </p>
                </div>
            </div>
        </section>

        <section className="space-y-3">
            {history.warnings.length ? history.warnings.map(warning => (
                <WarningRecordCard key={warning.uuid} guildId={guildId} warning={warning} />
            )) : <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No warnings recorded for this user.
            </div>}
        </section>
    </section>;
}
