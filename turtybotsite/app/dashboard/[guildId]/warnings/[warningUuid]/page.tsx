import Link from "next/link";
import {redirect} from "next/navigation";
import WarningRecordCard from "@/components/WarningRecordCard";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardWarningDetail} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function WarningDetailPage({
    params
}: {
    params: Promise<{guildId: string; warningUuid: string}>;
}) {
    const session = await requireCurrentSession();
    const {guildId, warningUuid} = await params;
    if (!session.guilds.some(entry => entry.id === guildId)) {
        redirect("/dashboard");
    }

    const detail = await fetchDashboardWarningDetail(guildId, warningUuid).catch(handleDashboardPageError);

    if (!detail) {
        return <section className="space-y-4">
            <div>
                <p className="text-sm uppercase tracking-[0.18em] text-slate-500">Warnings</p>
                <h2 className="mt-2 text-3xl font-bold tracking-tight">Warning detail</h2>
            </div>
            <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
                The dashboard API could not load this warning.
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
                <h2 className="mt-2 text-3xl font-bold tracking-tight">Warning detail</h2>
                <p className="mt-2 text-sm text-slate-400">
                    One warning record and related history for this user.
                </p>
            </div>
            <Link
                href={`/dashboard/${guildId}/warnings`}
                className="border border-slate-700 px-4 py-2 font-medium transition hover:border-slate-500 hover:bg-slate-800"
            >
                Back to warnings
            </Link>
        </div>

        <div className="space-y-3">
            <WarningRecordCard guildId={guildId} warning={detail.warning} highlight />
        </div>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <p className="text-sm font-semibold text-white">Related History</p>
                    <p className="mt-1 text-sm text-slate-400">
                        Other warnings for {detail.user.displayName}.
                    </p>
                </div>
                <Link
                    href={`/dashboard/${guildId}/warnings/user/${detail.user.id}`}
                    className="text-xs uppercase tracking-[0.18em] text-sky-300 transition hover:text-sky-200"
                >
                    View full history
                </Link>
            </div>

            <div className="mt-4 space-y-3">
                {detail.relatedWarnings.length ? detail.relatedWarnings.map(warning => (
                    <WarningRecordCard key={warning.uuid} guildId={guildId} warning={warning} />
                )) : <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                    No other warnings recorded for this user.
                </div>}
            </div>
        </section>
    </section>;
}
