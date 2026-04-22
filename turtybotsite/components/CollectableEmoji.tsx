"use client";

import Image from "next/image";

const CUSTOM_EMOJI_PATTERN = /^<(a?):([^:>]+):(\d+)>$/;
const FLAG_ALIAS_PATTERN = /^:?flag_([a-z]{2}):?$/i;

function toRegionalIndicatorFlag(code: string) {
    const upper = code.toUpperCase();
    if (!/^[A-Z]{2}$/.test(upper)) {
        return null;
    }

    return Array.from(upper, character =>
        String.fromCodePoint(0x1F1E6 + character.charCodeAt(0) - 65)
    ).join("");
}

export default function CollectableEmoji({emoji, label}: { emoji: string; label: string }) {
    const customEmoji = emoji.match(CUSTOM_EMOJI_PATTERN);
    if (customEmoji) {
        const animated = customEmoji[1] === "a";
        const extension = animated ? "gif" : "webp";

        return <span className="inline-flex h-6 w-6 items-center justify-center align-middle">
            <Image
                src={`https://cdn.discordapp.com/emojis/${customEmoji[3]}.${extension}?quality=lossless`}
                alt={label}
                width={24}
                height={24}
                className="h-6 w-6 object-contain"
            />
        </span>;
    }

    const flagAlias = emoji.match(FLAG_ALIAS_PATTERN);
    if (flagAlias?.[1]) {
        const flag = toRegionalIndicatorFlag(flagAlias[1]);
        if (flag) {
            const codepoint = Array.from(flag)
                .map(character => character.codePointAt(0)?.toString(16))
                .filter((value): value is string => Boolean(value))
                .join("-");

            return <span className="inline-flex h-6 w-6 items-center justify-center align-middle">
                <Image
                    src={`https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/svg/${codepoint}.svg`}
                    alt={label}
                    title={label}
                    width={24}
                    height={24}
                    className="h-6 w-6 object-contain"
                />
            </span>;
        }
    }

    return <span aria-label={label} title={label} className="inline-flex h-6 min-w-6 items-center justify-center text-base leading-none">
        {emoji}
    </span>;
}
