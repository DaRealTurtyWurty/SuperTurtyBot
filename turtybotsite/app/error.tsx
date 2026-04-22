"use client";

import Link from "next/link";
import {useEffect} from "react";
import {FaArrowRotateRight, FaPlugCircleXmark} from "react-icons/fa6";

export default function GlobalError({
    error,
    reset
}: Readonly<{
    error: Error & { digest?: string };
    reset: () => void;
}>) {
    useEffect(() => {
        console.error(error);
    }, [error]);

    return <main className="min-h-screen bg-slate-950 text-slate-100">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(248,113,113,0.14),transparent_30%),radial-gradient(circle_at_bottom_right,rgba(56,189,248,0.12),transparent_34%)]" />
        <div className="relative mx-auto flex min-h-screen w-full max-w-4xl items-center px-6 py-16">
            <section className="w-full border border-slate-800/80 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40 md:p-12">
                <div className="flex h-16 w-16 items-center justify-center border border-red-400/30 bg-red-500/10 text-red-200">
                    <FaPlugCircleXmark className="h-8 w-8" />
                </div>

                <p className="mt-8 inline-flex border border-red-400/20 bg-red-500/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] text-red-200">
                    Service Unavailable
                </p>
                <h1 className="mt-5 text-4xl font-black tracking-tight text-white md:text-5xl">
                    The bot is currently offline.
                </h1>
                <p className="mt-4 max-w-2xl text-lg leading-8 text-slate-300">
                    The site could not reach TurtyBot right now. Try again after the bot service comes back online.
                </p>

                <div className="mt-8 flex flex-wrap gap-3">
                    <button
                        type="button"
                        onClick={reset}
                        className="inline-flex items-center gap-2 bg-sky-500 px-5 py-3 font-semibold text-slate-950 transition hover:bg-sky-400"
                    >
                        <FaArrowRotateRight className="h-4 w-4" />
                        Retry
                    </button>
                    <Link
                        href="/bot-offline"
                        className="inline-flex items-center gap-2 border border-slate-700 px-5 py-3 font-semibold text-slate-100 transition hover:border-slate-500 hover:bg-slate-800"
                    >
                        Offline Page
                    </Link>
                </div>
            </section>
        </div>
    </main>;
}
