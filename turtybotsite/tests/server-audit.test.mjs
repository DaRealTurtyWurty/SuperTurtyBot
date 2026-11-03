import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import test from "node:test";
import vm from "node:vm";
import ts from "typescript";

const source = await readFile(new URL("../app/api/dashboard/guilds/[guildId]/audit/route.ts", import.meta.url), "utf8");
const compiled = ts.transpileModule(source, {compilerOptions: {module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022}}).outputText;

async function setup(session, apiError = null) {
    const calls = [];
    const context = vm.createContext({Error});
    const exports = {
        "next/server": {NextResponse: {json: (body, options) => ({body, status: options?.status ?? 200})}},
        "@/lib/auth": {getCurrentSession: async () => session},
        "@/lib/discord": {getPublicOrigin: () => "https://bot.example.com"},
        "@/lib/dashboard-api": {
            fetchDashboardAudit: async (...args) => {
                calls.push(args);
                if (apiError) throw apiError;
                return {report: {guildId: args[0], findings: []}};
            },
            isDashboardApiError: error => error?.api === true
        }
    };
    const route = new vm.SourceTextModule(compiled, {context});
    await route.link(async specifier => {
        const values = exports[specifier];
        assert.ok(values, `Unexpected import: ${specifier}`);
        return new vm.SyntheticModule(Object.keys(values), function () {
            for (const [name, value] of Object.entries(values)) this.setExport(name, value);
        }, {context});
    });
    await route.evaluate();
    return {route: route.namespace, calls};
}

const session = {user: {id: "manager"}, guilds: [{id: "123"}]};
const params = {params: Promise.resolve({guildId: "123"})};
function request(origin = "https://bot.example.com") {
    return {headers: new Headers({origin}), nextUrl: new URL("http://internal:3003/api/dashboard/guilds/123/audit?userId=attacker")};
}

test("signed-out users cannot read reports", async () => {
    const {route, calls} = await setup(null);
    assert.equal((await route.GET(request(), params)).status, 401);
    assert.equal(calls.length, 0);
});

test("a session cannot read another server's report", async () => {
    const {route, calls} = await setup({...session, guilds: [{id: "456"}]});
    assert.equal((await route.GET(request(), params)).status, 403);
    assert.equal(calls.length, 0);
});

test("the user ID is taken from the session, not query parameters", async () => {
    const {route, calls} = await setup(session);
    assert.equal((await route.GET(request(), params)).status, 200);
    assert.deepEqual(calls, [["123", "manager", "GET"]]);
});

test("cross-origin audit reruns are rejected", async () => {
    const {route, calls} = await setup(session);
    assert.equal((await route.POST(request("https://other.example.com"), params)).status, 403);
    assert.equal(calls.length, 0);
});

test("same-origin reruns work behind the configured public reverse proxy", async () => {
    const {route, calls} = await setup(session);
    assert.equal((await route.POST(request(), params)).status, 200);
    assert.deepEqual(calls, [["123", "manager", "POST"]]);
});

test("revoked Discord permissions and rate limits remain visible to the user", async () => {
    for (const status of [403, 409, 429]) {
        const {route} = await setup(session, {api: true, status, message: "Try again"});
        const response = await route.POST(request(), params);
        assert.equal(response.status, status);
        assert.equal(response.body.message, "Try again");
    }
});
