"use client";

import React, {isValidElement, useEffect, useMemo, useState, useTransition} from "react";
import type {ComponentPropsWithoutRef, ReactNode} from "react";
import Image from "next/image";
import {Remark} from "react-remark";
import {Prism as SyntaxHighlighter} from "react-syntax-highlighter";
import {vscDarkPlus} from "react-syntax-highlighter/dist/esm/styles/prism";
import remarkGfm from "remark-gfm";
import visit from "unist-util-visit";
import mdastToHastAll from "mdast-util-to-hast/lib/all";
import type {
    DashboardModmailLinkPreview,
    DashboardModmailTicketDetail,
    DashboardModmailTicketSummary,
    DashboardModmailTicketsResponse
} from "@/lib/dashboard-api";

interface ModmailTicketsBrowserProps {
    guildId: string;
    initialTickets: DashboardModmailTicketsResponse;
}

type TicketFilter = "all" | "open" | "closed";

interface MentionLookups {
    channels: Record<string, string>;
    roles: Record<string, string>;
    members: Record<string, string>;
}

interface DiscordMentionMatch {
    kind: "user" | "role" | "channel";
    index: number;
    id: string;
    raw: string;
}

interface DiscordMdastNode {
    type: string;
    value?: string;
    data?: {
        kind?: "user" | "role" | "channel";
        raw?: string;
    };
    children?: DiscordMdastNode[];
}

interface DiscordMdastParent extends DiscordMdastNode {
    children: DiscordMdastNode[];
}

interface DiscordHastNode {
    type?: string;
    tagName?: string;
    value?: string;
    children?: DiscordHastNode[];
}

type HtmlProps<Tag extends keyof React.JSX.IntrinsicElements> = ComponentPropsWithoutRef<Tag>;

function formatTimestamp(value: number) {
    if (!value) {
        return "Unknown";
    }

    return new Date(value).toLocaleString();
}

function TicketStatusBadge({open}: {open: boolean}) {
    return <span className={`border px-2 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] ${
        open
            ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-200"
            : "border-slate-700 bg-slate-800 text-slate-300"
    }`}>
        {open ? "Open" : "Closed"}
    </span>;
}

function formatTicketMeta(...parts: Array<string | null | undefined>) {
    return parts
        .map(part => part?.trim())
        .filter((part): part is string => Boolean(part))
        .join(" · ");
}

function parseAttachmentUrl(value: string) {
    const match = value.match(/^(\S+)\s+\(.+\)$/);
    return match ? match[1] : value;
}

function isRenderableMediaUrl(value: string) {
    try {
        const url = new URL(value);
        return /\.(png|jpe?g|gif|webp|bmp|avif)$/i.test(url.pathname);
    } catch {
        return false;
    }
}

function extractUrls(text: string) {
    const matches = text.match(/https?:\/\/[^\s<>()[\]{}"'`]+/gi) ?? [];
    const urls: string[] = [];

    for (const match of matches) {
        const normalized = normalizeUrl(match);
        if (normalized && !urls.includes(normalized)) {
            urls.push(normalized);
        }
    }

    return urls;
}

function normalizeUrl(value: string) {
    return value.trim().replace(/[.,!?:;)\]}]+$/g, "");
}

function escapeRegExp(value: string) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function toPlainText(children: ReactNode): string {
    if (typeof children === "string" || typeof children === "number") {
        return String(children);
    }

    if (Array.isArray(children)) {
        return children.map(child => toPlainText(child)).join("");
    }

    if (isValidElement<{children?: ReactNode}>(children)) {
        return toPlainText(children.props.children);
    }

    return "";
}

