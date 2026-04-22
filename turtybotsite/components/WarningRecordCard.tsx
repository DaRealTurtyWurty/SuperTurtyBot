import Image from "next/image";
import Link from "next/link";
import type {DashboardWarningRecord} from "@/lib/dashboard-api";

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

export default function WarningRecordCard({
    guildId,
    warning,
    highlight = false
}: {
    guildId: string;
    warning: DashboardWarningRecord;
    highlight?: boolean;
}) {
    return <article className={`border p-4 ${highlight ? "border-sky-400/40 bg-sky-400/5" : "border-slate-800/80 bg-slate-950/60"}`}>
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
            </div>
        </div>
    </article>;
}
