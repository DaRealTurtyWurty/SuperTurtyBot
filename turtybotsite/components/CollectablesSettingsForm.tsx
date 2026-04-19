"use client";

import type {FormEvent} from "react";
import {useState, useTransition} from "react";
import Link from "next/link";
import type {DashboardCollectablesSettings} from "@/lib/dashboard-api";
import GuildChannelSelect from "@/components/GuildChannelSelect";
import CollectableEmoji from "@/components/CollectableEmoji";

interface CollectablesSettingsFormProps {
    guildId: string;
    initialSettings: DashboardCollectablesSettings;
    collectionType?: string;
}

type CollectablesSettingsState = Omit<DashboardCollectablesSettings, "collectorChannelId"> & {
    collectorChannelId: string;
};

function cloneSettings(initialSettings: DashboardCollectablesSettings): CollectablesSettingsState {
    return {
        ...initialSettings,
        collectorChannelId: initialSettings.collectorChannelId ?? "",
        enabledCollectableTypeIds: [...initialSettings.enabledCollectableTypeIds],
        collections: initialSettings.collections.map(collection => ({
            ...collection,
            disabledCollectables: [...collection.disabledCollectables],
            collectables: [...collection.collectables]
        }))
    };
}

export default function CollectablesSettingsForm({guildId, initialSettings, collectionType}: CollectablesSettingsFormProps) {
    const [settings, setSettings] = useState(() => cloneSettings(initialSettings));
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();
    const selectedCollection = collectionType ? settings.collections.find(collection => collection.type === collectionType) ?? null : null;
    const visibleCollections = selectedCollection ? [selectedCollection] : settings.collections;

    function updateBoolean(key: "collectingEnabled" | "collectableTypesRestricted", value: boolean) {
        setSettings(current => ({
            ...current,
            [key]: value
        }));
    }

    function updateChannel(value: string) {
        setSettings(current => ({
            ...current,
            collectorChannelId: value
        }));
    }

    function toggleCollectionType(type: string, checked: boolean) {
        setSettings(current => ({
            ...current,
            enabledCollectableTypeIds: checked
                ? [...current.enabledCollectableTypeIds, type]
                : current.enabledCollectableTypeIds.filter(entry => entry !== type)
        }));
    }

    function toggleDisabledCollectable(collectionType: string, collectableName: string, checked: boolean) {
        setSettings(current => ({
            ...current,
            collections: current.collections.map(collection => {
                if (collection.type !== collectionType) {
                    return collection;
                }

                return {
                    ...collection,
                    disabledCollectables: checked
                        ? [...collection.disabledCollectables, collectableName]
                        : collection.disabledCollectables.filter(entry => entry !== collectableName)
                };
            })
        }));
    }

    function isCollectionTypeEnabled(type: string) {
        return !settings.collectableTypesRestricted || settings.enabledCollectableTypeIds.includes(type);
    }

    function collectionPageHref(type: string) {
        return `/dashboard/${guildId}/collectables/${type}`;
    }

    function onSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setSuccess(null);

        startTransition(async () => {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/collectables`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    collectorChannelId: settings.collectorChannelId.trim() || null,
                    collectingEnabled: settings.collectingEnabled,
                    collectableTypesRestricted: settings.collectableTypesRestricted,
                    enabledCollectableTypeIds: settings.enabledCollectableTypeIds,
                    disabledCollectablesByType: Object.fromEntries(settings.collections.map(collection => [
                        collection.type,
                        collection.disabledCollectables
                    ]))
                })
            });

            if (!response.ok) {
                const payload = await response.json().catch(() => null) as {message?: string} | null;
                setError(payload?.message ?? "Failed to save collectables settings.");
                return;
            }

            const updated = await response.json() as DashboardCollectablesSettings;
            setSettings(cloneSettings(updated));
            setSuccess("Collectables settings saved.");
        });
    }

    return <form onSubmit={onSubmit} className="space-y-5">
        <GuildChannelSelect
            id="collector-channel"
            guildId={guildId}
            value={settings.collectorChannelId}
            onChange={updateChannel}
            label="Collector Channel"
            description="Collectable messages will appear in this text channel."
            placeholder="Select a channel"
        />

        <div className="grid gap-4 md:grid-cols-2">
            <label id="enable-collectables" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Enable Collectables</p>
                        <p className="mt-1 text-sm text-slate-400">Allow the collectable game to run in this server.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.collectingEnabled}
                        onChange={event => updateBoolean("collectingEnabled", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>

            <label id="restrict-collection-types" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <p className="text-sm font-semibold text-white">Restrict Collection Types</p>
                        <p className="mt-1 text-sm text-slate-400">Only selected collections will be used when spawning collectables.</p>
                    </div>
                    <input
                        type="checkbox"
                        checked={settings.collectableTypesRestricted}
                        onChange={event => updateBoolean("collectableTypesRestricted", event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </div>
            </label>
        </div>

        {settings.collectableTypesRestricted ? <section id="enabled-collections" className="border border-slate-800/80 bg-slate-950/60 p-5 scroll-mt-24">
            <h3 className="text-lg font-semibold text-white">Enabled Collections</h3>
            <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {settings.collections.map(collection => <label key={collection.type} className="flex items-center justify-between gap-4 border border-slate-800/80 bg-slate-950/50 px-4 py-3">
                    <span className="text-sm font-semibold text-white">{collection.displayName}</span>
                    <input
                        type="checkbox"
                        checked={settings.enabledCollectableTypeIds.includes(collection.type)}
                        onChange={event => toggleCollectionType(collection.type, event.target.checked)}
                        className="h-5 w-5 accent-sky-400"
                    />
                </label>)}
            </div>
        </section> : null}

        {selectedCollection ? <section id={selectedCollection.type.replace(/_/g, "-")} className="space-y-4 scroll-mt-24">
            <div className="flex flex-wrap items-end justify-between gap-4">
                <div>
                    <h3 className="text-lg font-semibold text-white">{selectedCollection.displayName}</h3>
                    <p className="mt-1 text-sm text-slate-400">
                        {selectedCollection.collectables.length} collectables.
                        {isCollectionTypeEnabled(selectedCollection.type) ? "" : " Disabled in the collection type filter."}
                    </p>
                </div>

                <Link
                    href={`/dashboard/${guildId}/collectables`}
                    className="text-sm font-semibold text-sky-300 hover:text-sky-200"
                >
                    Back to overview
                </Link>
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {selectedCollection.collectables.map(collectable => {
                    const disabled = selectedCollection.disabledCollectables.includes(collectable.name);

                    return <label
                        key={collectable.name}
                        className="flex items-start justify-between gap-3 border border-slate-800/80 bg-slate-950/50 px-4 py-3"
                    >
                        <span className="min-w-0">
                            <span className="flex items-center gap-2 text-sm font-semibold text-white">
                                <CollectableEmoji emoji={collectable.emoji} label={collectable.richName} />
                                <span className="truncate">{collectable.richName}</span>
                            </span>
                            <span className="mt-1 block text-xs text-slate-500">
                                {collectable.rarity}{collectable.note ? ` · ${collectable.note}` : ""}
                            </span>
                        </span>
                        <input
                            type="checkbox"
                            checked={disabled}
                            onChange={event => toggleDisabledCollectable(selectedCollection.type, collectable.name, event.target.checked)}
                            className="h-5 w-5 accent-sky-400"
                        />
                    </label>;
                })}
            </div>
        </section> : <section className="space-y-4">
            <div>
                <h3 className="text-lg font-semibold text-white">Collections</h3>
                <p className="mt-1 text-sm text-slate-400">Open a collection page to manage disabled collectables for that type.</p>
            </div>

            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {visibleCollections.map(collection => {
                    const enabled = isCollectionTypeEnabled(collection.type);

                    return <article
                        key={collection.type}
                        id={collection.type.replace(/_/g, "-")}
                        className={`border border-slate-800/80 bg-slate-950/60 p-5 ${enabled ? "" : "opacity-75"}`}
                    >
                        <div className="flex items-start justify-between gap-4">
                            <div className="min-w-0">
                                <h4 className="text-base font-semibold text-white">{collection.displayName}</h4>
                                <p className="mt-1 text-sm text-slate-400">
                                    {collection.collectables.length} collectables.
                                    {collection.disabledCollectables.length} disabled.
                                </p>
                            </div>
                            <span className={`shrink-0 border px-2 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] ${
                                enabled
                                    ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                                    : "border-amber-500/30 bg-amber-500/10 text-amber-200"
                            }`}>
                                {enabled ? "Enabled" : "Disabled"}
                            </span>
                        </div>

                        <div className="mt-4 flex flex-wrap gap-2">
                            {collection.collectables.slice(0, 6).map(collectable => <span
                                key={collectable.name}
                                className="rounded border border-slate-800/80 bg-slate-950/50 px-2 py-1 text-sm text-white"
                                title={collectable.richName}
                            >
                                <CollectableEmoji emoji={collectable.emoji} label={collectable.richName} />
                            </span>)}
                        </div>

                        <div className="mt-5 flex items-center justify-between gap-3">
                            <Link
                                href={collectionPageHref(collection.type)}
                                className="text-sm font-semibold text-sky-300 hover:text-sky-200"
                            >
                                Open collection
                            </Link>
                            <span className="text-xs text-slate-500">Manage disabled items</span>
                        </div>
                    </article>;
                })}
            </div>
        </section>}

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
                {isPending ? "Saving..." : "Save Collectables"}
            </button>
        </div>
    </form>;
}
