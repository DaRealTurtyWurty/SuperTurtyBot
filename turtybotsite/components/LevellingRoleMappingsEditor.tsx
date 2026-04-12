"use client";

import {useMemo} from "react";
import DashboardNumberInput from "@/components/DashboardNumberInput";
import GuildRoleSelect from "@/components/GuildRoleSelect";

export interface LevellingRoleMappingRow {
    id: string;
    level: string;
    roleId: string;
}

export function createLevellingRoleMappingRow(level = "", roleId = ""): LevellingRoleMappingRow {
    return {
        id: crypto.randomUUID(),
        level,
        roleId
    };
}

export function parseLevellingRoleMappings(mappings: string[]): LevellingRoleMappingRow[] {
    if (!mappings.length) {
        return [createLevellingRoleMappingRow()];
    }

    return mappings.map(mapping => {
        const [level, roleId] = mapping.split("->");
        return createLevellingRoleMappingRow(level?.trim() ?? "", roleId?.trim() ?? "");
    });
}

export function serializeLevellingRoleMappings(rows: LevellingRoleMappingRow[]) {
    const mappings: string[] = [];

    for (const row of rows) {
        const level = row.level.trim();
        const roleId = row.roleId.trim();

        if (!level && !roleId) {
            continue;
        }

        if (!level || !roleId) {
            return {
                mappings: [],
                error: "Complete or remove incomplete level role mappings."
            };
        }

        const parsedLevel = Number.parseInt(level, 10);
        if (!Number.isInteger(parsedLevel) || parsedLevel <= 0) {
            return {
                mappings: [],
                error: "Level role mappings must use positive whole numbers for levels."
            };
        }

        mappings.push(`${parsedLevel}->${roleId}`);
    }

    return {
        mappings,
        error: null
    };
}

interface LevellingRoleMappingsEditorProps {
    guildId: string;
    disabled?: boolean;
    value: LevellingRoleMappingRow[];
    onChange: (value: LevellingRoleMappingRow[]) => void;
}

export default function LevellingRoleMappingsEditor({
    guildId,
    disabled,
    value,
    onChange
}: LevellingRoleMappingsEditorProps) {
    const selectedRoleIds = useMemo(
        () => value.map(entry => entry.roleId).filter(Boolean),
        [value]
    );

    function updateRow(index: number, nextRow: Partial<LevellingRoleMappingRow>) {
        onChange(value.map((row, rowIndex) => rowIndex === index ? {...row, ...nextRow} : row));
    }

    function addRow() {
        onChange([...value, createLevellingRoleMappingRow()]);
    }

    function removeRow(index: number) {
        const next = value.filter((_, rowIndex) => rowIndex !== index);
        onChange(next.length > 0 ? next : [createLevellingRoleMappingRow()]);
    }

    return <div className="border border-slate-800/80 bg-slate-950/60 p-5">
        <div className="flex items-start justify-between gap-4">
            <div>
                <p className="text-sm font-semibold text-white">Level Roles</p>
                <p className="mt-1 text-sm text-slate-400">
                    Add one row per reward. Each row pairs a level with a role.
                </p>
            </div>
            <button
                type="button"
                onClick={addRow}
                disabled={disabled}
                className="border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-semibold text-slate-100 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
                Add Row
            </button>
        </div>

        <div className="mt-4 space-y-4">
            {value.map((row, index) => {
                const otherRoleIds = selectedRoleIds.filter(roleId => roleId !== row.roleId);

                return <div key={row.id} className="grid gap-3 md:grid-cols-[140px_minmax(0,1fr)_auto]">
                    <label className="block border border-slate-800/80 bg-slate-900/70 px-4 py-3">
                        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Level</p>
                        <DashboardNumberInput
                            min={1}
                            value={row.level}
                            onChange={value => updateRow(index, {level: value})}
                            disabled={disabled}
                            placeholder="10"
                            className="mt-2"
                        />
                    </label>

                    <GuildRoleSelect
                        guildId={guildId}
                        value={row.roleId}
                        onChange={roleId => updateRow(index, {roleId})}
                        excludedRoleIds={otherRoleIds}
                        disabled={disabled}
                        label="Role"
                        description="Select the role awarded at this level."
                        placeholder="Select a role"
                    />

                    <button
                        type="button"
                        onClick={() => removeRow(index)}
                        disabled={disabled}
                        className="self-end border border-slate-700 bg-slate-900 px-4 py-3 text-sm font-semibold text-slate-200 transition hover:border-slate-600 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        Remove
                    </button>
                </div>;
            })}
        </div>
    </div>;
}
