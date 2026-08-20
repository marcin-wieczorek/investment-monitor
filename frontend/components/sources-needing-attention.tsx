"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { formatRelativeTime } from "@/lib/utils";
import { STALE_THRESHOLD_MS } from "@/lib/constants";
import type { DeveloperRegistryRow, MunicipalityRegistryRow, SourceSnapshotRow } from "@/lib/types";

interface SourcesNeedingAttentionProps {
  sources: SourceSnapshotRow[];
  developers: DeveloperRegistryRow[];
  municipalities: MunicipalityRegistryRow[];
}

export function SourcesNeedingAttention({ sources, developers, municipalities }: SourcesNeedingAttentionProps) {
  const { t, locale } = useI18n();

  const staleSources = sources.filter(
    (s) => Date.now() - new Date(s.captured_at).getTime() >= STALE_THRESHOLD_MS
  );
  const blockedDevelopers = developers.filter((d) => d.status === "BLOCKED");
  const blockedMunicipalities = municipalities.filter(
    (m) => m.developer_coverage === "BLOCKED" || m.discovery_coverage === "BLOCKED" || m.aggregator_coverage === "BLOCKED"
  );

  const nothingToShow =
    staleSources.length === 0 && blockedDevelopers.length === 0 && blockedMunicipalities.length === 0;

  if (nothingToShow) {
    return <p className="text-sm text-muted-foreground">{t("dashboard.noAttentionNeeded")}</p>;
  }

  return (
    <div className="space-y-4">
      {staleSources.length > 0 ? (
        <div className="space-y-2">
          <Link
            href="/sources"
            className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground hover:underline"
          >
            <AlertTriangle className="size-3.5 text-amber-500" />
            {t("dashboard.staleSources")} ({staleSources.length})
          </Link>
          <div className="flex flex-wrap gap-1.5">
            {staleSources.map((source) => (
              <Badge
                key={source.source}
                variant="outline"
                className="border-amber-500/30 font-mono text-[10px] text-amber-500 dark:text-amber-400"
                title={t("dashboard.staleSource").replace("{hours}", "24")}
              >
                {source.source} · {formatRelativeTime(source.captured_at, locale)}
              </Badge>
            ))}
          </div>
        </div>
      ) : null}

      {blockedDevelopers.length > 0 ? (
        <div className="space-y-2">
          <Link
            href="/developers"
            className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground hover:underline"
          >
            <AlertTriangle className="size-3.5 text-red-500" />
            {t("dashboard.blockedDevelopers")} ({blockedDevelopers.length})
          </Link>
          <div className="flex flex-wrap gap-1.5">
            {blockedDevelopers.map((dev) => (
              <Badge
                key={dev.id}
                variant="outline"
                className="border-red-500/30 text-[10px] text-red-500 dark:text-red-400"
              >
                {dev.name}
              </Badge>
            ))}
          </div>
        </div>
      ) : null}

      {blockedMunicipalities.length > 0 ? (
        <Link
          href="/coverage"
          className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground hover:underline"
        >
          <AlertTriangle className="size-3.5 text-red-500" />
          {t("dashboard.blockedMunicipalities")} ({blockedMunicipalities.length})
        </Link>
      ) : null}
    </div>
  );
}
