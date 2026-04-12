import type {DashboardNotifierEntry} from "@/lib/notifier-types";
import {getNotifierTypeDefinition} from "@/lib/notifiers";
import NotifierIcon from "@/components/NotifierIcon";
import NotifierPingPreview from "@/components/NotifierPingPreview";

function formatChannel(value: string | null) {
    if (!value) {
        return "Channel missing";
    }

    return value;
}

export default function NotifierEntryCard({
    entry,
    guildId,
    onEdit,
    onRemove,
    removeDisabled = false,
    removeLabel = "Remove notifier",
    editLabel = "Edit notifier"
}: {
    entry: DashboardNotifierEntry;
    guildId: string;
    onEdit?: () => void;
    onRemove?: () => void;
    removeDisabled?: boolean;
    removeLabel?: string;
    editLabel?: string;
}) {
    const typeInfo = getNotifierTypeDefinition(entry.type);

    return <article className="border border-slate-800/80 bg-slate-950/60 p-4">
        <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0 space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                    {typeInfo ? <NotifierIcon icon={typeInfo.icon} className="h-6 w-6" /> : null}
                    <span className="border border-sky-400/30 bg-sky-400/10 px-2 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-sky-200">
                        {entry.kind}
                    </span>
                    <span className="border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] font-medium uppercase tracking-[0.14em] text-slate-300">
                        {entry.targetLabel}
                    </span>
                </div>
                <p className="text-sm text-slate-400">
                    Channel <span className="font-semibold text-slate-200">{formatChannel(entry.channelName)}</span>
                    <span className="font-mono text-slate-500"> ({entry.channelId})</span>
                </p>
                <div className="space-y-1">
                    <p className="text-sm text-slate-400">Ping</p>
                    <NotifierPingPreview guildId={guildId} value={entry.mention ?? ""} />
                </div>
            </div>
            <div className="text-right text-xs text-slate-500">
                <p className="font-mono uppercase tracking-[0.14em] text-slate-400">{typeInfo?.label ?? entry.type}</p>
            </div>
        </div>

        {entry.details.length ? <ul className="mt-4 space-y-1.5 border-t border-slate-800 pt-4 text-sm text-slate-300">
            {entry.details.map(detail => <li key={detail} className="flex gap-2">
                <span className="mt-[0.45rem] h-1.5 w-1.5 flex-none rounded-full bg-sky-400/80" />
                <span>{detail}</span>
            </li>)}
        </ul> : null}

        {onEdit || onRemove ? <div className="mt-4 flex flex-wrap justify-end gap-2 border-t border-slate-800 pt-4">
            {onEdit ? <button
                type="button"
                onClick={onEdit}
                className="border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-slate-100 transition hover:border-slate-500 hover:bg-slate-800"
            >
                {editLabel}
            </button> : null}
            {onRemove ? <button
                type="button"
                onClick={onRemove}
                disabled={removeDisabled}
                className="border border-red-500/40 bg-red-500/10 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-red-100 transition hover:border-red-400 hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-60"
            >
                {removeDisabled ? "Removing..." : removeLabel}
            </button> : null}
        </div> : null}
    </article>;
}
