import NotifierSectionCard from "@/components/NotifierSectionCard";
import NotifierTypeCard from "@/components/NotifierTypeCard";
import NotifierTypeNav from "@/components/NotifierTypeNav";
import {fetchDashboardNotifiers, isDashboardApiError} from "@/lib/dashboard-api";
import {NOTIFIER_SECTIONS, NOTIFIER_TYPES} from "@/lib/notifiers";

function countEntriesByType(data: Awaited<ReturnType<typeof fetchDashboardNotifiers>>, type: string) {
    return data.sections.flatMap(section => section.entries).filter(entry => entry.type === type).length;
}

export default async function NotifiersPage({
    params
}: {
    params: Promise<{ guildId: string }>;
}) {
    const guildId = (await params).guildId;
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

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Notifiers</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Browse notifier categories, then open a type page to inspect every subscribed channel, feed, and target.
            </p>
        </div>

        <NotifierTypeNav guildId={guildId} activeType={null} />

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <div className="border border-slate-800/80 bg-slate-950/60 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Total notifiers</p>
                <p className="mt-1 text-2xl font-bold text-white">{data.totalCount}</p>
            </div>
            {data.sections.map(section => <div key={section.key} className="border border-slate-800/80 bg-slate-950/60 px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{section.title}</p>
                <p className="mt-1 text-2xl font-bold text-white">{section.count}</p>
            </div>)}
        </section>

        <section className="grid gap-4 md:grid-cols-2">
            {data.sections.map(section => {
                const sectionTypes = NOTIFIER_TYPES.filter(type => type.section === section.key);
                const sectionMeta = NOTIFIER_SECTIONS[section.key as keyof typeof NOTIFIER_SECTIONS];

                return <NotifierSectionCard
                    key={section.key}
                    id={section.key}
                    title={sectionMeta.label}
                    description={sectionMeta.description}
                    count={section.count}
                >
                    <div className="grid gap-4 xl:grid-cols-2">
                        {sectionTypes.map(type => <NotifierTypeCard
                            key={type.type}
                            guildId={guildId}
                            type={type.type}
                            section={{
                                ...section,
                                count: countEntriesByType(data, type.type)
                            }}
                        />)}
                    </div>
                </NotifierSectionCard>;
            })}
        </section>
    </div>;
}
