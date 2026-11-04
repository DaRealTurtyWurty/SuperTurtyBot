"use client";

import {useRemarkSync} from "react-remark";

export default function PrivacyPolicyContent({source}: {source: string}) {
    const content = useRemarkSync(source);

    return <article className="break-words text-base leading-8 text-slate-300 [&_h1]:mb-6 [&_h1]:text-4xl [&_h1]:font-bold [&_h1]:leading-tight [&_h1]:tracking-tight [&_h1]:text-white md:[&_h1]:text-5xl [&_h2]:mb-4 [&_h2]:mt-10 [&_h2]:text-2xl [&_h2]:font-semibold [&_h2]:leading-snug [&_h2]:text-white [&_p]:my-4 [&_ul]:my-5 [&_ul]:list-disc [&_ul]:space-y-3 [&_ul]:pl-6 [&_strong]:font-semibold [&_strong]:text-slate-100 [&_a]:text-sky-300 [&_a]:underline [&_a]:decoration-sky-400/40 [&_a]:underline-offset-4 [&_a:hover]:text-sky-200 [&_a:focus-visible]:outline [&_a:focus-visible]:outline-2 [&_a:focus-visible]:outline-sky-300 [&_blockquote]:my-6 [&_blockquote]:border-l-2 [&_blockquote]:border-amber-400 [&_blockquote]:bg-amber-400/10 [&_blockquote]:px-5 [&_blockquote]:py-1 [&_blockquote]:text-amber-100 [&_code]:text-sm [&_code]:text-sky-200">
        {content}
    </article>;
}
