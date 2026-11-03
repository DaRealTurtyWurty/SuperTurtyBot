import {NextRequest, NextResponse} from "next/server";
import {clearOAuthStateCookie, createAuthenticatedSession, setDiscordTokenCookies, setSessionCookie} from "@/lib/auth";
import {isDashboardApiError} from "@/lib/dashboard-api";
import {DiscordApiError, DiscordConfigurationError, createPublicUrl, exchangeCodeForAccessToken, fetchDiscordUser, fetchManageableDiscordGuilds, getPublicOrigin} from "@/lib/discord";

function redirectWithError(request: NextRequest, code: string) {
    const url = createPublicUrl("/", request.nextUrl.origin);
    url.searchParams.set("error", code);
    return NextResponse.redirect(url);
}

export async function GET(request: NextRequest) {
    const state = request.nextUrl.searchParams.get("state");
    const code = request.nextUrl.searchParams.get("code");
    const error = request.nextUrl.searchParams.get("error");
    const expectedState = request.cookies.get("turtybot_oauth_state")?.value;

    if (error) {
        const response = redirectWithError(request, error);
        clearOAuthStateCookie(response);
        return response;
    }

    if (!state || !code || !expectedState || state !== expectedState) {
        const response = redirectWithError(request, "invalid_oauth_state");
        clearOAuthStateCookie(response);
        return response;
    }

    let stage = "token_exchange";
    try {
        const publicOrigin = getPublicOrigin(request.nextUrl.origin);
        const token = await exchangeCodeForAccessToken(code, publicOrigin);
        stage = "discord_profile";
        const [user, guilds] = await Promise.all([
            fetchDiscordUser(token.accessToken),
            fetchManageableDiscordGuilds(token.accessToken)
        ]);

        stage = "dashboard_session";
        const session = await createAuthenticatedSession(user, guilds);
        const response = NextResponse.redirect(createPublicUrl("/dashboard", publicOrigin));
        stage = "session_cookies";
        clearOAuthStateCookie(response);
        setSessionCookie(response, session.sessionId, session.expiresAt);
        setDiscordTokenCookies(response, token.accessToken, token.refreshToken, token.expiresAtMs);
        return response;
    } catch (error) {
        const details = error instanceof DiscordApiError ? {
            service: "discord", operation: error.operation, status: error.status, code: error.code
        } : error instanceof DiscordConfigurationError ? {
            service: "configuration", variable: error.variable
        } : isDashboardApiError(error) ? {
            service: "dashboard", status: error.status, code: error.code
        } : {service: "unknown"};
        console.error("Discord OAuth callback failed", {stage, ...details});
        const reason = error instanceof DiscordApiError && error.code === "invalid_client" ? "oauth_invalid_client" :
            error instanceof DiscordConfigurationError ? "oauth_configuration_failed" :
            stage === "token_exchange" ? "oauth_token_exchange_failed" :
                stage === "discord_profile" ? "oauth_profile_failed" :
                    stage === "dashboard_session" ? "oauth_session_failed" : "oauth_callback_failed";
        const response = redirectWithError(request, reason);
        clearOAuthStateCookie(response);
        return response;
    }
}
