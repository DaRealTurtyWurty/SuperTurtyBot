"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardStarboardSettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import DashboardNumberInput from "@/components/DashboardNumberInput";

interface StarboardSettingsFormProps {
    guildId: string;
    initialSettings: DashboardStarboardSettings;
}

export default function StarboardSettingsForm({guildId, initialSettings}: StarboardSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        starboardChannelId: initialSettings.starboardChannelId ?? "",
        showcaseChannelIds: initialSettings.showcaseChannelIds
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const isDisabled = !settings.starboardEnabled;

    function updateBoolean(key: "starboardEnabled" | "botStarsCount" | "starboardMediaOnly", value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateText(key: "starboardChannelId" | "starEmoji", value: string) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateShowcaseChannelIds(values: string[]) {
        setSettings(current => ({
            ...current,
            showcaseChannelIds: values
        }));
    }

    function updateMinimumStars(value: string) {
        setSettings(current => ({
            ...current,
            minimumStars: Number.parseInt(value, 10) || 1
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/starboard`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    starboardEnabled: settings.starboardEnabled,
                    starboardChannelId: settings.starboardChannelId.trim() || null,
                    minimumStars: settings.minimumStars,
                    botStarsCount: settings.botStarsCount,
                    showcaseChannelIds: settings.showcaseChannelIds,
                    starboardMediaOnly: settings.starboardMediaOnly,
                    starEmoji: settings.starEmoji
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save starboard settings.");
                return;
            }

            const updated = await response.json() as DashboardStarboardSettings;
            setSettings({
                ...updated,
                starboardChannelId: updated.starboardChannelId ?? "",
                showcaseChannelIds: updated.showcaseChannelIds
            });
            setSuccess("Starboard settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <div className="grid gap-4 md:grid-cols-2">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Enable Starboard</p>
                        <p className="mt-1 text-sm text-slate-400">Turn message showcasing on or off for this guild.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.starboardEnabled}
                        onChange={event => updateBoolean("starboardEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <GuildChannelSelect
                guildId={guildId}
                value={settings.starboardChannelId}
                onChange={value => updateText("starboardChannelId", value)}
                disabled={isDisabled}
                label="Starboard Channel"
                description="Select the text channel used for featured messages."
                placeholder="Select a channel"
            />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm font-semibold text-white">Minimum Stars</p>
                <p className="mt-1 text-sm text-slate-400">How many stars a message needs before it is featured.</p>
                <DashboardNumberInput
                    min={1}
                    value={settings.minimumStars}
                    onChange={updateMinimumStars}
                    disabled={isDisabled}
                    placeholder="1"
                />
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <p className="text-sm font-semibold text-white">Star Emoji</p>
                <p className="mt-1 text-sm text-slate-400">Unicode or custom Discord emoji.</p>
                <input
                    value={settings.starEmoji}
                    onChange={event => updateText("starEmoji", event.target.value)}
                    disabled={isDisabled}
                    className="mt-4 w-full border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none transition focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
                    placeholder="⭐"
                />
            </label>
        </div>

        <GuildChannelSelect
            guildId={guildId}
            value=""
            onChange={() => {}}
            multiple
            values={settings.showcaseChannelIds}
            onValuesChange={updateShowcaseChannelIds}
            disabled={isDisabled}
            label="Showcase Channels"
            description="Optional allowlist. Only messages from these channels can reach the starboard."
        />

        <div className="grid gap-4 md:grid-cols-2">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Count Bot Stars</p>
                        <p className="mt-1 text-sm text-slate-400">Include stars added by bots in the total.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.botStarsCount}
                        onChange={event => updateBoolean("botStarsCount", event.target.checked)}
                        disabled={isDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Media Only</p>
                        <p className="mt-1 text-sm text-slate-400">Only allow messages with media attachments into the starboard.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.starboardMediaOnly}
                        onChange={event => updateBoolean("starboardMediaOnly", event.target.checked)}
                        disabled={isDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
        </div>

        {error ? <p className="border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
            {error}
        </p> : null}

        {success ? <p className="border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100">
            {success}
        </p> : null}

        <div className="flex items-center justify-between gap-4">
            <button
                type="submit"
                disabled={isPending}
                className="border border-sky-400 bg-sky-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-sky-300 disabled:cursor-not-allowed disabled:opacity-60">
                {isPending ? "Saving..." : "Save Starboard"}
            </button>
        </div>
    </form>;
}
