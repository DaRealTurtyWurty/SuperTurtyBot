import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import test from "node:test";
import vm from "node:vm";
import ts from "typescript";

function compile(source) {
    return ts.transpileModule(source, {compilerOptions: {module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022}}).outputText;
}
const callbackCode = compile(await readFile(new URL("../app/api/auth/discord/callback/route.ts", import.meta.url), "utf8"));
const discordCode = compile(await readFile(new URL("../lib/discord.ts", import.meta.url), "utf8"));

class DiscordApiError extends Error {
    constructor(status, code) {
        super("sensitive upstream content must not be logged");
        this.operation = "token_exchange";
        this.status = status;
        this.code = code;
    }
}
class DiscordConfigurationError extends Error {
    variable = "DISCORD_CLIENT_SECRET";
}

async function callback(failure) {
    const logs = [];
    const cookies = [];
    const token = {accessToken: "private-access-token", refreshToken: "private-refresh-token", expiresAtMs: 100};
    const failAt = stage => {if (failure?.stage === stage) throw failure.error;};
    const context = vm.createContext({console: {error: (...args) => logs.push(args)}});
    const values = {
        "next/server": {NextResponse: {redirect: url => ({url: String(url)})}},
        "@/lib/auth": {
            clearOAuthStateCookie: () => cookies.push("clear-state"),
            createAuthenticatedSession: async () => {failAt("session"); return {sessionId: "session", expiresAt: new Date()};},
            setDiscordTokenCookies: () => cookies.push("discord-tokens"),
            setSessionCookie: () => cookies.push("session")
        },
        "@/lib/dashboard-api": {isDashboardApiError: error => error?.dashboard === true},
        "@/lib/discord": {
            DiscordApiError, DiscordConfigurationError,
            createPublicUrl: pathname => new URL(pathname, "http://localhost:3003"),
            getPublicOrigin: () => "http://localhost:3003",
            exchangeCodeForAccessToken: async () => {failAt("token"); return token;},
            fetchDiscordUser: async () => {failAt("profile"); return {id: "user"};},
            fetchManageableDiscordGuilds: async () => []
        }
    };
    const module = new vm.SourceTextModule(callbackCode, {context});
    await module.link(async specifier => new vm.SyntheticModule(Object.keys(values[specifier]), function () {
        for (const [key, value] of Object.entries(values[specifier])) this.setExport(key, value);
    }, {context}));
    await module.evaluate();
    const request = {
        nextUrl: new URL("http://localhost:3003/api/auth/discord/callback?state=expected&code=private-code"),
        cookies: {get: () => ({value: failure?.stage === "state" ? "wrong" : "expected"})}
    };
    const response = await module.namespace.GET(request);
    return {url: new URL(response.url), logs, cookies};
}

test("successful login redirects to dashboard and sets cookies", async () => {
    const result = await callback();
    assert.equal(result.url.pathname, "/dashboard");
    assert.deepEqual(result.cookies, ["clear-state", "session", "discord-tokens"]);
    assert.deepEqual(result.logs, []);
});

test("invalid state stops sign-in before token exchange", async () => {
    const result = await callback({stage: "state"});
    assert.equal(result.url.searchParams.get("error"), "invalid_oauth_state");
    assert.deepEqual(result.cookies, ["clear-state"]);
    assert.deepEqual(result.logs, []);
});

test("Discord failures identify the failing stage without logging credentials or raw errors", async () => {
    for (const [stage, expected] of [["token", "oauth_token_exchange_failed"], ["profile", "oauth_profile_failed"]]) {
        const result = await callback({stage, error: new DiscordApiError(400, "invalid_grant")});
        assert.equal(result.url.searchParams.get("error"), expected);
        assert.equal(result.logs[0][1].status, 400);
        assert.equal(result.logs[0][1].code, "invalid_grant");
        assert.doesNotMatch(JSON.stringify(result.logs), /private-|sensitive|expected/);
        assert.deepEqual(result.cookies, ["clear-state"]);
    }
});

test("session-save failures identify the bot API rather than blaming Discord credentials", async () => {
    const result = await callback({stage: "session", error: {dashboard: true, status: 503, code: "dashboard_offline", message: "private-value"}});
    assert.equal(result.url.searchParams.get("error"), "oauth_session_failed");
    assert.equal(result.logs[0][1].stage, "dashboard_session");
    assert.equal(result.logs[0][1].service, "dashboard");
    assert.doesNotMatch(JSON.stringify(result.logs), /private-value/);
});

test("missing configuration identifies only the variable name", async () => {
    const result = await callback({stage: "token", error: new DiscordConfigurationError()});
    assert.equal(result.url.searchParams.get("error"), "oauth_configuration_failed");
    assert.equal(result.logs[0][1].variable, "DISCORD_CLIENT_SECRET");
});

test("Discord error bodies retain only allowlisted OAuth identifiers", async () => {
    for (const code of ["invalid_client", "private-secret-body"]) {
        const context = vm.createContext({URL, URLSearchParams, Set, Error, Number,
            process: {env: {DISCORD_CLIENT_ID: "id", DISCORD_CLIENT_SECRET: "secret", DISCORD_REDIRECT_URI: " http://localhost:3003/api/auth/discord/callback "}},
            fetch: async () => new Response(JSON.stringify({error: code, error_description: "private-secret-body"}), {status: 401})});
        const module = new vm.SourceTextModule(discordCode, {context});
        await module.link(() => {throw Error("Unexpected import");});
        await module.evaluate();
        await assert.rejects(module.namespace.exchangeCodeForAccessToken("code", "http://localhost:3003"), error => {
            assert.equal(error.status, 401);
            assert.equal(error.code, code === "invalid_client" ? code : null);
            assert.doesNotMatch(error.message, /private-secret-body/);
            return true;
        });
    }
});

test("invalid client credentials produce a specific configuration error", async () => {
    const result = await callback({stage: "token", error: new DiscordApiError(401, "invalid_client")});
    assert.equal(result.url.searchParams.get("error"), "oauth_invalid_client");
    assert.equal(result.logs[0][1].code, "invalid_client");
    assert.deepEqual(result.cookies, ["clear-state"]);
});
