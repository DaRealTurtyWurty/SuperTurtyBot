import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import test from "node:test";
import vm from "node:vm";
import ts from "typescript";
import React from "react";
import * as jsxRuntime from "react/jsx-runtime";
import {renderToStaticMarkup} from "react-dom/server";

const source = await readFile(new URL("../components/ServerAuditPanel.tsx", import.meta.url), "utf8");
const code = ts.transpileModule(source, {compilerOptions: {module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX}}).outputText;
const context = vm.createContext({});
const imports = {
    "react": {useState: React.useState},
    "react/jsx-runtime": jsxRuntime,
    "next/link": {default: props => React.createElement("a", props)},
    "react-icons/fa6": {FaArrowRotateRight: () => null, FaClipboardCheck: () => null}
};
const module = new vm.SourceTextModule(code, {context});
await module.link(async specifier => new vm.SyntheticModule(Object.keys(imports[specifier]), function () {
    for (const [name, value] of Object.entries(imports[specifier])) this.setExport(name, value);
}, {context}));
await module.evaluate();

function render(healthChecks) {
    const report = {guildId: "123", generatedAtMs: 1, findings: [], notes: [], healthChecks};
    return renderToStaticMarkup(React.createElement(module.namespace.default, {guildId: "123", initialReport: report}));
}

test("completed health checks show their evidence even with no findings", () => {
    const html = render([{name: "Discord gateway", status: "passed", detail: "The bot is connected to Discord."}]);
    assert.match(html, /All checks passed/);
    assert.match(html, /Discord gateway/);
    assert.match(html, /The bot is connected to Discord/);
    assert.match(html, />Passed</);
});

test("skipped public checks are distinguished from successful checks", () => {
    const html = render([
        {name: "Local dashboard/API", status: "passed", detail: "HTTP 200"},
        {name: "Public dashboard/API", status: "skipped", detail: "No public API URL is configured."}
    ]);
    assert.match(html, /Some checks skipped/);
    assert.match(html, /No public API URL is configured/);
    assert.match(html, />Skipped</);
    assert.doesNotMatch(html, /All checks passed/);
});

test("a failed check cannot produce an all-passed summary", () => {
    const html = render([{name: "Local dashboard/API", status: "failed", detail: "Timed out"}]);
    assert.match(html, /Needs attention/);
    assert.match(html, />Failed</);
    assert.match(html, /Timed out/);
    assert.doesNotMatch(html, /All checks passed/);
});

test("older snapshots do not imply health was verified", () => {
    for (const checks of [undefined, null, []]) {
        const html = render(checks);
        assert.match(html, /Check details unavailable/);
        assert.match(html, /Run the audit again with the updated bot/);
        assert.doesNotMatch(html, /All checks passed/);
    }
});
