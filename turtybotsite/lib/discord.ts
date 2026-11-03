const DISCORD_API_BASE = "https://discord.com/api/v10";
const ADMINISTRATOR_PERMISSION = BigInt(0x8);
const MANAGE_GUILD_PERMISSION = BigInt(0x20);

interface DiscordTokenResponse {
    access_token: string;
    refresh_token?: string;
    expires_in?: number;
    token_type: string;
}

interface DiscordUserResponse {
    id: string;
    username: string;
    global_name: string | null;
    avatar: string | null;
}

interface DiscordGuildResponse {
    id: string;
    name: string;
    icon: string | null;
    owner: boolean;
    permissions: string;
}

export class DiscordApiError extends Error {
    constructor(readonly operation: string, readonly status: number, readonly code: string | null) {
        super(`Discord ${operation} failed (HTTP ${status}).`);
        this.name = "DiscordApiError";
    }
}

export class DiscordConfigurationError extends Error {
    constructor(readonly variable: string) {
        super(`Missing required environment variable: ${variable}`);
        this.name = "DiscordConfigurationError";
    }
}

async function discordFailure(response: Response, operation: string) {
    const payload = await response.json().catch(() => null) as {error?: unknown; code?: unknown} | null;
    // Keep known error identifiers only. Never log response bodies, descriptions, or token values.
    const oauthCodes = new Set(["invalid_client", "invalid_grant", "invalid_request", "invalid_scope", "unsupported_grant_type", "access_denied", "server_error", "temporarily_unavailable"]);
    const code = typeof payload?.error === "string" && oauthCodes.has(payload.error) ? payload.error :
        typeof payload?.code === "number" && Number.isSafeInteger(payload.code) ? String(payload.code) : null;
    return new DiscordApiError(operation, response.status, code);
}

function getRequiredEnv(name: string) {
    const value = process.env[name]?.trim();
    if (!value) {
        throw new DiscordConfigurationError(name);
    }

    return value;
}

function getRedirectUri(origin: string) {
    return process.env.DISCORD_REDIRECT_URI?.trim() || `${origin}/api/auth/discord/callback`;
}

export function getPublicOrigin(origin: string) {
    const redirectUri = process.env.DISCORD_REDIRECT_URI?.trim();
    if (redirectUri) {
        try {
            return new URL(redirectUri).origin;
        } catch {
            // Fall back to the request origin if DISCORD_REDIRECT_URI is malformed.
        }
    }

    return origin;
}

export function createPublicUrl(pathname: string, origin: string) {
    return new URL(pathname, getPublicOrigin(origin));
}

export function buildDiscordAuthorizationUrl(state: string, origin: string) {
    const params = new URLSearchParams({
        client_id: getRequiredEnv("DISCORD_CLIENT_ID"),
        response_type: "code",
        redirect_uri: getRedirectUri(origin),
        scope: "identify guilds",
        state
    });

    return `https://discord.com/oauth2/authorize?${params.toString()}`;
}

export async function exchangeCodeForAccessToken(code: string, origin: string) {
    const response = await fetch(`${DISCORD_API_BASE}/oauth2/token`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
            client_id: getRequiredEnv("DISCORD_CLIENT_ID"),
            client_secret: getRequiredEnv("DISCORD_CLIENT_SECRET"),
            grant_type: "authorization_code",
            code,
            redirect_uri: getRedirectUri(origin)
        })
    });

    if (!response.ok) {
        throw await discordFailure(response, "token_exchange");
    }

    const payload = await response.json() as DiscordTokenResponse;
    return {
        accessToken: payload.access_token,
        refreshToken: payload.refresh_token ?? "",
        expiresAtMs: Date.now() + Math.max(0, (payload.expires_in ?? 0) * 1000)
    };
}

async function fetchDiscordResource<T>(path: string, accessToken: string) {
    const response = await fetch(`${DISCORD_API_BASE}${path}`, {
        headers: {
            Authorization: `Bearer ${accessToken}`
        },
        cache: "no-store"
    });

    if (!response.ok) {
        throw await discordFailure(response, path === "/users/@me" ? "user_profile" : "guild_list");
    }

    return response.json() as Promise<T>;
}

export async function fetchDiscordUser(accessToken: string) {
    const user = await fetchDiscordResource<DiscordUserResponse>("/users/@me", accessToken);
    return {
        id: user.id,
        username: user.username,
        globalName: user.global_name,
        avatar: user.avatar
    };
}

export async function fetchManageableDiscordGuilds(accessToken: string) {
    const guilds = await fetchDiscordResource<DiscordGuildResponse[]>("/users/@me/guilds", accessToken);
    return guilds
        .filter(guild => isManageableGuild(guild.owner, guild.permissions))
        .map(guild => ({
            id: guild.id,
            name: guild.name,
            icon: guild.icon,
            owner: guild.owner,
            permissions: guild.permissions
        }))
        .sort((left, right) => left.name.localeCompare(right.name));
}

export function isManageableGuild(owner: boolean, permissions: string) {
    if (owner)
        return true;

    try {
        const parsedPermissions = BigInt(permissions);
        return (parsedPermissions & ADMINISTRATOR_PERMISSION) !== BigInt(0)
            || (parsedPermissions & MANAGE_GUILD_PERMISSION) !== BigInt(0);
    } catch {
        return false;
    }
}

export function getDiscordAvatarUrl(userId: string, avatar: string | null, size = 128) {
    if (!avatar)
        return null;

    return `https://cdn.discordapp.com/avatars/${userId}/${avatar}.png?size=${size}`;
}

export function getDiscordGuildIconUrl(guildId: string, icon: string | null, size = 128) {
    if (!icon)
        return null;

    return `https://cdn.discordapp.com/icons/${guildId}/${icon}.png?size=${size}`;
}
