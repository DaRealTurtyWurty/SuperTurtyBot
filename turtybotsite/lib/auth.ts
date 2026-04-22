import {randomUUID} from "node:crypto";
import {cookies} from "next/headers";
import {redirect} from "next/navigation";
import {NextResponse} from "next/server";
import {
    createDashboardSession,
    deleteDashboardSession,
    getDashboardSession,
    isDashboardApiError,
    type DashboardSessionGuildSummary,
    type DashboardSessionUserSummary
} from "@/lib/dashboard-api";
import {isDashboardOfflineError} from "@/lib/dashboard-offline";

const SESSION_COOKIE_NAME = "turtybot_session";
const OAUTH_STATE_COOKIE_NAME = "turtybot_oauth_state";
const DISCORD_ACCESS_TOKEN_COOKIE_NAME = "turtybot_discord_access_token";
const DISCORD_REFRESH_TOKEN_COOKIE_NAME = "turtybot_discord_refresh_token";
const DISCORD_TOKEN_EXPIRES_AT_COOKIE_NAME = "turtybot_discord_token_expires_at";
const SESSION_DURATION_MS = 1000 * 60 * 60 * 24 * 7;
const OAUTH_STATE_DURATION_MS = 1000 * 60 * 10;

function isSecureCookie() {
    return process.env.NODE_ENV === "production";
}

function createCookieExpiry(durationMs: number) {
    return new Date(Date.now() + durationMs);
}

export async function getCurrentSession() {
    const cookieStore = await cookies();
    const sessionId = cookieStore.get(SESSION_COOKIE_NAME)?.value;
    if (!sessionId)
        return null;

    try {
        return await getDashboardSession(sessionId);
    } catch (error) {
        if (isDashboardApiError(error) && error.status === 404) {
            return null;
        }

        throw error;
    }
}

export async function requireCurrentSession() {
    let session;
    try {
        session = await getCurrentSession();
    } catch (error) {
        if (isDashboardOfflineError(error)) {
            redirect("/bot-offline");
        }

        throw error;
    }

    if (!session) {
        redirect("/api/auth/discord/login");
    }

    return session;
}

export async function createAuthenticatedSession(user: DashboardSessionUserSummary, guilds: DashboardSessionGuildSummary[]) {
    const createdAtMs = Date.now();
    const expiresAt = createCookieExpiry(SESSION_DURATION_MS);

    const session = await createDashboardSession({
        sessionId: randomUUID(),
        user,
        guilds,
        createdAtMs,
        expiresAtMs: expiresAt.getTime()
    });

    return {
        sessionId: session.sessionId,
        expiresAt: new Date(session.expiresAtMs)
    };
}

export function setSessionCookie(response: NextResponse, sessionId: string, expiresAt: Date) {
    response.cookies.set({
        name: SESSION_COOKIE_NAME,
        value: sessionId,
        httpOnly: true,
        sameSite: "lax",
        secure: isSecureCookie(),
        expires: expiresAt,
        path: "/"
    });
}

export function setOAuthStateCookie(response: NextResponse, state: string) {
    response.cookies.set({
        name: OAUTH_STATE_COOKIE_NAME,
        value: state,
        httpOnly: true,
        sameSite: "lax",
        secure: isSecureCookie(),
        expires: createCookieExpiry(OAUTH_STATE_DURATION_MS),
        path: "/"
    });
}

export function clearOAuthStateCookie(response: NextResponse) {
    response.cookies.set({
        name: OAUTH_STATE_COOKIE_NAME,
        value: "",
        httpOnly: true,
        sameSite: "lax",
        secure: isSecureCookie(),
        expires: new Date(0),
        path: "/"
    });
}

export function clearSessionCookie(response: NextResponse) {
    response.cookies.set({
        name: SESSION_COOKIE_NAME,
        value: "",
        httpOnly: true,
        sameSite: "lax",
        secure: isSecureCookie(),
        expires: new Date(0),
        path: "/"
    });
}

export function setDiscordTokenCookies(
    response: NextResponse,
    accessToken: string,
    refreshToken: string,
    expiresAtMs: number
) {
    const options = {
        httpOnly: true,
        sameSite: "lax" as const,
        secure: isSecureCookie(),
        expires: createCookieExpiry(SESSION_DURATION_MS),
        path: "/"
    };

    response.cookies.set({
        name: DISCORD_ACCESS_TOKEN_COOKIE_NAME,
        value: accessToken,
        ...options
    });
    response.cookies.set({
        name: DISCORD_REFRESH_TOKEN_COOKIE_NAME,
        value: refreshToken,
        ...options
    });
    response.cookies.set({
        name: DISCORD_TOKEN_EXPIRES_AT_COOKIE_NAME,
        value: expiresAtMs.toString(),
        ...options
    });
}

export function clearDiscordTokenCookies(response: NextResponse) {
    for (const name of [
        DISCORD_ACCESS_TOKEN_COOKIE_NAME,
        DISCORD_REFRESH_TOKEN_COOKIE_NAME,
        DISCORD_TOKEN_EXPIRES_AT_COOKIE_NAME
    ]) {
        response.cookies.set({
            name,
            value: "",
            httpOnly: true,
            sameSite: "lax",
            secure: isSecureCookie(),
            expires: new Date(0),
            path: "/"
        });
    }
}

export async function destroyCurrentSession(response: NextResponse) {
    const cookieStore = await cookies();
    const sessionId = cookieStore.get(SESSION_COOKIE_NAME)?.value;
    if (sessionId) {
        try {
            await deleteDashboardSession(sessionId);
        } catch (error) {
            if (!isDashboardApiError(error) || error.status !== 404) {
                throw error;
            }
        }
    }

    clearSessionCookie(response);
    clearDiscordTokenCookies(response);
}
