"use client";

import { useI18n } from "@/lib/i18n";
import { SourceCard } from "@/components/source-card";
import type { SourceSnapshotRow } from "@/lib/types";

export function SourcesView({ sources }: { sources: SourceSnapshotRow[] }) {
  const { t } = useI18n();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("sources.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("sources.subtitle")}</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        {sources.map((source) => (
          <SourceCard key={source.source} source={source} />
        ))}
      </div>
    </div>
  );
}
