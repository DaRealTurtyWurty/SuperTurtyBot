import Link from "next/link";
import type {DashboardNotifierSection} from "@/lib/notifier-types";
import {getNotifierSectionDefinition, getNotifierTypeDefinition} from "@/lib/notifiers";
import NotifierIcon from "@/components/NotifierIcon";

export default function NotifierTypeCard({
    guildId,
    section,
    type
}: {
    guildId: string;
    section: DashboardNotifierSection;
    type: string;
}) {
    const typeInfo = getNotifierTypeDefinition(type);
    const sectionInfo = typeInfo ? getNotifierSectionDefinition(typeInfo.section) : null;

    return <Link
        href={`/dashboard/${guildId}/notifiers/${type}`}
        className="group border border-slate-800/80 bg-slate-950/60 p-4 transition hover:border-sky-400/40 hover:bg-slate-900/80"
    >
        <div className="flex items-start justify-between gap-3">
            <div className="min-w-0 space-y-3">
                <div className="flex items-center gap-3">
                    {typeInfo ? <NotifierIcon icon={typeInfo.icon} className="h-8 w-8" /> : null}
                    <div className="min-w-0">
                        <p className="text-sm font-semibold text-white">{typeInfo?.label ?? type}</p>
                        <p className="mt-1 text-xs text-slate-500">{typeInfo?.description ?? section.description}</p>
                    </div>
                </div>
                {typeInfo?.details?.length ? <p className="text-xs leading-5 text-slate-400">
                    {typeInfo.details[0]}
                </p> : null}
            </div>
            <span className="border border-slate-700 bg-slate-900 px-2 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-300">
                {section.count}
            </span>
        </div>

        <div className="mt-4 flex items-center justify-between gap-3">
            <p className="text-xs text-slate-400">{sectionInfo?.label ?? section.title}</p>
            <span className="inline-flex items-center border border-sky-400/30 bg-sky-400/10 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-sky-100 transition group-hover:border-sky-400/50 group-hover:bg-sky-400/15 group-hover:text-white">
                Open type page
            </span>
        </div>
    </Link>;
}
