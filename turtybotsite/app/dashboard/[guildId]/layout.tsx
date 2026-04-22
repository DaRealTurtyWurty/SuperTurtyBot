import type {ReactNode} from "react";
import Image from "next/image";
import Link from "next/link";
import {redirect} from "next/navigation";
import DashboardCommandPalette from "@/components/DashboardCommandPalette";
import {DashboardNavigationGuard} from "@/components/DashboardNavigationGuard";
import GuildDashboardSidebar from "@/components/GuildDashboardSidebar";
import {requireCurrentSession} from "@/lib/auth";
import {fetchDashboardGuildConfig} from "@/lib/dashboard-api";
import {handleDashboardPageError} from "@/lib/dashboard-offline";
import {getDiscordGuildIconUrl} from "@/lib/discord";

export default async function GuildDashboardLayout({
    children,
    params
}: Readonly<{
    children: ReactNode;
    params: Promise<{ guildId: string }>;
}>) {
    const session = await requireCurrentSession();
    const guildId = (await params).guildId;
    const guild = session.guilds.find(entry => entry.id === guildId);
    if (!guild) {
        redirect("/dashboard");
    }

    const snapshot = await fetchDashboardGuildConfig(guildId).catch(handleDashboardPageError);

    if (!snapshot || !snapshot.guild.connected) {
        redirect("/dashboard");
    }

    const guildIconUrl = getDiscordGuildIconUrl(guild.id, guild.icon, 256);

    return <DashboardNavigationGuard>
        <main className="min-h-screen bg-slate-950 text-slate-100">
            <div className="flex min-h-screen w-full flex-col gap-6 px-5 py-6 xl:px-8">
                <div className="flex flex-wrap items-center justify-between gap-4">
                    <Link
                        href="/dashboard"
                        className="border border-slate-700 px-4 py-2 font-medium transition hover:border-slate-500 hover:bg-slate-800">
                        Back To Dashboard
                    </Link>
                    <div className="flex flex-wrap items-center gap-3">
                        <DashboardCommandPalette guildId={guildId} />
                        <a
                            href="/api/auth/logout"
                            className="border border-sky-400 bg-sky-400 px-4 py-2 font-medium text-slate-950 transition hover:bg-sky-300">
                            Sign Out
                        </a>
                    </div>
                </div>

                <header className="border border-slate-800/80 bg-slate-900/75 p-8">
                    <div className="flex flex-col gap-6 md:flex-row md:items-center">
                        {guildIconUrl ? <Image
                            src={guildIconUrl}
                            alt={guild.name}
                            width={96}
                            height={96}
                            className="h-24 w-24 border border-slate-700"
                        /> : <div
                            className="flex h-24 w-24 items-center justify-center border border-slate-700 bg-slate-800 text-3xl font-bold text-sky-200">
                            {guild.name.charAt(0).toUpperCase()}
                        </div>}
                        <div className="space-y-2">
                            <h1 className="text-4xl font-bold">{guild.name}</h1>
                            <p className="text-sm text-slate-400">Guild ID <span className="font-mono text-slate-300">{guild.id}</span></p>
                        </div>
                    </div>
                </header>

                <div className="grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)] xl:grid-cols-[320px_minmax(0,1fr)]">
                    <GuildDashboardSidebar guildId={guildId} />
                    <section className="border border-slate-800/80 bg-slate-900/75 p-6 md:p-8">
                        {children}
                    </section>
                </div>
            </div>
        </main>
    </DashboardNavigationGuard>;
}
