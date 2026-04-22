import Image from "next/image";
import Link from "next/link";
import {redirect} from "next/navigation";
import {getCurrentSession} from "@/lib/auth";
import {isDashboardOfflineError} from "@/lib/dashboard-offline";
import {getDiscordAvatarUrl} from "@/lib/discord";
import {FaArrowRight, FaBell, FaHashtag, FaMessage, FaTriangleExclamation} from "react-icons/fa6";

function getErrorMessage(error: string | undefined) {
    switch (error) {
        case "access_denied":
            return "Discord login was cancelled.";
        case "invalid_oauth_state":
            return "The Discord login state was invalid or expired. Please try again.";
        case "oauth_callback_failed":
            return "Discord login failed. Check the OAuth environment variables and try again.";
        default:
            return null;
    }
}

export default async function Home({
    searchParams
}: {
    searchParams?: Promise<{ error?: string }>;
}) {
    const session = await getCurrentSession().catch(error => {
        if (isDashboardOfflineError(error)) {
            redirect("/bot-offline");
        }

        throw error;
    });
    const error = getErrorMessage((await searchParams)?.error);
    const avatarUrl = session ? getDiscordAvatarUrl(session.user.id, session.user.avatar) : null;

    return <main className="min-h-screen bg-slate-950 text-slate-100">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(56,189,248,0.16),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.10),transparent_32%)]" />
        <div className="relative mx-auto flex min-h-screen w-full max-w-6xl flex-col justify-center gap-10 px-6 py-16 lg:grid lg:grid-cols-[minmax(0,1.1fr)_minmax(360px,0.9fr)] lg:items-center">
            <section className="space-y-8">
                <div className="space-y-5">
                    <p className="inline-flex border border-sky-400/20 bg-sky-400/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-sky-200">
                        TurtyBot Dashboard
                    </p>
                    <div className="space-y-4">
                        <h1 className="max-w-2xl text-5xl font-black tracking-tight text-white md:text-6xl">
                            One place for moderation, community tools, and server automation.
                        </h1>
                        <p className="max-w-2xl text-lg leading-8 text-slate-300">
                            TurtyBot pulls the common server jobs into a single dashboard: warnings, modmail,
                            automod, starboard, levelling, counting, sticky messages, and notifier management.
                        </p>
                    </div>
                    <div>
                        <a
                            href="https://turtywurty.dev/projects/turtybot"
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-2 border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-100 transition hover:border-slate-500 hover:bg-slate-900"
                        >
                            Learn More About TurtyBot
                            <FaArrowRight className="h-3.5 w-3.5" />
                        </a>
                    </div>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                    <div className="border border-slate-800/80 bg-slate-950/60 p-4">
                        <FaTriangleExclamation className="h-5 w-5 text-amber-300" />
                        <p className="mt-3 text-sm font-semibold text-white">Moderation that stays organized</p>
                        <p className="mt-2 text-sm text-slate-400">Warnings, modmail, and automod live in one place.</p>
                    </div>
                    <div className="border border-slate-800/80 bg-slate-950/60 p-4">
                        <FaMessage className="h-5 w-5 text-emerald-300" />
                        <p className="mt-3 text-sm font-semibold text-white">Community tools that keep up</p>
                        <p className="mt-2 text-sm text-slate-400">Sticky messages, counting, starboard, and chat revival.</p>
                    </div>
                    <div className="border border-slate-800/80 bg-slate-950/60 p-4">
                        <FaBell className="h-5 w-5 text-sky-300" />
                        <p className="mt-3 text-sm font-semibold text-white">Notifiers and subscriptions</p>
                        <p className="mt-2 text-sm text-slate-400">Track social and game updates without extra bots.</p>
                    </div>
                    <div className="border border-slate-800/80 bg-slate-950/60 p-4">
                        <FaHashtag className="h-5 w-5 text-violet-300" />
                        <p className="mt-3 text-sm font-semibold text-white">Server progress and rewards</p>
                        <p className="mt-2 text-sm text-slate-400">Levelling, birthday flows, and other server systems.</p>
                    </div>
                </div>

                {error ? <p className="inline-flex max-w-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                    {error}
                </p> : null}
            </section>

            {session ? <section className="border border-slate-800/80 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40">
                <div className="flex flex-col gap-6">
                    <div className="flex items-center gap-4">
                        {avatarUrl ? <Image
                            src={avatarUrl}
                            alt={session.user.username}
                            width={80}
                            height={80}
                            className="h-20 w-20 border border-slate-700"
                        /> : <div
                            className="flex h-20 w-20 items-center justify-center border border-slate-700 bg-slate-800 text-2xl font-bold text-sky-200">
                            {(session.user.globalName ?? session.user.username).charAt(0).toUpperCase()}
                        </div>}
                        <div className="space-y-1">
                            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Signed in</p>
                            <h2 className="text-2xl font-semibold text-white">
                                {(session.user.globalName ?? session.user.username)}
                            </h2>
                            <p className="text-sm text-slate-400">@{session.user.username}</p>
                            <p className="text-sm text-slate-300">
                                You can currently manage {session.guilds.length} guild{session.guilds.length === 1 ? "" : "s"}.
                            </p>
                        </div>
                    </div>

                    <div className="flex flex-wrap gap-3">
                        <Link
                            href="/dashboard"
                            className="inline-flex items-center gap-2 bg-sky-500 px-5 py-3 font-semibold text-slate-950 transition hover:bg-sky-400">
                            Open Dashboard
                            <FaArrowRight className="h-4 w-4" />
                        </Link>
                        <a
                            href="/api/auth/logout"
                            className="inline-flex items-center gap-2 border border-slate-700 px-5 py-3 font-semibold text-slate-100 transition hover:border-slate-500 hover:bg-slate-800">
                            Sign Out
                        </a>
                    </div>

                </div>
            </section> : <section className="border border-slate-800/80 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40">
                <div className="space-y-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Sign in</p>
                    <h2 className="text-2xl font-semibold text-white">Open the dashboard with Discord</h2>
                    <p className="text-slate-300">
                        Sign in to pick a guild and configure the bot from the same place every time.
                    </p>
                </div>
                <div className="mt-8 space-y-3">
                    <a
                        href="/api/auth/discord/login"
                        className="inline-flex items-center gap-2 bg-sky-500 px-5 py-3 font-semibold text-slate-950 transition hover:bg-sky-400">
                        Sign In With Discord
                        <FaArrowRight className="h-4 w-4" />
                    </a>
                    <p className="text-sm text-slate-400">
                        After sign-in you will land on the guild dashboard automatically.
                    </p>
                </div>
            </section>}
        </div>
    </main>
}
