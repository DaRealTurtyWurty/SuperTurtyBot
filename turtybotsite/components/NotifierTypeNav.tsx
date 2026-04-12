import Link from "next/link";
import {NOTIFIER_TYPES} from "@/lib/notifiers";
import NotifierIcon from "@/components/NotifierIcon";

export default function NotifierTypeNav({
    guildId,
    activeType
}: {
    guildId: string;
    activeType: string | null;
}) {
    return <div className="flex flex-wrap gap-2">
        <Link
            href={`/dashboard/${guildId}/notifiers`}
            className={`border px-3 py-2 text-xs font-semibold uppercase tracking-[0.14em] transition ${
                activeType === null
                    ? "border-sky-400 bg-sky-400/10 text-sky-100"
                    : "border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-500 hover:text-white"
            }`}
        >
            Overview
        </Link>
        {NOTIFIER_TYPES.map(type => <Link
            key={type.type}
            href={`/dashboard/${guildId}/notifiers/${type.type}`}
            className={`border px-3 py-2 text-xs font-semibold uppercase tracking-[0.14em] transition ${
                activeType === type.type
                    ? "border-sky-400 bg-sky-400/10 text-sky-100"
                    : "border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-500 hover:text-white"
            }`}
        >
            <span className="flex items-center gap-2">
                <NotifierIcon icon={type.icon} className="h-3.5 w-3.5" />
                <span>{type.label}</span>
            </span>
        </Link>)}
    </div>;
}
