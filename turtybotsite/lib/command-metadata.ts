export interface CommandOption {
    name: string;
    description: string;
    type: number;
    required?: boolean;
    autocomplete?: boolean;
    options?: CommandOption[];
    choices?: Array<{name: string; value: string | number}>;
    min_value?: number;
    max_value?: number;
    min_length?: number;
    max_length?: number;
    channel_types?: number[];
}

export interface CommandRegistration {
    name: string;
    type: number;
    description?: string;
    contexts?: Array<string | number>;
    integration_types?: Array<string | number>;
    default_member_permissions?: string | null;
    nsfw?: boolean;
    options?: CommandOption[];
}

export interface CommandMetadata {
    name: string;
    richName: string;
    description: string;
    category: string;
    access: string;
    usage: string;
    serverOnly: boolean;
    types: {slash: boolean; prefix: boolean; messageContext: boolean; userContext: boolean};
    rateLimit: {duration: number; unit: string};
    registrations: CommandRegistration[];
}

export interface CommandCatalog {
    schemaVersion: number;
    commands: CommandMetadata[];
}

export interface CommandSettings {
    disabledCommands: string[];
}

export const COMMAND_TYPES = {slash: "Slash", prefix: "Prefix", messageContext: "Message context", userContext: "User context"};
export const OPTION_TYPES: Record<number, string> = {
    1: "Subcommand", 2: "Subcommand group", 3: "Text", 4: "Integer", 5: "Boolean", 6: "User",
    7: "Channel", 8: "Role", 9: "Mentionable", 10: "Number", 11: "Attachment"
};

export function commandTypes(command: CommandMetadata) {
    return (Object.keys(COMMAND_TYPES) as Array<keyof typeof COMMAND_TYPES>)
        .filter(type => command.types[type]).map(type => COMMAND_TYPES[type]);
}

export function commandCooldown(command: CommandMetadata) {
    const {duration, unit} = command.rateLimit;
    if (duration <= 0) return "None";
    const label = unit.toLowerCase().replace(/_/g, " ");
    return `${duration} ${duration === 1 ? label.replace(/s$/, "") : label}`;
}

export function commandScopes(command: CommandMetadata) {
    return command.serverOnly ? "Server only" : "Server and direct messages";
}

export function registrationContexts(registration: CommandRegistration) {
    const labels: Record<string, string> = {"0": "Servers", "1": "Bot direct messages", "2": "Private channels"};
    return registration.contexts?.map(context => labels[String(context)] ?? `Context ${context}`).join(", ") ?? "Not specified";
}

export function registrationInstalls(registration: CommandRegistration) {
    const labels: Record<string, string> = {"0": "Server install", "1": "User install"};
    return registration.integration_types?.map(type => labels[String(type)] ?? `Install type ${type}`).join(", ") ?? "Not specified";
}

function optionSearchText(options: CommandOption[]): string {
    return options.map(option => [option.name, option.description, OPTION_TYPES[option.type],
        ...option.choices?.map(choice => `${choice.name} ${choice.value}`) ?? [], optionSearchText(option.options ?? [])].join(" ")).join(" ");
}

export function commandMatchesQuery(command: CommandMetadata, query: string) {
    const text = [command.name, command.richName, command.description, command.category, command.access, command.usage,
        commandScopes(command), commandCooldown(command), ...commandTypes(command),
        ...command.registrations.map(registration => `${registration.name} ${optionSearchText(registration.options ?? [])}`)].join(" ").toLowerCase();
    return query.trim().toLowerCase().split(/\s+/).every(term => text.includes(term));
}
