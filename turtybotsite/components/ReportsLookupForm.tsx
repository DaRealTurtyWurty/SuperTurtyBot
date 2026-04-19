"use client";

import type {FormEvent} from "react";
import {useRouter} from "next/navigation";
import {useState} from "react";

export default function ReportsLookupForm({guildId}: {guildId: string}) {
    const router = useRouter();
    const [userId, setUserId] = useState("");

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const trimmed = userId.trim();
        if (!trimmed) {
            return;
        }

        router.push(`/dashboard/${guildId}/reports/${trimmed}`);
    }

    return <form onSubmit={onSubmit} className="space-y-4">
        <label id="user-id" className="block scroll-mt-24">
            <span className="text-sm font-semibold text-white">User ID</span>
            <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                value={userId}
                onChange={event => setUserId(event.target.value.replace(/[^0-9]/g, "").slice(0, 18))}
                placeholder="Enter user ID"
                className="mt-2 w-full border border-slate-700 bg-slate-950 px-4 py-3 text-slate-100 placeholder:text-slate-500 focus:border-sky-400 focus:outline-none"
            />
        </label>

        <button
            type="submit"
            className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300"
        >
            View reports
        </button>
    </form>;
}
