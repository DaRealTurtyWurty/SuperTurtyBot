import Link from "next/link";
import {searchDashboardEntries} from "@/lib/dashboard-search";

export default async function DashboardSearchPage({
    params,
    searchParams
}: {
    params: Promise<{guildId: string}>;
    searchParams: Promise<{q?: string}>;
}) {
    const guildId = (await params).guildId;
    const query = (await searchParams).q?.trim() ?? "";
    const results = searchDashboardEntries(guildId, query);

    return <div className="space-y-6">
        <div className="space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Search</h2>
            <p className="max-w-3xl text-sm text-slate-400">
                Search dashboard pages and settings. Type in sidebar, press Enter, then refine here.
            </p>
        </div>

        <form action={`/dashboard/${guildId}/search`} method="get" className="space-y-3">
            <label className="block">
                <span className="text-sm font-semibold text-white">Search dashboard</span>
                <input
                    name="q"
                    defaultValue={query}
                    placeholder="Search settings, pages, channels, roles..."
                    className="mt-2 w-full border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-sky-400"
                />
            </label>
            <button
                type="submit"
                className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300"
            >
                Search
            </button>
        </form>

        {query ? <div className="space-y-3">
            <p className="text-sm text-slate-400">
                {results.length} result{results.length === 1 ? "" : "s"} for <span className="font-semibold text-slate-100">{query}</span>
            </p>

            {results.length > 0 ? <div className="grid gap-3">
                {results.map(result => <Link
                    key={`${result.href}:${result.term}`}
                    href={result.href}
                    className="border border-slate-800/80 bg-slate-950/60 p-5 transition hover:border-sky-400/40 hover:bg-slate-900/80"
                >
                    <p className="text-sm font-semibold text-white">{result.label}</p>
                    <p className="mt-1 text-sm text-slate-400">{result.href}</p>
                </Link>)}
            </div> : <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
                No results found.
            </div>}
        </div> : <div className="border border-slate-800/80 bg-slate-950/60 p-5 text-sm text-slate-400">
            Enter query to search dashboard.
        </div>}
    </div>;
}
