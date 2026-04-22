import {NextRequest, NextResponse} from "next/server";
import {clearOAuthStateCookie, createAuthenticatedSession, setDiscordTokenCookies, setSessionCookie} from "@/lib/auth";
import {createPublicUrl, exchangeCodeForAccessToken, fetchDiscordUser, fetchManageableDiscordGuilds, getPublicOrigin} from "@/lib/discord";

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

    try {
        const publicOrigin = getPublicOrigin(request.nextUrl.origin);
        const token = await exchangeCodeForAccessToken(code, publicOrigin);
        const [user, guilds] = await Promise.all([
            fetchDiscordUser(token.accessToken),
            fetchManageableDiscordGuilds(token.accessToken)
        ]);

        const session = await createAuthenticatedSession(user, guilds);
        const response = NextResponse.redirect(createPublicUrl("/dashboard", publicOrigin));
        clearOAuthStateCookie(response);
        setSessionCookie(response, session.sessionId, session.expiresAt);
        setDiscordTokenCookies(response, token.accessToken, token.refreshToken, token.expiresAtMs);
        return response;
    } catch {
        const response = redirectWithError(request, "oauth_callback_failed");
        clearOAuthStateCookie(response);
        return response;
    }
}
