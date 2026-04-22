import "server-only";

import {redirect} from "next/navigation";
import {isDashboardApiError} from "@/lib/dashboard-api";

const NETWORK_ERROR_CODES = new Set([
    "ECONNREFUSED",
    "ECONNRESET",
    "EHOSTUNREACH",
    "ENETUNREACH",
    "ENOTFOUND",
    "ETIMEDOUT",
    "UND_ERR_CONNECT_TIMEOUT",
    "UND_ERR_SOCKET"
]);

function readErrorCode(error: unknown): string | null {
    if (!error || typeof error !== "object") {
        return null;
    }

    const code = "code" in error ? error.code : null;
    return typeof code === "string" ? code : null;
}

function readErrorCause(error: unknown): unknown {
    if (!error || typeof error !== "object" || !("cause" in error)) {
        return null;
    }

    return error.cause;
}

function isNetworkFetchError(error: unknown): boolean {
    const code = readErrorCode(error);
    if (code && NETWORK_ERROR_CODES.has(code)) {
        return true;
    }

    const cause = readErrorCause(error);
    if (cause && cause !== error && isNetworkFetchError(cause)) {
        return true;
    }

    if (!(error instanceof Error)) {
        return false;
    }

    return error.message.toLowerCase().includes("fetch failed");
}

export function isDashboardOfflineError(error: unknown): boolean {
    if (isDashboardApiError(error)) {
        return error.status >= 500;
    }

    return isNetworkFetchError(error);
}

export function handleDashboardPageError(error: unknown) {
    if (isDashboardOfflineError(error)) {
        redirect("/bot-offline");
    }

    if (isDashboardApiError(error)) {
        return null;
    }

    throw error;
}
