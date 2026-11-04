import type {Metadata} from "next";
import {readFile} from "node:fs/promises";
import path from "node:path";
import Link from "next/link";
import PolicyContent from "@/components/PolicyContent";

export const metadata: Metadata = {
    title: "Privacy Policy | TurtyBot",
    description: "How TurtyBot processes your information and how to request access, correction, or deletion."
};

export const dynamic = "force-static";

export default async function PrivacyPage() {
    const markdown = await readFile(path.join(process.cwd(), "content", "privacy.md"), "utf8");
    const source = markdown.replace(/<!--[\s\S]*?-->/g, "").trim();

    return <main className="min-h-screen bg-slate-950 px-6 py-10 text-slate-100 md:py-16">
        <div className="mx-auto max-w-3xl">
            <nav aria-label="Privacy page navigation" className="mb-10 flex flex-wrap items-center justify-between gap-4 border-b border-slate-800 pb-6 text-sm">
                <Link href="/" className="font-semibold text-slate-200 transition hover:text-sky-300">
                    Back to TurtyBot
                </Link>
                <a href="mailto:turtybot@gmail.com" className="text-sky-300 underline underline-offset-4 hover:text-sky-200">
                    Contact us about privacy
                </a>
            </nav>
            <PolicyContent source={source} />
        </div>
    </main>;
}
