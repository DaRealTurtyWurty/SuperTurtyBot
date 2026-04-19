import Link from "next/link";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardGuildConfig, type DashboardGuildConfigSnapshot} from "@/lib/dashboard-api";
import {getDiscordAvatarUrl, getDiscordGuildIconUrl} from "@/lib/discord";

function readBoolean(config: Record<string, unknown> | undefined, key: string) {
    const value = config?.[key];
    return typeof value === "boolean" ? value : undefined;
}

function formatStatus(value: boolean | undefined) {
    return value ? "Enabled" : "Disabled";
}

export default async function DashboardPage() {
    const session = await requireCurrentSession();
    const avatarUrl = getDiscordAvatarUrl(session.user.id, session.user.avatar);

    const guildCards = await Promise.all(session.guilds.map(async guild => {
        const snapshot = await fetchDashboardGuildConfig(guild.id).catch(() => null) as DashboardGuildConfigSnapshot | null;
        return {
            guild,
            snapshot
        };
    }));
    const connectedGuildCount = guildCards.filter(card => card.snapshot?.guild.connected).length;
    const configuredGuildCount = guildCards.filter(card => card.snapshot?.persisted).length;

    return <main className="min-h-screen bg-slate-950 text-slate-100">
        <div className="flex min-h-screen w-full flex-col gap-6 px-5 py-6 xl:px-8">
            <header className="flex flex-col gap-4 border border-slate-800/80 bg-slate-900/75 p-6 md:flex-row md:items-center md:justify-between">
                <div className="flex items-center gap-4">
                    {avatarUrl ? <img
                        src={avatarUrl}
                        alt={session.user.username}
                        className="h-14 w-14 border border-slate-700"
                    /> : <div
                        className="flex h-14 w-14 items-center justify-center border border-slate-700 bg-slate-800 text-lg font-bold text-sky-200">
                        {(session.user.globalName ?? session.user.username).charAt(0).toUpperCase()}
                    </div>}
                    <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-sky-300">Dashboard</p>
                        <h1 className="mt-1 text-3xl font-bold tracking-tight">
                            Welcome, {session.user.globalName ?? session.user.username}
                        </h1>
                        <p className="text-sm text-slate-400">
                            Pick a guild, check its status, and jump into the right settings page.
                        </p>
                    </div>
                </div>
                <div className="flex flex-wrap gap-3">
                    <Link
                        href="/"
                        className="border border-slate-700 px-4 py-2 font-medium transition hover:border-slate-500 hover:bg-slate-800">
                        Home
                    </Link>
                    <a
                        href="/api/auth/logout"
                        className="border border-sky-400 bg-sky-400 px-4 py-2 font-medium text-slate-950 transition hover:bg-sky-300">
                        Sign Out
                    </a>
                </div>
            </header>

            <section className="space-y-4">
                <div>
                    <h2 className="text-xl font-bold text-white">At a glance</h2>
                    <p className="mt-1 text-sm text-slate-400">Quick numbers for the guilds you can manage.</p>
                </div>

                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <div className="border border-slate-800/80 bg-slate-900/75 p-5">
                    <p className="text-sm text-slate-500">Manageable guilds</p>
                    <p className="mt-3 text-3xl font-bold">{guildCards.length}</p>
                </div>
                <div className="border border-slate-800/80 bg-slate-900/75 p-5">
                    <p className="text-sm text-slate-500">Bot connected</p>
                    <p className="mt-3 text-3xl font-bold">{connectedGuildCount}</p>
                </div>
                <div className="border border-slate-800/80 bg-slate-900/75 p-5">
                    <p className="text-sm text-slate-500">Saved configs</p>
                    <p className="mt-3 text-3xl font-bold">{configuredGuildCount}</p>
                </div>
                </div>
            </section>

            {guildCards.length === 0 ? <section className="border border-slate-800/80 bg-slate-900/75 p-8 text-center text-slate-300">
                No guilds with Manage Server or Administrator access were found on this Discord account.
            </section> : <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {guildCards.map(({guild, snapshot}) => {
                    const guildIconUrl = getDiscordGuildIconUrl(guild.id, guild.icon);
                    const config = snapshot?.config;
                    const isConnected = snapshot?.guild.connected === true;
                    const description = !snapshot
                        ? "Dashboard API unavailable"
                        : !isConnected
                            ? "TurtyBot is not in this server"
                            : snapshot.persisted
                                ? "Bot configuration found"
                                : "No saved guild config yet";

                    const cardContent = <>
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex items-center gap-4">
                            {guildIconUrl ? <img
                                src={guildIconUrl}
                                alt={guild.name}
                                className="h-14 w-14 border border-slate-700"
                            /> : <div
                                className="flex h-14 w-14 items-center justify-center border border-slate-700 bg-slate-800 text-lg font-bold text-sky-200">
                                {guild.name.charAt(0).toUpperCase()}
                            </div>}
                            <div>
                                <h2 className="text-xl font-semibold">{guild.name}</h2>
                                <p className="text-sm text-slate-400">{description}</p>
                            </div>
                            </div>
                            <span className={`border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] ${
                                isConnected
                                    ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-200"
                                    : "border-amber-400/30 bg-amber-400/10 text-amber-200"
                            }`}>
                                {isConnected ? "Connected" : "Offline"}
                            </span>
                        </div>

                        {!snapshot ? <div className="mt-6 border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
                            The dashboard API is unavailable, so this server cannot be opened right now.
                        </div> : !isConnected ? <div className="mt-6 border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
                            Invite TurtyBot to this server before opening its dashboard.
                        </div> : <div className="mt-6 flex flex-wrap gap-2 text-xs font-semibold uppercase tracking-[0.14em] text-slate-200">
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">Starboard {formatStatus(readBoolean(config, "starboard_enabled"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">Levelling {formatStatus(readBoolean(config, "levelling_enabled"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">Economy {formatStatus(readBoolean(config, "economy_enabled"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">AI {formatStatus(readBoolean(config, "ai_enabled"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">Chat Revival {formatStatus(readBoolean(config, "chat_revival_enabled"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">Threads {formatStatus(readBoolean(config, "should_moderators_join_threads"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">Misc {formatStatus(readBoolean(config, "should_create_gists"))}</span>
                            <span className="border border-slate-700 bg-slate-950/80 px-2.5 py-1">NSFW {formatStatus(readBoolean(config, "artist_nsfw_filter_enabled"))}</span>
                        </div>}
                    </>;

                    if (!isConnected) {
                        return <div
                            key={guild.id}
                            className="border border-amber-500/30 bg-slate-900/75 p-6 opacity-90">
                            {cardContent}
                        </div>;
                    }

                    return <Link
                        key={guild.id}
                        href={`/dashboard/${guild.id}`}
                        className="border border-slate-800/80 bg-slate-900/75 p-6 transition hover:border-sky-400/60 hover:bg-slate-900/90">
                        {cardContent}
                    </Link>;
                })}
            </section>}
        </div>
    </main>;
}
