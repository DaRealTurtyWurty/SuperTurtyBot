export default function DashboardLoading() {
    return <main className="min-h-screen bg-slate-950 px-5 py-6 text-slate-100 xl:px-8">
        <div role="status" className="border border-slate-800/80 bg-slate-900/75 p-8">
            <h1 className="text-2xl font-bold">Loading dashboard…</h1>
            <p className="mt-2 text-sm text-slate-400">Loading your server settings. This may take a moment.</p>
        </div>
    </main>;
}
