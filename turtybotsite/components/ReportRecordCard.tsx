import Image from "next/image";
import Link from "next/link";
import type {DashboardReportRecord} from "@/lib/dashboard-api";

function formatTimestamp(value: number) {
    if (!value) {
        return "Unknown";
    }

    return new Date(value).toLocaleString();
}

function ReportAvatar({name, avatarUrl}: {name: string; avatarUrl: string | null}) {
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

export default function ReportRecordCard({
    guildId,
    userId,
    report
}: {
    guildId: string;
    userId: string;
    report: DashboardReportRecord;
}) {
    return <article className="border border-slate-800/80 bg-slate-950/60 p-4">
        <div className="flex gap-3">
            <ReportAvatar name={report.reporterDisplayName} avatarUrl={report.reporterAvatarUrl} />
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                        <p className="text-sm font-semibold text-white">Reported by {report.reporterDisplayName}</p>
                        <p className="mt-1 text-xs text-slate-500">
                            Reporter ID <span className="font-mono text-slate-300">{report.reporterId}</span> · {formatTimestamp(report.reportedAt)}
                        </p>
                    </div>
                    <Link
                        href={`/dashboard/${guildId}/warnings/user/${userId}`}
                        className="rounded border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] text-slate-300 transition hover:border-slate-500 hover:text-white"
                    >
                        Open warnings
                    </Link>
                </div>
                <p className="mt-3 whitespace-pre-wrap text-sm text-slate-200">
                    {report.reason?.trim() ? report.reason : "No reason provided."}
                </p>
            </div>
        </div>
    </article>;
}
