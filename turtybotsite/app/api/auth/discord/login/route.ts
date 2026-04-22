import {randomUUID} from "node:crypto";
import {NextRequest, NextResponse} from "next/server";
import {setOAuthStateCookie} from "@/lib/auth";
import {buildDiscordAuthorizationUrl, getPublicOrigin} from "@/lib/discord";

export async function GET(request: NextRequest) {
    const state = randomUUID();
    const authorizationUrl = buildDiscordAuthorizationUrl(state, getPublicOrigin(request.nextUrl.origin));
    const response = NextResponse.redirect(authorizationUrl);
    setOAuthStateCookie(response, state);
    return response;
}
