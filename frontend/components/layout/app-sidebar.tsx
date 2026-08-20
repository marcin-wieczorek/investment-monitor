"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Building2, LayoutDashboard, History, Radio, Radar, Link2, Users, Map, MapPin, Settings } from "lucide-react";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";
import { useSidebar } from "@/lib/sidebar-context";
import { ScanButton } from "@/components/scan-button";
import { ScanProgress } from "@/components/scan-progress";

const navItems = [
  { href: "/", key: "nav.dashboard" as const, icon: LayoutDashboard },
  { href: "/investments", key: "nav.investments" as const, icon: Building2 },
  { href: "/map", key: "nav.map" as const, icon: MapPin },
  { href: "/signals", key: "nav.signals" as const, icon: Radar },
  { href: "/correlations", key: "nav.correlations" as const, icon: Link2 },
  { href: "/developers", key: "nav.developers" as const, icon: Users },
  { href: "/coverage", key: "nav.coverage" as const, icon: Map },
  { href: "/sources", key: "nav.sources" as const, icon: Radio },
  { href: "/history", key: "nav.history" as const, icon: History },
  { href: "/settings", key: "nav.settings" as const, icon: Settings },
];

export function AppSidebar() {
  const pathname = usePathname();
  const { t } = useI18n();
  const { isExpanded, isMobileOpen, closeMobileSidebar } = useSidebar();

  const showLabels = isExpanded || isMobileOpen;

  return (
    <>
      {isMobileOpen ? (
        <button
          type="button"
          aria-label="Close sidebar"
          onClick={closeMobileSidebar}
          className="fixed inset-0 z-40 bg-black/40 lg:hidden"
        />
      ) : null}

      <aside
        className={cn(
          "fixed left-0 top-0 z-50 flex h-screen flex-col border-r border-border bg-card transition-all duration-300 ease-in-out",
          showLabels ? "w-64" : "w-20",
          isMobileOpen ? "translate-x-0" : "-translate-x-full",
          "lg:translate-x-0"
        )}
      >
        <div className={cn("flex h-14 items-center border-b border-border px-4", !showLabels && "justify-center px-0")}>
          {showLabels ? (
            <span className="font-mono text-sm font-semibold tracking-tight">
              investment-monitor
            </span>
          ) : (
            <span className="font-mono text-sm font-semibold">IM</span>
          )}
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          {navItems.map((item) => {
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={closeMobileSidebar}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  !showLabels && "justify-center px-0",
                  active
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                )}
                title={!showLabels ? t(item.key) : undefined}
              >
                <Icon className="size-5 shrink-0" />
                {showLabels ? <span>{t(item.key)}</span> : null}
              </Link>
            );
          })}
        </nav>

        <div className={cn("border-t border-border p-3", !showLabels && "flex flex-col items-center")}>
          {showLabels ? (
            <ScanButton className="w-full justify-center" />
          ) : (
            <ScanButton size="icon" iconOnly />
          )}
          <ScanProgress collapsed={!showLabels} />
        </div>
      </aside>
    </>
  );
}
