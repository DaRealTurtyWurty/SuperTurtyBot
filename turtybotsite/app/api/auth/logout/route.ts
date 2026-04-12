import {NextRequest, NextResponse} from "next/server";
import {clearOAuthStateCookie, destroyCurrentSession} from "@/lib/auth";

export async function GET(request: NextRequest) {
    const response = NextResponse.redirect(new URL("/", request.url));
    clearOAuthStateCookie(response);
    await destroyCurrentSession(response);
    return response;
}
