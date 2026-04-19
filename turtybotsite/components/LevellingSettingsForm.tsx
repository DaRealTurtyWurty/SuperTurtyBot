"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardLevellingSettings} from "@/lib/dashboard-api";
import DashboardNumberInput from "@/components/DashboardNumberInput";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import GuildRoleSelect from "@/components/GuildRoleSelect";
import LevellingRoleMappingsEditor, {
    parseLevellingRoleMappings,
    serializeLevellingRoleMappings,
    type LevellingRoleMappingRow
} from "@/components/LevellingRoleMappingsEditor";

interface LevellingSettingsFormProps {
    guildId: string;
    initialSettings: DashboardLevellingSettings;
}

export default function LevellingSettingsForm({guildId, initialSettings}: LevellingSettingsFormProps) {
    const [settings, setSettings] = useState({
        ...initialSettings,
        levelUpMessageChannelId: initialSettings.levelUpMessageChannelId ?? "",
        disabledLevellingChannelIds: initialSettings.disabledLevellingChannelIds,
        levelRoleMappings: parseLevellingRoleMappings(initialSettings.levelRoleMappings),
        xpBoostedChannelIds: initialSettings.xpBoostedChannelIds,
        xpBoostedRoleIds: initialSettings.xpBoostedRoleIds
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const levellingDisabled = !settings.levellingEnabled;
    const levelUpChannelDisabled = levellingDisabled || !settings.hasLevelUpChannel;

    function updateBoolean(
        key: "levellingEnabled" | "disableLevelUpMessages" | "hasLevelUpChannel" | "shouldEmbedLevelUpMessage" | "levelDepletionEnabled" | "doServerBoostsAffectXP",
        value: boolean
    ) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateText(
        key: "levelUpMessageChannelId",
        value: string
    ) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateLevelRoleMappings(value: LevellingRoleMappingRow[]) {
        setSettings(current => ({
            ...current,
            levelRoleMappings: value
        }));
    }

    function updateDisabledLevellingChannelIds(values: string[]) {
        setSettings(current => ({
            ...current,
            disabledLevellingChannelIds: values
        }));
    }

    function updateXpBoostedChannelIds(values: string[]) {
        setSettings(current => ({
            ...current,
            xpBoostedChannelIds: values
        }));
    }

    function updateXpBoostedRoleIds(values: string[]) {
        setSettings(current => ({
            ...current,
            xpBoostedRoleIds: values
        }));
    }

    function updateNumber(key: "levelCooldown" | "minXp" | "maxXp" | "levellingItemChance" | "xpBoostPercentage", value: string) {
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
            const serializedLevelRoles = serializeLevellingRoleMappings(settings.levelRoleMappings);
            if (serializedLevelRoles.error) {
                setError(serializedLevelRoles.error);
                return;
            }

            const response = await fetch(`/api/dashboard/guilds/${guildId}/levelling`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    levellingEnabled: settings.levellingEnabled,
                    levelCooldown: settings.levelCooldown,
                    minXp: settings.minXp,
                    maxXp: settings.maxXp,
                    levellingItemChance: settings.levellingItemChance,
                    disabledLevellingChannelIds: settings.disabledLevellingChannelIds,
                    disableLevelUpMessages: settings.disableLevelUpMessages,
                    hasLevelUpChannel: settings.hasLevelUpChannel,
                    levelUpMessageChannelId: settings.levelUpMessageChannelId.trim() || null,
                    shouldEmbedLevelUpMessage: settings.shouldEmbedLevelUpMessage,
                    levelDepletionEnabled: settings.levelDepletionEnabled,
                    levelRoleMappings: serializedLevelRoles.mappings,
                    xpBoostedChannelIds: settings.xpBoostedChannelIds,
                    xpBoostedRoleIds: settings.xpBoostedRoleIds,
                    xpBoostPercentage: settings.xpBoostPercentage,
                    doServerBoostsAffectXP: settings.doServerBoostsAffectXP
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save levelling settings.");
                return;
            }

            const updated = await response.json() as DashboardLevellingSettings;
            setSettings({
                ...updated,
                levelUpMessageChannelId: updated.levelUpMessageChannelId ?? "",
                disabledLevellingChannelIds: updated.disabledLevellingChannelIds,
                levelRoleMappings: parseLevellingRoleMappings(updated.levelRoleMappings),
                xpBoostedChannelIds: updated.xpBoostedChannelIds,
                xpBoostedRoleIds: updated.xpBoostedRoleIds
            });
            setSuccess("Levelling settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <div className="grid gap-4 md:grid-cols-2">
            <label id="enable-levelling" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Enable Levelling</p>
                        <p className="mt-1 text-sm text-slate-400">Turn XP gain and levelling on or off for this guild.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.levellingEnabled}
                        onChange={event => updateBoolean("levellingEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label id="level-cooldown" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">Level Cooldown</p>
                <p className="mt-1 text-sm text-slate-400">Cooldown in milliseconds between XP gains.</p>
                <DashboardNumberInput
                    id="level-cooldown-input"
                    min={1}
                    value={settings.levelCooldown}
                    onChange={value => updateNumber("levelCooldown", value)}
                    disabled={levellingDisabled}
                    placeholder="1000"
                />
            </label>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
            <label id="minimum-xp" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">Minimum XP</p>
                <DashboardNumberInput
                    id="minimum-xp-input"
                    min={1}
                    max={99}
                    value={settings.minXp}
                    onChange={value => updateNumber("minXp", value)}
                    disabled={levellingDisabled}
                    placeholder="1"
                />
            </label>

            <label id="maximum-xp" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">Maximum XP</p>
                <DashboardNumberInput
                    id="maximum-xp-input"
                    min={1}
                    max={99}
                    value={settings.maxXp}
                    onChange={value => updateNumber("maxXp", value)}
                    disabled={levellingDisabled}
                    placeholder="99"
                />
            </label>

            <label id="item-chance" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">Item Chance</p>
                <p className="mt-1 text-sm text-slate-400">Chance for levelling item drops as a percentage.</p>
                <DashboardNumberInput
                    id="item-chance-input"
                    min={0}
                    max={100}
                    value={settings.levellingItemChance}
                    onChange={value => updateNumber("levellingItemChance", value)}
                    disabled={levellingDisabled}
                    placeholder="0"
                />
            </label>
        </div>

        <GuildChannelSelect
            id="disabled-levelling-channels"
            guildId={guildId}
            value=""
            onChange={() => {}}
            multiple
            values={settings.disabledLevellingChannelIds}
            onValuesChange={updateDisabledLevellingChannelIds}
            disabled={levellingDisabled}
            label="Disabled Levelling Channels"
            description="Messages in these text channels will not award XP."
        />

        <LevellingRoleMappingsEditor
            id="level-roles"
            guildId={guildId}
            value={settings.levelRoleMappings}
            onChange={updateLevelRoleMappings}
            disabled={levellingDisabled}
        />

        <div className="grid gap-4 md:grid-cols-2">
            <label id="disable-level-up-messages" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Disable Level-Up Messages</p>
                        <p className="mt-1 text-sm text-slate-400">Suppress level-up notifications entirely.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.disableLevelUpMessages}
                        onChange={event => updateBoolean("disableLevelUpMessages", event.target.checked)}
                        disabled={levellingDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label id="use-dedicated-level-up-channel" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Use Dedicated Level-Up Channel</p>
                        <p className="mt-1 text-sm text-slate-400">Send level-up messages to one text channel.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.hasLevelUpChannel}
                        onChange={event => updateBoolean("hasLevelUpChannel", event.target.checked)}
                        disabled={levellingDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
            <GuildChannelSelect
                id="level-up-message-channel"
                guildId={guildId}
                value={settings.levelUpMessageChannelId}
                onChange={value => updateText("levelUpMessageChannelId", value)}
                disabled={levelUpChannelDisabled}
                label="Level-Up Message Channel"
                description="Required when the dedicated level-up channel option is enabled."
                placeholder="Select a channel"
            />

            <div className="grid gap-4">
                <label id="embed-level-up-messages" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                    <div className="flex items-center justify-between gap-4">
                        <div>
                            <p className="text-sm font-semibold text-white">Embed Level-Up Messages</p>
                            <p className="mt-1 text-sm text-slate-400">Use embeds rather than plain text for notifications.</p>
                        </div>
                        <input
                            type="checkbox"
                            checked={settings.shouldEmbedLevelUpMessage}
                            onChange={event => updateBoolean("shouldEmbedLevelUpMessage", event.target.checked)}
                            disabled={levellingDisabled}
                            className="h-5 w-5 accent-sky-400"
                        />
                    </div>
                </label>

                <label id="allow-level-depletion" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                    <div className="flex items-center justify-between gap-4">
                        <div>
                            <p className="text-sm font-semibold text-white">Allow Level Depletion</p>
                            <p className="mt-1 text-sm text-slate-400">Allow users to lose levels when the system applies depletion.</p>
                        </div>
                        <input
                            type="checkbox"
                            checked={settings.levelDepletionEnabled}
                            onChange={event => updateBoolean("levelDepletionEnabled", event.target.checked)}
                            disabled={levellingDisabled}
                            className="h-5 w-5 accent-sky-400"
                        />
                    </div>
                </label>
            </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
            <div className="md:col-span-2">
                <GuildChannelSelect
                    id="xp-boosted-channels"
                    guildId={guildId}
                    value=""
                    onChange={() => {}}
                    multiple
                    values={settings.xpBoostedChannelIds}
                    onValuesChange={updateXpBoostedChannelIds}
                    disabled={levellingDisabled}
                    label="XP Boosted Channels"
                    description="Messages in these channels gain the configured XP boost percentage."
                />
            </div>

            <label id="xp-boost-percentage" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">XP Boost Percentage</p>
                <p className="mt-1 text-sm text-slate-400">Applied once for each matching boosted channel, role, and optional server boost.</p>
                <DashboardNumberInput
                    id="xp-boost-percentage-input"
                    min={0}
                    max={1000}
                    value={settings.xpBoostPercentage}
                    onChange={value => updateNumber("xpBoostPercentage", value)}
                    disabled={levellingDisabled}
                    placeholder="0"
                />
            </label>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
            <GuildRoleSelect
                id="xp-boosted-roles"
                guildId={guildId}
                value=""
                onChange={() => {}}
                multiple
                values={settings.xpBoostedRoleIds}
                onValuesChange={updateXpBoostedRoleIds}
                disabled={levellingDisabled}
                label="XP Boosted Roles"
                description="Members with any of these roles receive the configured XP boost."
            />

            <label id="count-server-boosters" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Count Server Boosters</p>
                        <p className="mt-1 text-sm text-slate-400">Apply the XP boost percentage to members who are boosting the server.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.doServerBoostsAffectXP}
                        onChange={event => updateBoolean("doServerBoostsAffectXP", event.target.checked)}
                        disabled={levellingDisabled}
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
                {isPending ? "Saving..." : "Save Levelling"}
            </button>
        </div>
    </form>;
}
