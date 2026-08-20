"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, ChevronRight } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import type { MessageKey } from "@/lib/i18n";
import { useSidebar } from "@/lib/sidebar-context";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { LocaleToggle } from "@/components/locale-toggle";

const breadcrumbKeys: Record<string, MessageKey> = {
  "/": "nav.dashboard",
  "/investments": "nav.investments",
  "/map": "nav.map",
  "/signals": "nav.signals",
  "/correlations": "nav.correlations",
  "/developers": "nav.developers",
  "/coverage": "nav.coverage",
  "/sources": "nav.sources",
  "/history": "nav.history",
  "/settings": "nav.settings",
};

export function AppHeader() {
  const pathname = usePathname();
  const { t } = useI18n();
  const { toggleSidebar, toggleMobileSidebar } = useSidebar();

  const isInvestmentDetail = pathname.startsWith("/investments/") && pathname !== "/investments";
  const baseKey = isInvestmentDetail ? "/investments" : pathname;
  const baseLabel = t(breadcrumbKeys[baseKey] ?? "nav.dashboard");

  function handleToggle() {
    if (window.innerWidth >= 1024) {
      toggleSidebar();
    } else {
      toggleMobileSidebar();
    }
  }

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur-md">
      <Button variant="ghost" size="icon" onClick={handleToggle} aria-label="Toggle sidebar">
        <Menu className="size-5" />
      </Button>

      <nav className="flex items-center gap-1.5 text-sm text-muted-foreground">
        <Link href="/" className="hover:text-foreground">
          {t("nav.dashboard")}
        </Link>
        {pathname !== "/" ? (
          <>
            <ChevronRight className="size-3.5" />
            <Link href={baseKey} className="hover:text-foreground">
              {baseLabel}
            </Link>
          </>
        ) : null}
        {isInvestmentDetail ? (
          <>
            <ChevronRight className="size-3.5" />
            <span className="text-foreground">{t("investments.detail")}</span>
          </>
        ) : null}
      </nav>

      <div className="ml-auto flex items-center gap-1">
        <LocaleToggle />
        <ThemeToggle />
      </div>
    </header>
  );
}