function normalizeDiscordMarkdown(source: string) {
    let normalized = source.replace(/\r\n/g, "\n");

    normalized = normalized.replace(/(^|\n)-#\s+/g, "$1###### ");
    normalized = normalized.replace(/([^\n])```(?=\s*(?:\n|$))/g, "$1\n```");
    normalized = normalized.replace(/(^|[^\S\n])__([^_\n]+?)__(?=$|[^\S\n.,!?;:])/g, "$1*$2*");

    for (const marker of ["**", "||"] as const) {
        const escaped = escapeRegExp(marker);
        normalized = normalized.replace(new RegExp(`(${escaped})([^\\n]*?)\\s+${escaped}`, "g"), `$1$2$1`);
    }

    normalized = normalized.replace(/~~/g, "\\~\\~");

    return normalized;
}

function isBareUrlContent(content: string) {
    const trimmed = content.trim();
    const urls = extractUrls(trimmed);
    return urls.length === 1 && trimmed === urls[0];
}

function getPreviewUrl(preview: DashboardModmailLinkPreview) {
    return preview.url || preview.imageUrl || "";
}

function collectMentionIds(source: string) {
    const ids = new Set<string>();
    const regex = /<@!?(\d+)>/g;
    for (const match of source.matchAll(regex)) {
        ids.add(match[1]);
    }

    return [...ids];
}

function TranscriptPreviewCard({preview}: {preview: DashboardModmailLinkPreview}) {
    const previewUrl = getPreviewUrl(preview);
    const isImage = preview.type === "image" || (preview.imageUrl && isRenderableMediaUrl(preview.imageUrl));
    const label = preview.siteName?.trim() || (() => {
        try {
            return new URL(previewUrl).hostname.replace(/^www\./, "");
        } catch {
            return "Link";
        }
    })();

    if (isImage && preview.imageUrl) {
        return <a
            href={previewUrl}
            target="_blank"
            rel="noreferrer"
            className="group block overflow-hidden border border-slate-700 bg-slate-950/70 transition hover:border-sky-400/40 hover:bg-slate-900/80"
        >
            <Image
                src={preview.imageUrl}
                alt={preview.title || label}
                width={800}
                height={420}
                className="max-h-[420px] w-full object-contain"
                loading="lazy"
            />
            <div className="space-y-1 p-3">
                <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {label}
                </p>
                {preview.title ? <p className="text-sm font-semibold text-white">{preview.title}</p> : null}
                {preview.description ? <p className="text-sm text-slate-300">{preview.description}</p> : null}
                <p className="truncate text-xs text-sky-300 group-hover:text-sky-200">{previewUrl}</p>
            </div>
        </a>;
    }

    return <a
        href={previewUrl}
        target="_blank"
        rel="noreferrer"
        className="group block overflow-hidden border border-slate-700 bg-slate-950/70 transition hover:border-sky-400/40 hover:bg-slate-900/80"
    >
        <div className="grid gap-3 p-3 md:grid-cols-[minmax(0,1fr)_160px]">
            <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {label}
                </p>
                {preview.title ? <p className="mt-1 text-sm font-semibold text-white">{preview.title}</p> : null}
                {preview.description ? <p className="mt-1 text-sm text-slate-300">{preview.description}</p> : null}
                <p className="mt-2 truncate text-xs text-sky-300 group-hover:text-sky-200">{previewUrl}</p>
            </div>
            {preview.imageUrl ? <Image
                src={preview.imageUrl}
                alt={preview.title || label}
                width={160}
                height={112}
                className="h-28 w-full rounded-md border border-slate-700 object-cover md:h-full"
                loading="lazy"
            /> : null}
        </div>
    </a>;
}

function rehypeDiscordSyntax(lookups: MentionLookups) {
    return (tree: DiscordHastNode) => {
        transformDiscordNodes(tree, lookups);
    };
}

function transformDiscordNodes(node: DiscordHastNode, lookups: MentionLookups) {
    if (!node || !Array.isArray(node.children)) {
        return;
    }

    if (node.tagName === "code" || node.tagName === "pre") {
        return;
    }

    const nextChildren: DiscordHastNode[] = [];

    for (const child of node.children) {
        if (child?.type === "text" && typeof child.value === "string") {
            nextChildren.push(...splitDiscordText(child.value, lookups));
            continue;
        }

        transformDiscordNodes(child, lookups);
        nextChildren.push(child);
    }

    node.children = nextChildren;
}

function remarkDiscordSyntax(lookups: MentionLookups) {
    return (tree: DiscordMdastParent) => {
        visit(tree, "text", (node: DiscordMdastNode, index: number | undefined, parent: DiscordMdastParent | undefined) => {
            if (index == null || !parent || typeof node.value !== "string") {
                return;
            }

            const value = node.value;
            if (!value.includes("||") && !value.includes("<@")) {
                return;
            }

            const children: DiscordMdastNode[] = [];
            let cursor = 0;

            while (cursor < value.length) {
                const mentionMatch = findNextMention(value, cursor);
                const strikeIndex = value.indexOf("~~", cursor);
                const spoilerIndex = value.indexOf("||", cursor);

                const nextStrikeIndex = strikeIndex === -1 ? Number.POSITIVE_INFINITY : strikeIndex;
                const nextSpoilerIndex = spoilerIndex === -1 ? Number.POSITIVE_INFINITY : spoilerIndex;
                const nextMentionIndex = mentionMatch ? mentionMatch.index : Number.POSITIVE_INFINITY;
                const nextIndex = Math.min(nextStrikeIndex, nextSpoilerIndex, nextMentionIndex);

                if (!Number.isFinite(nextIndex)) {
                    const rest = value.slice(cursor);
                    if (rest) {
                        children.push({type: "text", value: rest});
                    }
                    break;
                }

                if (nextIndex > cursor) {
                    children.push({type: "text", value: value.slice(cursor, nextIndex)});
                    cursor = nextIndex;
                    continue;
                }

                if (nextIndex === nextMentionIndex && mentionMatch) {
                    const label = buildMentionLabel(mentionMatch.kind, mentionMatch.id, lookups);
                    children.push({
                        type: "discordMention",
                        data: {
                            kind: mentionMatch.kind,
                            id: mentionMatch.id,
                            raw: mentionMatch.raw,
                            label,
                        },
                        children: [{type: "text", value: label}],
                    });
                    cursor = mentionMatch.index + mentionMatch.raw.length;
                    continue;
                }

                const closingIndex = value.indexOf("||", cursor + 2);
                if (closingIndex === -1) {
                    children.push({type: "text", value: value.slice(cursor)});
                    break;
                }

                const inner = value.slice(cursor + 2, closingIndex);
                children.push({
                    type: "discordSpoiler",
                    children: [{type: "text", value: inner}],
                });
                cursor = closingIndex + 2;
            }

            parent.children.splice(index, 1, ...children);
            return index + children.length;
        });
    };
}

function buildMentionLabel(kind: "user" | "role" | "channel", id: string, lookups: MentionLookups) {
    const resolved = kind === "channel"
        ? lookups.channels[id] ?? null
        : kind === "role"
            ? lookups.roles[id] ?? null
            : lookups.members[id] ?? null;

    return resolved ?? (kind === "channel" ? `#${id}` : `@${id}`);
}

function splitDiscordText(value: string, lookups: MentionLookups) {
    const nodes: DiscordHastNode[] = [];
    let index = 0;

    while (index < value.length) {
        const spoilerIndex = value.indexOf("||", index);
        const mentionMatch = findNextMention(value, index);

        const nextSpoilerIndex = spoilerIndex === -1 ? Number.POSITIVE_INFINITY : spoilerIndex;
        const nextMentionIndex = mentionMatch ? mentionMatch.index : Number.POSITIVE_INFINITY;
        const nextIndex = Math.min(nextSpoilerIndex, nextMentionIndex);

        if (!Number.isFinite(nextIndex)) {
            const rest = value.slice(index);
            if (rest) {
                nodes.push({type: "text", value: rest});
            }
            break;
        }

        if (nextIndex > index) {
            nodes.push({type: "text", value: value.slice(index, nextIndex)});
            index = nextIndex;
            continue;
        }

        if (nextIndex === nextMentionIndex && mentionMatch) {
            nodes.push(buildMentionNode(mentionMatch.kind, mentionMatch.id, mentionMatch.raw, lookups));
            index = mentionMatch.index + mentionMatch.raw.length;
            continue;
        }

        const closingIndex = value.indexOf("||", index + 2);
        if (closingIndex === -1) {
            nodes.push({type: "text", value: value.slice(index)});
            break;
        }

        const inner = value.slice(index + 2, closingIndex);
        nodes.push({
            type: "element",
            tagName: "span",
            properties: {
                className: ["discord-spoiler"],
                title: "Spoiler",
            },
            children: [{type: "text", value: inner}],
        });
        index = closingIndex + 2;
    }

    return nodes;
}

function findNextMention(value: string, startIndex: number) {
    const mentionPatterns = [
        {kind: "user" as const, pattern: /<@!?(\d+)>/g},
        {kind: "role" as const, pattern: /<@&(\d+)>/g},
        {kind: "channel" as const, pattern: /<#(\d+)>/g},
    ];

    let best: DiscordMentionMatch | null = null;

    for (const {kind, pattern} of mentionPatterns) {
        pattern.lastIndex = startIndex;
        const match = pattern.exec(value);
        if (!match) {
            continue;
        }

        const raw = match[0];
        const index = match.index;
        const id = match[1];
        if (best == null || index < best.index) {
            best = {kind, index, id, raw};
        }
    }

    return best;
}

function buildMentionNode(kind: "user" | "role" | "channel", id: string, raw: string, lookups: MentionLookups) {
    const baseClasses = [
        "inline-flex",
        "items-center",
        "rounded",
        "px-1.5",
        "py-0.5",
        "text-[0.92em]",
        "font-medium",
    ];

    const variantClasses = kind === "channel"
        ? ["bg-sky-400/10", "text-sky-100"]
        : kind === "role"
            ? ["bg-violet-400/10", "text-violet-100"]
            : ["bg-emerald-400/10", "text-emerald-100"];

    const resolvedLabel = kind === "channel"
        ? lookups.channels[id] ?? null
        : kind === "role"
            ? lookups.roles[id] ?? null
            : lookups.members[id] ?? null;

    return {
        type: "element",
        tagName: "span",
        properties: {
            className: [...baseClasses, ...variantClasses],
            title: raw,
        },
        children: [{
            type: "text",
            value: resolvedLabel ?? (kind === "channel" ? `#${id}` : kind === "role" ? `@${id}` : `@${id}`),
        }],
    };
}

function TranscriptMarkdown({source, lookups}: {source: string; lookups: MentionLookups}) {
    return <Remark
        remarkPlugins={[remarkGfm, () => remarkDiscordSyntax(lookups)]}
        rehypePlugins={[rehypeDiscordSyntax(lookups)]}
        remarkToRehypeOptions={{
            handlers: {
                discordSpoiler: (h: unknown, node: DiscordMdastNode) => (h as typeof mdastToHastAll extends (...args: infer _A) => infer _R ? (node: DiscordMdastNode, tagName: string, properties: object, children: unknown) => _R : never)(node, "span", {
                    className: ["discord-spoiler"],
                    title: "Spoiler",
                }, mdastToHastAll(h as never, node as never)),
                discordMention: (h: unknown, node: DiscordMdastNode) => (h as typeof mdastToHastAll extends (...args: infer _A) => infer _R ? (node: DiscordMdastNode, tagName: string, properties: object, children: unknown) => _R : never)(node, "span", {
                    className: [
                        "inline-flex",
                        "items-center",
                        "rounded",
                        "px-1.5",
                        "py-0.5",
                        "text-[0.92em]",
                        "font-medium",
                        node.data?.kind === "channel"
                            ? "bg-sky-400/10 text-sky-100"
                            : node.data?.kind === "role"
                                ? "bg-violet-400/10 text-violet-100"
                                : "bg-emerald-400/10 text-emerald-100",
                    ],
                    title: node.data?.raw,
                }, mdastToHastAll(h as never, node as never)),
            },
        }}
        rehypeReactOptions={{
            components: {
                p: (props: HtmlProps<"p">) => <p className="whitespace-pre-wrap text-sm text-slate-200" {...props} />,
                a: (props: HtmlProps<"a">) => <a className="text-sky-300 underline decoration-sky-400/30 underline-offset-2 hover:text-sky-200" target="_blank" rel="noreferrer" {...props} />,
                strong: (props: HtmlProps<"strong">) => <strong className="font-semibold text-white" {...props} />,
                em: (props: HtmlProps<"em">) => <em className="italic text-slate-100" {...props} />,
                del: (props: HtmlProps<"del">) => <del {...props} />,
                s: (props: HtmlProps<"s">) => <s {...props} />,
                blockquote: (props: HtmlProps<"blockquote">) => <blockquote className="border-l-4 border-slate-700 bg-slate-900/60 px-4 py-3 text-sm text-slate-300" {...props} />,
                pre: (props: HtmlProps<"pre">) => <div className="my-3">{props.children}</div>,
                code: ({className, children, ...props}: HtmlProps<"code">) => {
                    const isBlockCode = typeof className === "string" && className.includes("language-");
                    if (isBlockCode) {
                        const language = className.match(/language-([a-z0-9_-]+)/i)?.[1] || "text";
                        const code = toPlainText(children);

                        return <div className="overflow-hidden border border-slate-700 bg-slate-950/80">
                            <div className="border-b border-slate-700 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">
                                {language}
                            </div>
                            <SyntaxHighlighter
                                language={language}
                                style={vscDarkPlus}
                                PreTag="div"
                                customStyle={{
                                    margin: 0,
                                    background: "transparent",
                                    padding: "1rem",
                                    fontSize: "0.875rem",
                                    lineHeight: "1.6",
                                }}
                                codeTagProps={{
                                    style: {
                                        fontFamily: "var(--font-ibm-plex-mono), monospace",
                                    },
                                }}
                                wrapLongLines
                            >
                                {code}
                            </SyntaxHighlighter>
                        </div>;
                    }

                    return <code className="rounded bg-slate-800 px-1 py-0.5 font-mono text-[0.95em] text-sky-100" {...props}>{children}</code>;
                },
                ul: (props: HtmlProps<"ul">) => <ul className="list-disc space-y-1 pl-5 text-sm text-slate-200" {...props} />,
                ol: (props: HtmlProps<"ol">) => <ol className="list-decimal space-y-1 pl-5 text-sm text-slate-200" {...props} />,
                li: (props: HtmlProps<"li">) => <li className="leading-6" {...props} />,
                h1: (props: HtmlProps<"h1">) => <h1 className="text-2xl font-bold text-white" {...props} />,
                h2: (props: HtmlProps<"h2">) => <h2 className="text-xl font-bold text-white" {...props} />,
                h3: (props: HtmlProps<"h3">) => <h3 className="text-lg font-semibold text-white" {...props} />,
                h4: (props: HtmlProps<"h4">) => <h4 className="text-base font-semibold text-white" {...props} />,
                h5: (props: HtmlProps<"h5">) => <h5 className="text-sm font-semibold text-white" {...props} />,
                h6: (props: HtmlProps<"h6">) => <h6 className="text-xs font-medium tracking-[0.12em] text-slate-400" {...props} />,
                hr: (props: HtmlProps<"hr">) => <hr className="border-slate-800" {...props} />,
                table: (props: HtmlProps<"table">) => <div className="overflow-x-auto"><table className="min-w-full border-collapse text-sm text-slate-200" {...props} /></div>,
                thead: (props: HtmlProps<"thead">) => <thead className="bg-slate-900 text-slate-300" {...props} />,
                tbody: (props: HtmlProps<"tbody">) => <tbody className="divide-y divide-slate-800" {...props} />,
                tr: (props: HtmlProps<"tr">) => <tr className="border-b border-slate-800" {...props} />,
                th: (props: HtmlProps<"th">) => <th className="border border-slate-800 px-3 py-2 text-left font-semibold" {...props} />,
                td: (props: HtmlProps<"td">) => <td className="border border-slate-800 px-3 py-2 align-top" {...props} />,
                span: (props: HtmlProps<"span">) => {
                    const className = Array.isArray(props.className)
                        ? props.className.join(" ")
                        : typeof props.className === "string"
                            ? props.className
                            : "";

                    if (className.includes("discord-spoiler")) {
                        return <DiscordSpoiler {...props} />;
                    }

                    return <span {...props} />;
                },
            }
        }}
    >
        {normalizeDiscordMarkdown(source)}
    </Remark>;
}

function TranscriptEntryCard({
    entry,
    lookups,
    hideContent,
    attachmentUrls
}: {
    entry: DashboardModmailTicketDetail["transcript"][number];
    lookups: MentionLookups;
    hideContent: boolean;
    attachmentUrls: string[];
}) {
    const [showRaw, setShowRaw] = useState(false);

    return <article className="border border-slate-800 bg-slate-900/70 p-4">
        <div className="flex flex-wrap items-center gap-3">
            {entry.authorAvatarUrl ? <Image
                src={entry.authorAvatarUrl}
                alt={entry.authorTag || "Unknown User"}
                width={40}
                height={40}
                className="h-10 w-10 shrink-0 rounded-full border border-slate-700 object-cover"
            /> : <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-slate-700 bg-slate-800 text-xs font-semibold uppercase text-slate-300">
                {(entry.authorTag || "U").slice(0, 2)}
            </div>}
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-semibold text-white">{entry.authorTag || "Unknown User"}</p>
                    {entry.bot ? <span className="border border-sky-400/30 bg-sky-400/10 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.14em] text-sky-100">Bot</span> : null}
                </div>
                <span className="text-xs text-slate-500">{formatTimestamp(entry.createdAt)}</span>
            </div>
            <button
                type="button"
                onClick={() => setShowRaw(current => !current)}
                className="border border-slate-700 bg-slate-950/70 px-2 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-300 transition hover:border-slate-500 hover:text-white"
            >
                {showRaw ? "Rendered" : "Raw"}
            </button>
        </div>

        {entry.content && !hideContent ? <div className="mt-3 space-y-3">
            {showRaw ? <pre className="overflow-x-auto border border-slate-800 bg-slate-950/80 p-4 text-sm whitespace-pre-wrap text-slate-200">
                {entry.content}
            </pre> : <TranscriptMarkdown source={entry.content} lookups={lookups} />}
        </div> : null}

        {entry.previews.length > 0 ? <div className="mt-3 space-y-3">
            {entry.previews.map(preview => <TranscriptPreviewCard key={`${entry.messageId}:${preview.url}`} preview={preview} />)}
        </div> : null}

        {attachmentUrls.length > 0 ? <div className="mt-3 space-y-1">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Attachments</p>
            {attachmentUrls.map(url => <a key={url} href={url} className="block break-all text-sm text-sky-300 hover:text-sky-200" target="_blank" rel="noreferrer">{url}</a>)}
        </div> : null}
    </article>;
}

function DiscordSpoiler({children}: {children?: ReactNode}) {
    const [revealed, setRevealed] = useState(false);

    return <button
        type="button"
        onClick={() => setRevealed(current => !current)}
        className={`rounded px-1 text-left transition ${
            revealed
                ? "bg-slate-800 text-slate-100"
                : "bg-slate-700 text-slate-700 hover:text-slate-200"
        }`}
        title="Spoiler"
    >
        {revealed ? children : "Spoiler"}
    </button>;
}

export default function ModmailTicketsBrowser({guildId, initialTickets}: ModmailTicketsBrowserProps) {
    const [filter, setFilter] = useState<TicketFilter>("all");
    const [tickets, setTickets] = useState<DashboardModmailTicketSummary[]>(initialTickets.tickets);
    const [selectedTicketNumber, setSelectedTicketNumber] = useState<number | null>(initialTickets.tickets[0]?.ticketNumber ?? null);
    const [detail, setDetail] = useState<DashboardModmailTicketDetail | null>(null);
    const [mentionLookups, setMentionLookups] = useState<MentionLookups>({channels: {}, roles: {}, members: {}});
    const [isLoadingDetail, setIsLoadingDetail] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [listError, setListError] = useState<string | null>(null);
    const [isPending, startTransition] = useTransition();

    const selectedTicket = useMemo(
        () => tickets.find(ticket => ticket.ticketNumber === selectedTicketNumber) ?? null,
        [selectedTicketNumber, tickets]
    );

    useEffect(() => {
        let isCancelled = false;

        setListError(null);

        fetch(`/api/dashboard/guilds/${guildId}/modmail/tickets?status=${filter}`, {
            cache: "no-store"
        })
            .then(async response => {
                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to load modmail tickets.");
                }

                return response.json() as Promise<DashboardModmailTicketsResponse>;
            })
            .then(payload => {
                if (isCancelled) {
                    return;
                }

                setTickets(payload.tickets);
                setSelectedTicketNumber(current => {
                    if (current && payload.tickets.some(ticket => ticket.ticketNumber === current)) {
                        return current;
                    }

                    return payload.tickets[0]?.ticketNumber ?? null;
                });
            })
            .catch(fetchError => {
                if (!isCancelled) {
                    setListError(fetchError instanceof Error ? fetchError.message : "Failed to load modmail tickets.");
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [guildId, filter]);

    useEffect(() => {
        if (selectedTicketNumber == null) {
            setDetail(null);
            return;
        }

        let isCancelled = false;
        setIsLoadingDetail(true);
        setError(null);

        fetch(`/api/dashboard/guilds/${guildId}/modmail/tickets/${selectedTicketNumber}`, {
            cache: "no-store"
        })
            .then(async response => {
                if (!response.ok) {
                    const payload = await response.json().catch(() => null) as {message?: string} | null;
                    throw new Error(payload?.message ?? "Failed to load modmail ticket.");
                }

                return response.json() as Promise<DashboardModmailTicketDetail>;
            })
            .then(payload => {
                if (!isCancelled) {
                    setDetail(payload);
                }
            })
            .catch(fetchError => {
                if (!isCancelled) {
                    setError(fetchError instanceof Error ? fetchError.message : "Failed to load modmail ticket.");
                }
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsLoadingDetail(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [guildId, selectedTicketNumber]);

    useEffect(() => {
        if (!detail) {
            setMentionLookups({channels: {}, roles: {}, members: {}});
            return;
        }

        let isCancelled = false;
        const memberIds = new Set<string>();
        const transcriptSources = [detail.openerMessage, ...detail.transcript.map(entry => entry.content)];

        for (const source of transcriptSources) {
            for (const id of collectMentionIds(source)) {
                memberIds.add(id);
            }
        }

        async function loadLookups() {
            const [channelsPayload, rolesPayload, memberPayloads] = await Promise.all([
                fetch(`/api/discord/guilds/${guildId}/channels`, {cache: "no-store"}).then(async response => {
                    if (!response.ok) {
                        return {channels: [] as {id: string; name: string}[]};
                    }

                    return response.json() as Promise<{channels: {id: string; name: string}[]}>;
                }),
                fetch(`/api/discord/guilds/${guildId}/roles`, {cache: "no-store"}).then(async response => {
                    if (!response.ok) {
                        return {roles: [] as {id: string; name: string}[]};
                    }

                    return response.json() as Promise<{roles: {id: string; name: string}[]}>;
                }),
                Promise.all([...memberIds].map(async memberId => {
                    const response = await fetch(`/api/discord/guilds/${guildId}/members?query=${encodeURIComponent(memberId)}`, {
                        cache: "no-store"
                    });

                    if (!response.ok) {
                        return [] as {id: string; displayName: string}[];
                    }

                    const payload = await response.json() as {members: {id: string; displayName: string}[]};
                    return payload.members.filter(member => member.id === memberId);
                }))
            ]);

            if (isCancelled) {
                return;
            }

            setMentionLookups({
                channels: Object.fromEntries(channelsPayload.channels.map(channel => [channel.id, `#${channel.name}`])),
                roles: Object.fromEntries(rolesPayload.roles.map(role => [role.id, `@${role.name}`])),
                members: Object.fromEntries(memberPayloads.flat().map(member => [member.id, `@${member.displayName}`])),
            });
        }

        loadLookups().catch(() => {
            if (!isCancelled) {
                setMentionLookups({channels: {}, roles: {}, members: {}});
            }
        });

        return () => {
            isCancelled = true;
        };
    }, [detail, guildId]);

    function selectFilter(nextFilter: TicketFilter) {
        startTransition(() => {
            setFilter(nextFilter);
        });
    }

    return <div className="grid gap-5 xl:grid-cols-[minmax(0,360px)_minmax(0,1fr)]">
        <div className="border border-slate-800/80 bg-slate-950/60 p-4">
            <div className="flex flex-wrap gap-2">
                {(["all", "open", "closed"] as TicketFilter[]).map(option => (
                    <button
                        key={option}
                        type="button"
                        onClick={() => selectFilter(option)}
                        className={`border px-3 py-2 text-xs font-semibold uppercase tracking-[0.14em] transition ${
                            filter === option
                                ? "border-sky-400 bg-sky-400/12 text-sky-100"
                                : "border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-600 hover:text-white"
                        }`}
                    >
                        {option}
                    </button>
                ))}
            </div>

            <div className="mt-4 space-y-2">
                {listError ? <p className="border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-100">
                    {listError}
                </p> : null}

                {tickets.length === 0 ? <p className="px-2 py-4 text-sm text-slate-500">
                    No tickets found for this filter.
                </p> : tickets.map(ticket => (
                    <button
                        key={ticket.ticketNumber}
                        type="button"
                        onClick={() => setSelectedTicketNumber(ticket.ticketNumber)}
                        className={`w-full border px-4 py-3 text-left transition ${
                            selectedTicketNumber === ticket.ticketNumber
                                ? "border-sky-400 bg-sky-400/12"
                                : "border-slate-800 bg-slate-950/60 hover:border-slate-700 hover:bg-slate-900/80"
                        }`}
                    >
                        <div className="flex items-start gap-3">
                            {ticket.userAvatarUrl ? <Image
                                src={ticket.userAvatarUrl}
                                alt={ticket.userDisplayName}
                                width={40}
                                height={40}
                                className="h-10 w-10 shrink-0 rounded-full border border-slate-700 object-cover"
                            /> : <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-slate-700 bg-slate-800 text-xs font-semibold uppercase text-slate-300">
                                {ticket.userDisplayName.slice(0, 2)}
                            </div>}
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-semibold text-white">
                                    #{ticket.ticketNumber} {ticket.userDisplayName}
                                </p>
                        <p className="mt-1 truncate text-xs text-slate-400">
                                    {formatTicketMeta(ticket.channelName, ticket.categoryName)}
                        </p>
                                <p className="mt-1 truncate text-xs text-slate-500">
                                    Opened {formatTimestamp(ticket.openedAt)}
                                </p>
                            </div>
                            <TicketStatusBadge open={ticket.open} />
                        </div>
                    </button>
                ))}
            </div>
        </div>

        <div className="border border-slate-800/80 bg-slate-950/60 p-5">
            {!selectedTicket ? <p className="text-sm text-slate-400">Select a ticket to view its details.</p> : null}

            {selectedTicket ? <div className="space-y-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <h3 className="text-2xl font-bold text-white">Ticket #{selectedTicket.ticketNumber}</h3>
                        <p className="mt-1 text-sm text-slate-400">
                            {formatTicketMeta(selectedTicket.userDisplayName, selectedTicket.channelName, selectedTicket.categoryName)}
                        </p>
                    </div>
                    <TicketStatusBadge open={selectedTicket.open} />
                </div>

                <div className="grid gap-3 md:grid-cols-2">
                    <InfoCard label="Opened" value={formatTimestamp(selectedTicket.openedAt)} />
                    <InfoCard label="Closed" value={formatTimestamp(selectedTicket.closedAt)} />
                    <InfoCard label="Source" value={selectedTicket.source} />
                    <InfoCard label="Messages" value={String(selectedTicket.transcriptMessageCount)} />
                </div>

                {selectedTicket.closeReason ? <section className="border border-slate-800 bg-slate-900/70 p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Close Reason</p>
                    <p className="mt-2 text-sm text-slate-200">{selectedTicket.closeReason}</p>
                </section> : null}

                {detail ? <section className="space-y-3">
                    <div className="border border-slate-800 bg-slate-900/70 p-4">
                        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Opener Message</p>
                        <p className="mt-2 whitespace-pre-wrap text-sm text-slate-200">
                            {detail.openerMessage || "No opener message recorded."}
                        </p>
                    </div>

                    <div className="space-y-3">
                        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">Transcript</p>
                        {isLoadingDetail || isPending ? <p className="text-sm text-slate-400">Loading transcript...</p> : null}
                        {error ? <p className="border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-100">
                            {error}
                        </p> : null}
                        {!isLoadingDetail && !error && detail.transcript.length === 0 ? <p className="text-sm text-slate-400">No transcript was archived for this ticket.</p> : null}
                        <div className="space-y-3">
                            {detail.transcript.map(entry => {
                                const previewUrls = new Set(entry.previews.map(preview => getPreviewUrl(preview)));
                                const bareContentUrl = entry.content ? extractUrls(entry.content.trim())[0] ?? null : null;
                                const shouldHideContent = entry.content ? isBareUrlContent(entry.content) && previewUrls.has(bareContentUrl ?? "") : false;
                                const attachmentUrls = entry.attachments
                                    .map(parseAttachmentUrl)
                                    .filter(url => !previewUrls.has(url));

                                return <TranscriptEntryCard
                                    key={entry.messageId}
                                    entry={entry}
                                    lookups={mentionLookups}
                                    hideContent={shouldHideContent}
                                    attachmentUrls={attachmentUrls}
                                />;
                            })}
                        </div>
                    </div>
                </section> : null}
            </div> : null}
        </div>
    </div>;
}

function InfoCard({label, value}: {label: string; value: string}) {
    return <div className="border border-slate-800 bg-slate-900/70 p-4">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">{label}</p>
        <p className="mt-2 text-sm text-slate-200">{value}</p>
    </div>;
}
