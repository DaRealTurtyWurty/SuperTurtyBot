import {notFound} from "next/navigation";
import NotifierManagementPanel from "@/components/NotifierManagementPanel";
import NotifierIcon from "@/components/NotifierIcon";
import NotifierTypeNav from "@/components/NotifierTypeNav";
import {fetchDashboardNotifiers, isDashboardApiError} from "@/lib/dashboard-api";
import {getNotifierSectionDefinition, getNotifierTypeDefinition} from "@/lib/notifiers";

export default async function NotifierTypePage({
    params
}: {
    params: Promise<{ guildId: string; type: string }>;
}) {
    const {guildId, type} = await params;
    const typeInfo = getNotifierTypeDefinition(type);
    if (!typeInfo) {
        notFound();
    }

    const data = await fetchDashboardNotifiers(guildId).catch(error => {
        if (isDashboardApiError(error)) {
            return null;
        }

        throw error;
    });

    if (!data) {
        return <div className="border border-red-500/30 bg-red-500/10 p-6 text-red-100">
            Notifiers could not be loaded from the dashboard API.
        </div>;
    }

    const section = getNotifierSectionDefinition(typeInfo.section);

    return <div className="space-y-6">
        <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-4">
                <span className="flex h-14 w-14 items-center justify-center border border-slate-700 bg-slate-900 text-slate-100">
                    <NotifierIcon icon={typeInfo.icon} className="h-7 w-7" />
                </span>
                <div className="space-y-1">
                    <h2 className="text-3xl font-bold tracking-tight">{typeInfo.label}</h2>
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{section.label}</p>
                </div>
            </div>
            <p className="max-w-4xl text-sm leading-6 text-slate-400">
                {typeInfo.description}
            </p>
        </div>

        <NotifierTypeNav guildId={guildId} activeType={type} />

        <section className="flex flex-wrap gap-3">
            <div className="border border-slate-800/80 bg-slate-950/60 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Command</p>
                <p className="mt-1 text-sm text-slate-200">{typeInfo.command}</p>
            </div>
            <div className="border border-slate-800/80 bg-slate-950/60 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Entries</p>
                <p className="mt-1 text-sm text-slate-200">
                    {data.sections.flatMap(entry => entry.entries).filter(entry => entry.type === type).length}
                </p>
            </div>
            <div className="border border-slate-800/80 bg-slate-950/60 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Group</p>
                <p className="mt-1 text-sm text-slate-200">{section.label}</p>
            </div>
            <div className="border border-slate-800/80 bg-slate-950/60 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Group count</p>
                <p className="mt-1 text-sm text-slate-200">
                    {data.sections.find(entry => entry.key === typeInfo.section)?.count ?? 0}
                </p>
            </div>
        </section>

        {typeInfo.details.length ? <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <h3 className="text-sm font-semibold uppercase tracking-[0.16em] text-slate-500">What this notifier does</h3>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-300">
                {typeInfo.details.map(detail => <li key={detail} className="flex gap-3">
                    <span className="mt-[0.55rem] h-1.5 w-1.5 flex-none rounded-full bg-sky-400/80" />
                    <span>{detail}</span>
                </li>)}
            </ul>
        </section> : null}

        <NotifierManagementPanel guildId={guildId} type={type} initialData={data} />
    </div>;
}
