"use client";

import {useEffect, useMemo, useRef, useState} from "react";

export interface DashboardGuildRoleOption {
    id: string;
    name: string;
    color: number;
    position: number;
}

interface GuildRoleSelectProps {
    guildId: string;
    value?: string;
    onChange?: (value: string) => void;
    disabled?: boolean;
    label: string;
    description?: string;
    placeholder?: string;
    multiple?: boolean;
    values?: string[];
    onValuesChange?: (values: string[]) => void;
    excludedRoleIds?: string[];
}

function discordColorToHex(color: number): string {
    if (color === 0) return "#FFFFFF";
    return `#${color.toString(16).padStart(6, "0")}`;
}

export function useGuildRoles(guildId: string) {
    const [roles, setRoles] = useState<DashboardGuildRoleOption[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let isCancelled = false;

        setIsLoading(true);
        setError(null);

        fetch(`/api/discord/guilds/${guildId}/roles`, {
            cache: "no-store"
        })
            .then(async response => {
                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to load guild roles.");
                }

                return response.json() as Promise<{roles: DashboardGuildRoleOption[]}>;
            })
            .then(payload => {
                if (!isCancelled) {
                    setRoles(payload.roles);
                    setIsLoading(false);
                }
            })
            .catch(fetchError => {
                if (!isCancelled) {
                    setError(fetchError instanceof Error ? fetchError.message : "Failed to load guild roles.");
                    setIsLoading(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [guildId]);

    return {roles, isLoading, error};
}

function formatSummary(
    selectedRoles: DashboardGuildRoleOption[],
    placeholder: string,
    multiple: boolean
) {
    if (selectedRoles.length === 0) {
        return placeholder;
    }

    if (!multiple || selectedRoles.length === 1) {
        return selectedRoles[0].name;
    }

    return `${selectedRoles.length} roles selected`;
}

export default function GuildRoleSelect({
    guildId,
    value = "",
    onChange,
    disabled,
    label,
    description,
    placeholder = "Select a role",
    multiple = false,
    values = [],
    onValuesChange,
    excludedRoleIds = []
}: GuildRoleSelectProps) {
    const {roles, isLoading, error} = useGuildRoles(guildId);
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement | null>(null);
    const excludedSet = useMemo(() => new Set(excludedRoleIds), [excludedRoleIds]);

    const selectedIds = multiple ? values : value ? [value] : [];
    const selectedRoles = useMemo(
        () => roles.filter(role => selectedIds.includes(role.id)),
        [roles, selectedIds]
    );
    const summary = formatSummary(selectedRoles, placeholder, multiple);

    useEffect(() => {
        function onDocumentClick(event: MouseEvent) {
            if (!containerRef.current?.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }

        if (isOpen) {
            document.addEventListener("mousedown", onDocumentClick);
        }

        return () => {
            document.removeEventListener("mousedown", onDocumentClick);
        };
    }, [isOpen]);

    function toggleRole(roleId: string) {
        if (multiple) {
            const nextValues = values.includes(roleId)
                ? values.filter(valueEntry => valueEntry !== roleId)
                : [...values, roleId];
            onValuesChange?.(nextValues);
            return;
        }

        onChange?.(roleId);
        setIsOpen(false);
    }

    return <div ref={containerRef} className="block border border-slate-800/80 bg-slate-950/60 p-5">
        <p className="text-sm font-semibold text-white">{label}</p>
        {description ? <p className="mt-1 text-sm text-slate-400">{description}</p> : null}

        <div className="relative mt-4">
            <button
                type="button"
                onClick={() => setIsOpen(current => !current)}
                disabled={disabled || isLoading}
                className="flex min-h-12 w-full items-center justify-between gap-3 border border-slate-700 bg-slate-900 px-4 py-3 text-left text-sm text-white outline-none transition hover:border-slate-600 focus:border-sky-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
                <span className="flex min-w-0 items-center gap-2 text-slate-100">
                    {selectedRoles.length > 0 ? (
                        <span
                            className="h-4 w-4 shrink-0 rounded"
                            style={{backgroundColor: discordColorToHex(selectedRoles[0].color)}}
                            aria-hidden="true"
                        />
                    ) : null}
                    <span className="truncate">
                        {isLoading ? "Loading roles..." : summary}
                    </span>
                </span>
                <svg
                    className={`h-4 w-4 shrink-0 text-slate-400 transition ${isOpen ? "rotate-180" : ""}`}
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                >
                    <path d="m6 9 6 6 6-6" />
                </svg>
            </button>

            {isOpen && !isLoading && roles.length > 0 ? (
                <div className="dashboard-scrollbar absolute left-0 right-0 top-full z-30 mt-1 max-h-96 overflow-y-auto border border-slate-700 bg-slate-900 shadow-2xl">
                    <div className="p-3">
                        {roles.map(role => {
                            const isSelected = selectedIds.includes(role.id);
                            const isAllowed = !excludedSet.has(role.id) || isSelected;

                            return <button
                                key={role.id}
                                type="button"
                                onClick={() => isAllowed ? toggleRole(role.id) : undefined}
                                disabled={!isAllowed}
                                className={`flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm transition ${
                                    !isAllowed
                                        ? "cursor-not-allowed text-slate-600"
                                        : isSelected
                                        ? "bg-sky-400/12 text-sky-100"
                                        : "text-slate-200 hover:bg-slate-800 hover:text-white"
                                }`}
                            >
                                <span className="flex min-w-0 items-center gap-2">
                                    <span
                                        className="h-4 w-4 shrink-0 rounded"
                                        style={{backgroundColor: discordColorToHex(role.color)}}
                                        aria-hidden="true"
                                    />
                                    <span className="truncate">{role.name}</span>
                                </span>
                                {isSelected ? (
                                    <svg
                                        className="h-4 w-4 shrink-0 text-sky-300"
                                        viewBox="0 0 24 24"
                                        fill="none"
                                        stroke="currentColor"
                                        strokeWidth="2"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        aria-hidden="true"
                                    >
                                        <path d="M20 6 9 17l-5-5" />
                                    </svg>
                                ) : null}
                            </button>;
                        })}
                    </div>
                </div>
            ) : null}

            {isOpen && !isLoading && roles.length === 0 && !error ? (
                <div className="dashboard-scrollbar absolute left-0 right-0 top-full z-30 mt-1 border border-slate-700 bg-slate-900 p-3 shadow-2xl">
                    <p className="text-sm text-slate-400">No roles found for this server.</p>
                </div>
            ) : null}
        </div>

        {error ? <p className="mt-3 text-sm text-red-300">{error}</p> : null}
        {!error && !isLoading && roles.length > 0 ? (
            <p className="mt-3 text-xs text-slate-500">
                Select a role to configure it.
            </p>
        ) : null}
    </div>;
}
