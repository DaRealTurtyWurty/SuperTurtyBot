"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import type {DashboardEconomySettings} from "@/lib/dashboard-api";
import DashboardNumberInput from "@/components/DashboardNumberInput";
import DashboardPresetSelect from "@/components/DashboardPresetSelect";

const CURRENCY_PRESETS = [
    {label: "Dollar ($)", value: "$"},
    {label: "Euro (€)", value: "€"},
    {label: "Pound (£)", value: "£"},
    {label: "Yen (¥)", value: "¥"},
    {label: "Custom", value: "__custom__"}
] as const;

function getCurrencyMode(currency: string) {
    return CURRENCY_PRESETS.some(option => option.value === currency) ? currency : "__custom__";
}

interface EconomySettingsFormProps {
    guildId: string;
    initialSettings: DashboardEconomySettings;
}

export default function EconomySettingsForm({guildId, initialSettings}: EconomySettingsFormProps) {
    const initialCurrencyMode = getCurrencyMode(initialSettings.economyCurrency);
    const [settings, setSettings] = useState({
        ...initialSettings,
        currencyMode: initialCurrencyMode,
        customCurrencyText: initialCurrencyMode === "__custom__" ? initialSettings.economyCurrency : "",
        incomeTaxText: initialSettings.incomeTax.toString()
    });
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const economyDisabled = !settings.economyEnabled;

    function updateBoolean(key: "economyEnabled" | "donateEnabled", value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateText(key: "customCurrencyText" | "defaultEconomyBalance" | "incomeTaxText", value: string) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateCurrencyMode(value: string) {
        setSettings(current => ({
            ...current,
            currencyMode: value,
            customCurrencyText: value === "__custom__" ? current.customCurrencyText || current.economyCurrency : current.customCurrencyText
        }));
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/economy`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    economyCurrency: settings.currencyMode === "__custom__" ? settings.customCurrencyText : settings.currencyMode,
                    economyEnabled: settings.economyEnabled,
                    donateEnabled: settings.donateEnabled,
                    defaultEconomyBalance: settings.defaultEconomyBalance,
                    incomeTax: Number.parseFloat(settings.incomeTaxText)
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save economy settings.");
                return;
            }

            const updated = await response.json() as DashboardEconomySettings;
            const nextCurrencyMode = getCurrencyMode(updated.economyCurrency);
            setSettings({
                ...updated,
                currencyMode: nextCurrencyMode,
                customCurrencyText: nextCurrencyMode === "__custom__" ? updated.economyCurrency : "",
                incomeTaxText: updated.incomeTax.toString()
            });
            setSuccess("Economy settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <div className="grid gap-4 md:grid-cols-2">
            <label id="enable-economy" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Enable Economy</p>
                        <p className="mt-1 text-sm text-slate-400">Turn the guild economy on or off.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.economyEnabled}
                        onChange={event => updateBoolean("economyEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label id="enable-donations" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Enable Donations</p>
                        <p className="mt-1 text-sm text-slate-400">Allow users to donate currency to each other.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.donateEnabled}
                        onChange={event => updateBoolean("donateEnabled", event.target.checked)}
                        disabled={economyDisabled}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
            <div>
                <DashboardPresetSelect
                    id="currency"
                    label="Currency"
                    description="Short symbol or label shown in economy responses."
                    value={settings.currencyMode}
                    onChange={updateCurrencyMode}
                    disabled={economyDisabled}
                    options={CURRENCY_PRESETS.filter(option => option.value !== "__custom__")}
                    customValue={settings.customCurrencyText}
                    onCustomValueChange={value => updateText("customCurrencyText", value)}
                    customPlaceholder="Custom symbol or label"
                />
            </div>

            <label id="default-balance" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">Default Balance</p>
                <p className="mt-1 text-sm text-slate-400">Starting balance for new economy accounts.</p>
                <DashboardNumberInput
                    min={0}
                    value={settings.defaultEconomyBalance}
                    onChange={value => updateText("defaultEconomyBalance", value)}
                    disabled={economyDisabled}
                    placeholder="200"
                />
            </label>

            <label id="income-tax" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <p className="text-sm font-semibold text-white">Income Tax</p>
                <p className="mt-1 text-sm text-slate-400">Decimal between 0 and 1. For example, 0.1 means 10%.</p>
                <DashboardNumberInput
                    min={0}
                    max={1}
                    step={0.01}
                    value={settings.incomeTaxText}
                    onChange={value => updateText("incomeTaxText", value)}
                    disabled={economyDisabled}
                    placeholder="0.1"
                />
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
                {isPending ? "Saving..." : "Save Economy"}
            </button>
        </div>
    </form>;
}
