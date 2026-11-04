"use client";

import {useMemo, useRef, useState} from "react";
import {
    COMMAND_TYPES, OPTION_TYPES, commandCooldown, commandMatchesQuery, commandScopes, commandTypes,
    registrationContexts, registrationInstalls
} from "@/lib/command-metadata";
import type {CommandMetadata, CommandOption, CommandSettings} from "@/lib/command-metadata";

interface CommandExplorerProps {
    guildId: string;
    commands: CommandMetadata[];
    initialSettings: CommandSettings;
}

const INPUT_STYLE = "w-full border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-sky-400";
const BUTTON_STYLE = "border border-slate-600 px-3 py-2 text-sm font-medium hover:border-sky-400 hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-400 disabled:cursor-wait disabled:opacity-50";
const PAGE_SIZE = 20;

function Options({options, path}: {options: CommandOption[]; path: string}) {
    return <ul className="space-y-3">
        {options.map(option => {
            const nested = option.type === 1 || option.type === 2;
            return <li key={option.name} className="border-l border-slate-700 pl-4">
                <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                    <code className="break-all text-sm text-sky-200">{nested ? `${path} ${option.name}` : option.name}</code>
                    <span className="text-xs text-slate-400">{OPTION_TYPES[option.type] ?? `Type ${option.type}`}</span>
                    {!nested && <span className={`text-xs ${option.required ? "text-amber-200" : "text-slate-400"}`}>
                        {option.required ? "Required" : "Optional"}
                    </span>}
                </div>
                <p className="mt-1 break-words text-sm leading-6 text-slate-300">{option.description}</p>
                {option.autocomplete && <p className="mt-1 text-xs text-slate-400">Supports autocomplete</p>}
                {option.choices && option.choices.length > 0 && <p className="mt-1 break-words text-xs leading-5 text-slate-400">
                    Choices: {option.choices.map(choice => `${choice.name} (${choice.value})`).join(", ")}
                </p>}
                {(option.min_value !== undefined || option.max_value !== undefined) && <p className="mt-1 text-xs text-slate-400">
                    Value: {option.min_value ?? "No minimum"} to {option.max_value ?? "No maximum"}
                </p>}
                {(option.min_length !== undefined || option.max_length !== undefined) && <p className="mt-1 text-xs text-slate-400">
                    Length: {option.min_length ?? 0} to {option.max_length ?? "No maximum"} characters
                </p>}
                {option.options && option.options.length > 0 && <div className="mt-3">
                    <Options options={option.options} path={`${path} ${option.name}`} />
                </div>}
            </li>;
        })}
    </ul>;
}

function CommandDetails({command}: {command: CommandMetadata}) {
    return <div className="space-y-6 border-t border-slate-800 pb-5 pt-5">
        <dl className="grid gap-4 text-sm sm:grid-cols-2">
            <div><dt className="text-slate-400">Permissions / access</dt><dd className="mt-1 text-slate-200">{command.access}</dd></div>
            <div><dt className="text-slate-400">Cooldown per user</dt><dd className="mt-1 text-slate-200">{commandCooldown(command)}</dd></div>
            <div><dt className="text-slate-400">Command scope</dt><dd className="mt-1 text-slate-200">{commandScopes(command)}</dd></div>
            <div><dt className="text-slate-400">Command types</dt><dd className="mt-1 text-slate-200">{commandTypes(command).join(", ")}</dd></div>
        </dl>
        <div>
            <h4 className="mb-2 text-sm font-semibold">Usage</h4>
            <pre className="whitespace-pre-wrap break-words border border-slate-800 bg-slate-950 p-3 text-sm leading-6 text-sky-200">{command.usage}</pre>
        </div>
        {command.registrations.map(registration => <section key={`${registration.type}-${registration.name}`} className="space-y-3">
            <h4 className="break-words text-sm font-semibold">
                {registration.type === 1 ? `/${registration.name}` : registration.name}
                <span className="ml-2 font-normal text-slate-400">{registration.type === 1 ? "Slash command" : registration.type === 2 ? "User context menu" : "Message context menu"}</span>
            </h4>
            <dl className="grid gap-3 text-xs sm:grid-cols-2">
                <div><dt className="text-slate-400">Discord registration contexts</dt><dd className="mt-1 text-slate-300">{registrationContexts(registration)}</dd></div>
                <div><dt className="text-slate-400">Install scopes</dt><dd className="mt-1 text-slate-300">{registrationInstalls(registration)}</dd></div>
                {registration.default_member_permissions != null && <div>
                    <dt className="text-slate-400">Default member permission bits</dt><dd className="mt-1 text-slate-300">{registration.default_member_permissions}</dd>
                </div>}
            </dl>
            {registration.nsfw && <p className="text-xs text-amber-200">Age-restricted command</p>}
            {registration.options && registration.options.length > 0 && <div className="space-y-3">
                <h5 className="text-sm font-medium text-slate-300">Subcommands and options</h5>
                <Options options={registration.options} path={`/${registration.name}`} />
            </div>}
        </section>)}
    </div>;
}

