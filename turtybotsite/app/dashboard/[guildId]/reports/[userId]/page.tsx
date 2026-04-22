import Image from "next/image";
import Link from "next/link";
import {redirect} from "next/navigation";
import ReportRecordCard from "@/components/ReportRecordCard";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardUserReports} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";

export default async function ReportsUserPage({
    params
}: {
    params: Promise<{guildId: string; userId: string}>;
}) {
    const session = await requireCurrentSession();
    const {guildId, userId} = await params;
    if (!session.guilds.some(entry => entry.id === guildId)) {
        redirect("/dashboard");
    }

    const history = await fetchDashboardUserReports(guildId, userId).catch(handleDashboardPageError);

    if (!history) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Reports could not be loaded from dashboard API.
        </div>;
    }

    return <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
                <h2 className="text-3xl font-bold tracking-tight">Reports</h2>
                <p className="mt-2 text-sm text-slate-400">
                    Reports for {history.user.displayName}.
                </p>
            </div>
            <Link
                href={`/dashboard/${guildId}/reports`}
                className="border border-slate-700 px-4 py-2 font-medium transition hover:border-slate-500 hover:bg-slate-800"
            >
                Search another user
            </Link>
        </div>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex flex-wrap items-center gap-4">
                {history.user.avatarUrl ? <Image
                    src={history.user.avatarUrl}
                    alt={history.user.displayName}
                    width={64}
                    height={64}
                    className="h-16 w-16 rounded-full border border-slate-700 object-cover"
                /> : <div className="flex h-16 w-16 items-center justify-center rounded-full border border-slate-700 bg-slate-900 text-xl font-semibold text-slate-200">
                    {history.user.displayName.trim().charAt(0).toUpperCase() || "?"}
                </div>}
                <div>
                    <p className="text-lg font-semibold text-white">{history.user.displayName}</p>
                    <p className="mt-1 text-sm text-slate-500">
                        <span className="font-mono text-slate-300">{history.user.id}</span>
                    </p>
                    <p className="mt-1 text-sm text-slate-400">
                        {history.reports.length} report{history.reports.length === 1 ? "" : "s"}
                    </p>
                </div>
            </div>
        </section>

        <section className="space-y-3">
            {history.reports.length ? history.reports.map(report => (
                <ReportRecordCard key={`${report.reporterId}-${report.reason}`} guildId={guildId} userId={history.user.id} report={report} />
            )) : <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No reports recorded for this user.
            </div>}
        </section>
    </div>;
}
