"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardAutomodSettings} from "@/lib/dashboard-api";
import DashboardNumberInput from "@/components/DashboardNumberInput";
import GuildChannelSelect from "@/components/GuildChannelSelect";

interface AutomodSettingsFormProps {
    guildId: string;
    initialSettings: DashboardAutomodSettings;
}

export default function AutomodSettingsForm({guildId, initialSettings}: AutomodSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        inviteGuardWhitelistChannelIds: initialSettings.inviteGuardWhitelistChannelIds
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    function updateBoolean(key: "inviteGuardEnabled" | "scamDetectionEnabled" | "imageSpamAutoBanEnabled", value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateInviteGuardChannels(values: string[]) {
        setSettings(current => ({
            ...current,
            inviteGuardWhitelistChannelIds: values
        }));
    }

    function updateNumber(key: "imageSpamWindowSeconds" | "imageSpamMinImages" | "imageSpamNewMemberThresholdHours", value: string) {
        setSettings(current => ({
            ...current,
            [key]: Number.parseInt(value, 10) || 0
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/automod`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    inviteGuardEnabled: settings.inviteGuardEnabled,
                    inviteGuardWhitelistChannelIds: settings.inviteGuardWhitelistChannelIds,
                    scamDetectionEnabled: settings.scamDetectionEnabled,
                    imageSpamAutoBanEnabled: settings.imageSpamAutoBanEnabled,
                    imageSpamWindowSeconds: settings.imageSpamWindowSeconds,
                    imageSpamMinImages: settings.imageSpamMinImages,
                    imageSpamNewMemberThresholdHours: settings.imageSpamNewMemberThresholdHours
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save automod settings.");
                return;
            }

            const updated = await response.json() as DashboardAutomodSettings;
            setSettings({
                ...updated,
                inviteGuardWhitelistChannelIds: updated.inviteGuardWhitelistChannelIds
            });
            setSuccess("Automod settings saved.");
        });
    }

    const inviteGuardDisabled = !settings.inviteGuardEnabled;
    const imageSpamDisabled = !settings.imageSpamAutoBanEnabled;

    return <form onSubmit={onSubmit} className="space-y-5">
        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <p className="text-sm font-semibold text-white">Invite Guard</p>
                    <p className="mt-1 text-sm text-slate-400">Blocks invite links in the selected text channels.</p>
                </div>
                <input
                    type="checkbox"
                    checked={settings.inviteGuardEnabled}
                    onChange={event => updateBoolean("inviteGuardEnabled", event.target.checked)}
                    className="h-5 w-5 accent-sky-400"
                />
            </div>

            <div className="mt-4">
                <GuildChannelSelect
                    guildId={guildId}
                    value=""
                    onChange={() => {}}
                    multiple
                    values={settings.inviteGuardWhitelistChannelIds}
                    onValuesChange={updateInviteGuardChannels}
                    disabled={inviteGuardDisabled}
                    label="Blocked Channels"
                    description="Invite links are removed in these text channels."
                />
            </div>
        </section>

        <div className="grid gap-4 md:grid-cols-2">
            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Scam Detection</p>
                        <p className="mt-1 text-sm text-slate-400">Flags common scam domains before they spread.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.scamDetectionEnabled}
                        onChange={event => updateBoolean("scamDetectionEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label className="border border-slate-800/80 bg-slate-950/60 p-5">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Image Spam AutoBan</p>
                        <p className="mt-1 text-sm text-slate-400">Bans fast image spam from newer members.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.imageSpamAutoBanEnabled}
                        onChange={event => updateBoolean("imageSpamAutoBanEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
        </div>

        <section className="border border-slate-800/80 bg-slate-950/60 p-5">
            <div>
                <p className="text-sm font-semibold text-white">Image Spam Thresholds</p>
                <p className="mt-1 text-sm text-slate-400">These values control how aggressively image spam is detected.</p>
            </div>

            <div className="mt-4 grid gap-4 xl:grid-cols-3">
                <label className="border border-slate-800/80 bg-slate-950/50 p-4">
                    <p className="text-sm font-semibold text-white">Window Seconds</p>
                    <p className="mt-1 text-sm text-slate-400">How long the rolling window lasts.</p>
                    <DashboardNumberInput
                        min={1}
                        value={settings.imageSpamWindowSeconds}
                        onChange={value => updateNumber("imageSpamWindowSeconds", value)}
                        disabled={imageSpamDisabled}
                        placeholder="10"
                    />
                </label>

                <label className="border border-slate-800/80 bg-slate-950/50 p-4">
                    <p className="text-sm font-semibold text-white">Minimum Images</p>
                    <p className="mt-1 text-sm text-slate-400">How many images qualify as spam.</p>
                    <DashboardNumberInput
                        min={1}
                        value={settings.imageSpamMinImages}
                        onChange={value => updateNumber("imageSpamMinImages", value)}
                        disabled={imageSpamDisabled}
                        placeholder="3"
                    />
                </label>

                <label className="border border-slate-800/80 bg-slate-950/50 p-4">
                    <p className="text-sm font-semibold text-white">New Member Threshold Hours</p>
                    <p className="mt-1 text-sm text-slate-400">Only members newer than this age are checked.</p>
                    <DashboardNumberInput
                        min={1}
                        value={settings.imageSpamNewMemberThresholdHours}
                        onChange={value => updateNumber("imageSpamNewMemberThresholdHours", value)}
                        disabled={imageSpamDisabled}
                        placeholder="48"
                    />
                </label>
            </div>
        </section>

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
                {isPending ? "Saving..." : "Save Automod"}
            </button>
        </div>
    </form>;
}
