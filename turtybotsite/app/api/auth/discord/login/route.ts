import {randomUUID} from "node:crypto";
import {NextRequest, NextResponse} from "next/server";
import {setOAuthStateCookie} from "@/lib/auth";
import {buildDiscordAuthorizationUrl} from "@/lib/discord";

export async function GET(request: NextRequest) {
    const state = randomUUID();
    const authorizationUrl = buildDiscordAuthorizationUrl(state, request.nextUrl.origin);
    const response = NextResponse.redirect(authorizationUrl);
    setOAuthStateCookie(response, state);
    return response;
}
