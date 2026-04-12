import ReportsLookupForm from "@/components/ReportsLookupForm";

export default async function ReportsPage({
    params
}: {
    params: Promise<{guildId: string}>;
}) {
    const guildId = (await params).guildId;

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Reports</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Look up report history for user by ID. Reports page has no timestamps, so this view is user-focused.
            </p>
        </div>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <ReportsLookupForm guildId={guildId} />
        </section>
    </div>;
}
