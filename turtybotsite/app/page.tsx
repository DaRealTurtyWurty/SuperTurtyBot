import Link from "next/link";
import UserIdInput from "@/components/UserIdInput";
import {getCurrentSession} from "@/lib/auth";
import {getDiscordAvatarUrl} from "@/lib/discord";

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
    const session = await getCurrentSession();
    const error = getErrorMessage((await searchParams)?.error);
    const avatarUrl = session ? getDiscordAvatarUrl(session.user.id, session.user.avatar) : null;

    return <main className="min-h-screen bg-slate-950 text-slate-100">
        <div className="mx-auto flex min-h-screen w-full max-w-5xl flex-col items-center justify-center gap-8 px-6 py-16 text-center">
            <div className="space-y-4">
                <p className="text-sm uppercase tracking-[0.3em] text-sky-300">TurtyBot Dashboard</p>
                <h1 className="text-5xl font-bold tracking-tight">Manage your server with Discord sign-in</h1>
                <p className="mx-auto max-w-2xl text-lg text-slate-300">
                    Sign in with Discord to access your guild dashboard, view bot configuration, and build out the
                    admin surface safely behind authenticated sessions.
                </p>
            </div>

            {error ? <p className="rounded-full border border-red-500/40 bg-red-500/10 px-5 py-2 text-sm text-red-200">
                {error}
            </p> : null}

            {session ? <section className="w-full max-w-3xl rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40">
                <div className="flex flex-col items-center gap-4">
                    {avatarUrl ? <img
                        src={avatarUrl}
                        alt={session.user.username}
                        className="h-20 w-20 rounded-full border border-slate-700"
                    /> : <div
                        className="flex h-20 w-20 items-center justify-center rounded-full border border-slate-700 bg-slate-800 text-2xl font-bold text-sky-200">
                        {(session.user.globalName ?? session.user.username).charAt(0).toUpperCase()}
                    </div>}
                    <div className="space-y-1">
                        <h2 className="text-2xl font-semibold">
                            {(session.user.globalName ?? session.user.username)}
                        </h2>
                        <p className="text-sm text-slate-400">@{session.user.username}</p>
                        <p className="text-sm text-slate-300">
                            You can currently manage {session.guilds.length} guild{session.guilds.length === 1 ? "" : "s"}.
                        </p>
                    </div>
                    <div className="flex flex-wrap items-center justify-center gap-3 pt-4">
                        <Link
                            href="/dashboard"
                            className="rounded-full bg-sky-500 px-5 py-3 font-semibold text-slate-950 transition hover:bg-sky-400">
                            Open Dashboard
                        </Link>
                        <a
                            href="/api/auth/logout"
                            className="rounded-full border border-slate-700 px-5 py-3 font-semibold text-slate-100 transition hover:border-slate-500 hover:bg-slate-800">
                            Sign Out
                        </a>
                    </div>
                </div>

                <div className="mt-10 space-y-4 border-t border-slate-800 pt-8">
                    <p className="text-sm uppercase tracking-[0.25em] text-slate-400">Legacy User Lookup</p>
                    <p className="text-sm text-slate-300">
                        This existing profile viewer is still available while the guild dashboard is being built out.
                    </p>
                    <div className="flex justify-center">
                        <UserIdInput />
                    </div>
                </div>
            </section> : <section className="w-full max-w-2xl rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40">
                <div className="space-y-4">
                    <h2 className="text-2xl font-semibold">Discord OAuth is now required</h2>
                    <p className="text-slate-300">
                        Sign in with Discord to start using the dashboard and to prevent anonymous access to bot data.
                    </p>
                </div>
                <div className="pt-6">
                    <a
                        href="/api/auth/discord/login"
                        className="inline-flex rounded-full bg-sky-500 px-5 py-3 font-semibold text-slate-950 transition hover:bg-sky-400">
                        Sign In With Discord
                    </a>
                </div>
            </section>}
        </div>
    </main>
}