export default function CommandExplorer({guildId, commands, initialSettings}: CommandExplorerProps) {
    const [settings, setSettings] = useState(initialSettings);
    const [query, setQuery] = useState("");
    const [category, setCategory] = useState("");
    const [type, setType] = useState("");
    const [access, setAccess] = useState("");
    const [scope, setScope] = useState("");
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(1);
    const [saving, setSaving] = useState<string | null>(null);
    const busy = useRef(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState("");
    const disabled = useMemo(() => new Set(settings.disabledCommands), [settings]);
    const categories = useMemo(() => [...new Set(commands.map(command => command.category))].sort(), [commands]);
    const accessLevels = useMemo(() => [...new Set(commands.map(command => command.access))].sort(), [commands]);
    const filtered = useMemo(() => commands.filter(command =>
        commandMatchesQuery(command, query) && (!category || command.category === category)
        && (!type || commandTypes(command).includes(type)) && (!access || command.access === access)
        && (!scope || (scope === "server" ? command.serverOnly : !command.serverOnly))
        && (!status || disabled.has(command.name) === (status === "disabled"))
    ).sort((a, b) => a.name.localeCompare(b.name)), [commands, query, category, type, access, scope, status, disabled]);
    const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    const currentPage = Math.min(page, pageCount);
    const visible = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

    function filter(setValue: (value: string) => void, value: string) {
        setValue(value);
        setPage(1);
    }

    function clearFilters() {
        setQuery(""); setCategory(""); setType(""); setAccess(""); setScope(""); setStatus(""); setPage(1);
    }

    async function toggle(command: CommandMetadata) {
        if (busy.current) return;
        busy.current = true;
        const enabled = disabled.has(command.name);
        setSaving(command.name);
        setError(null);
        setNotice("");
        try {
            const response = await fetch(`/api/dashboard/guilds/${guildId}/commands/${encodeURIComponent(command.name)}`, {
                method: "PATCH", headers: {"Content-Type": "application/json"}, body: JSON.stringify({enabled})
            });
            const payload = await response.json() as Partial<CommandSettings> & {message?: string};
            if (!response.ok || !Array.isArray(payload.disabledCommands)) {
                throw new Error(payload.message ?? "The command could not be saved. Try again shortly.");
            }
            setSettings({disabledCommands: payload.disabledCommands});
            setNotice(`${command.name} ${enabled ? "enabled" : "disabled"} in this server.`);
        } catch (error) {
            setError(error instanceof Error ? error.message : "The command could not be saved. Try again shortly.");
        } finally {
            busy.current = false;
            setSaving(null);
        }
    }

    return <div className="space-y-6">
        <header className="max-w-3xl space-y-2">
            <h2 className="text-3xl font-bold tracking-tight">Command explorer</h2>
            <p className="text-sm leading-6 text-slate-400">Find a command and open its details for permissions, cooldowns, scopes, and subcommands.</p>
        </header>
        <section aria-label="Filter commands" className="space-y-4 border-y border-slate-700 py-5">
            <label className="block space-y-2 text-sm font-medium">
                <span>Search commands</span>
                <input type="search" value={query} onChange={event => filter(setQuery, event.target.value)} className={INPUT_STYLE}
                    placeholder="Search names, descriptions, permissions, or subcommands" />
            </label>
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <label className="space-y-1 text-sm text-slate-300">Category
                    <select value={category} onChange={event => filter(setCategory, event.target.value)} className={INPUT_STYLE}>
                        <option value="">All categories</option>{categories.map(value => <option key={value}>{value}</option>)}
                    </select>
                </label>
                <label className="space-y-1 text-sm text-slate-300">Command type
                    <select value={type} onChange={event => filter(setType, event.target.value)} className={INPUT_STYLE}>
                        <option value="">All types</option>{Object.values(COMMAND_TYPES).map(value => <option key={value}>{value}</option>)}
                    </select>
                </label>
                <label className="space-y-1 text-sm text-slate-300">Permissions / access
                    <select value={access} onChange={event => filter(setAccess, event.target.value)} className={INPUT_STYLE}>
                        <option value="">All access levels</option>{accessLevels.map(value => <option key={value}>{value}</option>)}
                    </select>
                </label>
                <label className="space-y-1 text-sm text-slate-300">Command scope
                    <select value={scope} onChange={event => filter(setScope, event.target.value)} className={INPUT_STYLE}>
                        <option value="">All scopes</option><option value="server">Server only</option><option value="dm">Server and direct messages</option>
                    </select>
                </label>
                <label className="space-y-1 text-sm text-slate-300">Server availability
                    <select value={status} onChange={event => filter(setStatus, event.target.value)} className={INPUT_STYLE}>
                        <option value="">All commands</option><option value="enabled">Enabled</option><option value="disabled">Disabled</option>
                    </select>
                </label>
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3">
                <p role="status" className="text-sm text-slate-400">{filtered.length} of {commands.length} commands</p>
                <button type="button" onClick={clearFilters} className={BUTTON_STYLE}>Clear filters</button>
            </div>
        </section>
        <p className="max-w-3xl text-sm leading-6 text-slate-400">
            Server availability saves immediately and applies to every command type and subcommand in this server.
            Enabling a command still requires its usual permissions and module settings. Direct messages are unaffected.
        </p>
        {error && <p role="alert" className="border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-100">{error}</p>}
        <p role="status" aria-live="polite" className="text-sm text-sky-200">{saving ? `Saving ${saving}...` : notice}</p>
        {visible.length === 0 ? <div className="border border-dashed border-slate-700 p-6">
            <h3 className="font-semibold">No commands match these filters</h3>
            <p className="mt-2 text-sm text-slate-400">Try another search or clear the filters to see all commands.</p>
        </div> : <div className="divide-y divide-slate-700 border-y border-slate-700">
            {visible.map(command => <article key={command.name} className="flex flex-col gap-4 py-5 xl:flex-row xl:gap-6">
                <details className="min-w-0 flex-1">
                    <summary className="cursor-pointer rounded-sm text-sky-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-sky-400">
                        <code className="break-all text-lg font-semibold">{command.types.slash ? "/" : command.types.prefix ? "." : ""}{command.name}</code>
                        <span className="ml-3 text-xs text-slate-400">{command.category}</span>
                        <span className="ml-3 text-xs text-slate-300">View details</span>
                        <span className="mt-2 block max-w-3xl break-words text-sm leading-6 text-slate-300">{command.description}</span>
                    </summary>
                    <CommandDetails command={command} />
                </details>
                <div className="space-y-2 xl:w-44 xl:shrink-0">
                    <p className="text-xs text-slate-400">Server availability</p>
                    <button type="button" role="switch" aria-checked={!disabled.has(command.name)}
                        aria-label={`${command.name} enabled in this server`} disabled={saving !== null} onClick={() => toggle(command)}
                        className={`${BUTTON_STYLE} w-full ${disabled.has(command.name) ? "text-slate-300" : "border-sky-700 bg-sky-400/10 text-sky-200"}`}>
                        {saving === command.name ? "Saving..." : disabled.has(command.name) ? "Disabled" : "Enabled"}
                    </button>
                </div>
            </article>)}
        </div>}
        {pageCount > 1 && <nav aria-label="Command pages" className="flex flex-wrap items-center justify-between gap-3">
            <button type="button" className={BUTTON_STYLE} disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>Previous</button>
            <span className="text-sm text-slate-400">Page {currentPage} of {pageCount}</span>
            <button type="button" className={BUTTON_STYLE} disabled={currentPage === pageCount} onClick={() => setPage(currentPage + 1)}>Next</button>
        </nav>}
    </div>;
}
