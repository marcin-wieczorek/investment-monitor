"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";
import { ThemeToggle } from "@/components/theme-toggle";
import { LocaleToggle } from "@/components/locale-toggle";
import { ScanButton } from "@/components/scan-button";

const links = [
  { href: "/", key: "nav.dashboard" as const },
  { href: "/investments", key: "nav.investments" as const },
  { href: "/history", key: "nav.history" as const },
  { href: "/sources", key: "nav.sources" as const },
];

export function Nav() {
  const pathname = usePathname();
  const { t } = useI18n();

  return (
    <header className="sticky top-0 z-40 border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
        <div className="flex items-center gap-6">
          <span className="font-mono text-sm font-medium tracking-tight">
            investment-monitor
          </span>
          <nav className="hidden items-center gap-1 sm:flex">
            {links.map((link) => {
              const active =
                link.href === "/" ? pathname === "/" : pathname.startsWith(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={cn(
                    "rounded-md px-3 py-1.5 text-sm transition-colors",
                    active
                      ? "bg-secondary text-secondary-foreground"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  {t(link.key)}
                </Link>
              );
            })}
          </nav>
        </div>
        <div className="flex items-center gap-2">
          <ScanButton size="sm" />
          <div className="mx-1 h-5 w-px bg-border" />
          <LocaleToggle />
          <ThemeToggle />
        </div>
      </div>
      <nav className="flex items-center gap-1 overflow-x-auto border-t border-border/60 px-4 py-1.5 sm:hidden">
        {links.map((link) => {
          const active = link.href === "/" ? pathname === "/" : pathname.startsWith(link.href);
          return (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                "shrink-0 rounded-md px-3 py-1.5 text-sm transition-colors",
                active
                  ? "bg-secondary text-secondary-foreground"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {t(link.key)}
            </Link>
          );
        })}
      </nav>
    </header>
  );
}
