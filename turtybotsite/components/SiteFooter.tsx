import Link from "next/link";

export default function SiteFooter() {
    return <footer className="relative border-t border-slate-800 bg-slate-950 px-6 py-6 text-sm text-slate-400">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4">
            <span>TurtyBot</span>
            <nav aria-label="Footer navigation" className="flex flex-wrap gap-x-6 gap-y-3">
                <Link href="/terms" className="underline underline-offset-4 transition hover:text-sky-300">
                    Terms of service
                </Link>
                <Link href="/privacy" className="underline underline-offset-4 transition hover:text-sky-300">
                    Privacy policy
                </Link>
                <a href="mailto:turtybot@gmail.com" className="transition hover:text-sky-300">
                    Contact
                </a>
            </nav>
        </div>
    </footer>;
}
