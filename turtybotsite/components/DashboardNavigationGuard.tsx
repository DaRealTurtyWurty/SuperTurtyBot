"use client";

import type {ReactNode} from "react";
import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useId,
    useMemo,
    useRef,
    useState
} from "react";
import {usePathname, useSearchParams} from "next/navigation";

const UNSAVED_CHANGES_MESSAGE = "You have unsaved changes. Leave this page without saving?";

interface DashboardNavigationGuardContextValue {
    confirmNavigation: () => boolean;
    hasUnsavedChanges: boolean;
    setDirty: (id: string, isDirty: boolean) => void;
}

const DashboardNavigationGuardContext = createContext<DashboardNavigationGuardContextValue | null>(null);

export function DashboardNavigationGuard({children}: {children: ReactNode}) {
    const pathname = usePathname();
    const searchParams = useSearchParams();
    const [dirtySources, setDirtySources] = useState<Record<string, boolean>>({});
    const allowNextNavigationRef = useRef(false);

    const hasUnsavedChanges = useMemo(
        () => Object.values(dirtySources).some(Boolean),
        [dirtySources]
    );
    const currentUrl = useMemo(() => {
        const query = searchParams.toString();
        return `${pathname}${query ? `?${query}` : ""}`;
    }, [pathname, searchParams]);

    const setDirty = useCallback((id: string, isDirty: boolean) => {
        setDirtySources(current => {
            if (isDirty) {
                if (current[id]) {
                    return current;
                }

                return {
                    ...current,
                    [id]: true
                };
            }

            if (!current[id]) {
                return current;
            }

            const next = {...current};
            delete next[id];
            return next;
        });
    }, []);

    const confirmNavigation = useCallback(() => {
        if (!hasUnsavedChanges || typeof window === "undefined") {
            return true;
        }

        const confirmed = window.confirm(UNSAVED_CHANGES_MESSAGE);
        if (confirmed) {
            allowNextNavigationRef.current = true;
        }

        return confirmed;
    }, [hasUnsavedChanges]);

    useEffect(() => {
        allowNextNavigationRef.current = false;
    }, [currentUrl]);

    useEffect(() => {
        function onBeforeUnload(event: BeforeUnloadEvent) {
            if (!hasUnsavedChanges || allowNextNavigationRef.current) {
                return;
            }

            event.preventDefault();
            event.returnValue = "";
        }

        function onDocumentClick(event: MouseEvent) {
            if (!hasUnsavedChanges || allowNextNavigationRef.current || event.defaultPrevented) {
                return;
            }

            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                return;
            }

            const target = event.target;
            if (!(target instanceof Element)) {
                return;
            }

            const anchor = target.closest("a[href]");
            if (!(anchor instanceof HTMLAnchorElement)) {
                return;
            }

            if (anchor.target && anchor.target !== "_self") {
                return;
            }

            if (anchor.hasAttribute("download")) {
                return;
            }

            const destination = new URL(anchor.href, window.location.href);
            const current = new URL(window.location.href);
            if (destination.origin !== current.origin) {
                return;
            }

            if (destination.pathname === current.pathname && destination.search === current.search) {
                return;
            }

            if (!confirmNavigation()) {
                event.preventDefault();
            }
        }

        function onPopState() {
            if (!hasUnsavedChanges || allowNextNavigationRef.current) {
                return;
            }

            if (!window.confirm(UNSAVED_CHANGES_MESSAGE)) {
                window.history.go(1);
                return;
            }

            allowNextNavigationRef.current = true;
        }

        window.addEventListener("beforeunload", onBeforeUnload);
        document.addEventListener("click", onDocumentClick, true);
        window.addEventListener("popstate", onPopState);

        return () => {
            window.removeEventListener("beforeunload", onBeforeUnload);
            document.removeEventListener("click", onDocumentClick, true);
            window.removeEventListener("popstate", onPopState);
        };
    }, [confirmNavigation, hasUnsavedChanges]);

    const contextValue = useMemo(() => ({
        confirmNavigation,
        hasUnsavedChanges,
        setDirty
    }), [confirmNavigation, hasUnsavedChanges, setDirty]);

    return <DashboardNavigationGuardContext.Provider value={contextValue}>
        {children}
    </DashboardNavigationGuardContext.Provider>;
}

export function useDashboardNavigationGuard() {
    const context = useContext(DashboardNavigationGuardContext);
    if (!context) {
        throw new Error("useDashboardNavigationGuard must be used within DashboardNavigationGuard.");
    }

    return context;
}

export function useDashboardUnsavedChanges<T>(value: T) {
    const id = useId();
    const {setDirty} = useDashboardNavigationGuard();
    const currentSnapshot = useMemo(() => JSON.stringify(value), [value]);
    const [savedSnapshot, setSavedSnapshot] = useState(currentSnapshot);

    useEffect(() => {
        const isDirty = currentSnapshot !== savedSnapshot;
        setDirty(id, isDirty);

        return () => {
            setDirty(id, false);
        };
    }, [currentSnapshot, id, savedSnapshot, setDirty]);

    const markSaved = useCallback((nextValue?: T) => {
        setSavedSnapshot(nextValue === undefined ? currentSnapshot : JSON.stringify(nextValue));
    }, [currentSnapshot]);

    return {
        isDirty: currentSnapshot !== savedSnapshot,
        markSaved
    };
}
