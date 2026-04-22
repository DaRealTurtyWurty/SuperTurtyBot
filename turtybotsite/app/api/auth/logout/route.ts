import {NextRequest, NextResponse} from "next/server";
import {clearOAuthStateCookie, destroyCurrentSession} from "@/lib/auth";
import {createPublicUrl} from "@/lib/discord";

export async function GET(request: NextRequest) {
    const response = NextResponse.redirect(createPublicUrl("/", request.nextUrl.origin));
    clearOAuthStateCookie(response);
    await destroyCurrentSession(response);
    return response;
}
