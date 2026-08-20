"use client";

import { ExternalLink } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { cn, formatRelativeTime } from "@/lib/utils";
import type { DeveloperCandidateRow, DeveloperRegistryRow } from "@/lib/types";

interface DevelopersViewProps {
  developers: DeveloperRegistryRow[];
  candidates: DeveloperCandidateRow[];
}

const STATUS_BADGE: Record<string, string> = {
  MONITORED: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  DISCOVERED: "border-purple-500/30 text-purple-500 dark:text-purple-400",
  CANDIDATE: "border-amber-500/30 text-amber-500 dark:text-amber-400",
  NO_CURRENT_INVESTMENTS: "border-border text-muted-foreground",
  INACTIVE: "border-border text-muted-foreground",
  BLOCKED: "border-red-500/30 text-red-500 dark:text-red-400",
};

const CANDIDATE_STATUS_BADGE: Record<string, string> = {
  NEW: "border-blue-500/30 text-blue-500 dark:text-blue-400",
  REVIEW_REQUIRED: "border-amber-500/30 text-amber-500 dark:text-amber-400",
  ACCEPTED: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  REJECTED: "border-red-500/30 text-red-500 dark:text-red-400",
  IMPLEMENTED: "border-emerald-500/30 text-emerald-500 dark:text-emerald-400",
  BLOCKED: "border-red-500/30 text-red-500 dark:text-red-400",
};

function DeveloperTierTable({ title, developers }: { title: string; developers: DeveloperRegistryRow[] }) {
  const { t } = useI18n();
  const monitored = developers.filter((d) => d.status === "MONITORED").length;

  return (
    <div className="space-y-3">
      <div className="flex items-baseline justify-between">
        <h2 className="text-lg font-semibold">{title}</h2>
        <span className="text-sm text-muted-foreground">
          {monitored} / {developers.length} {t("developers.monitored")}
        </span>
      </div>
      <div className="rounded-xl border border-border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("developers.name")}</TableHead>
              <TableHead>{t("developers.status")}</TableHead>
              <TableHead className="hidden md:table-cell">{t("developers.website")}</TableHead>
              <TableHead className="hidden md:table-cell">{t("developers.adapter")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {developers.map((dev) => (
              <TableRow key={dev.id}>
                <TableCell className="font-medium">{dev.name}</TableCell>
                <TableCell>
                  <Badge variant="outline" className={cn("text-[10px] uppercase", STATUS_BADGE[dev.status])}>
                    {t(`developers.statusLabel.${dev.status}` as "developers.statusLabel.MONITORED")}
                  </Badge>
                </TableCell>
                <TableCell className="hidden md:table-cell">
                  {dev.website ? (
                    <a
                      href={dev.website}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 text-muted-foreground hover:text-foreground hover:underline"
                    >
                      {new URL(dev.website).hostname}
                      <ExternalLink className="size-3" />
                    </a>
                  ) : (
                    <span className="text-muted-foreground">—</span>
                  )}
                </TableCell>
                <TableCell className="hidden font-mono text-xs text-muted-foreground md:table-cell">
                  {dev.adapter_source_id ?? "—"}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}

export function DevelopersView({ developers, candidates }: DevelopersViewProps) {
  const { t, locale } = useI18n();
  const tierA = developers.filter((d) => d.tier === "A");
  const tierB = developers.filter((d) => d.tier === "B");

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("developers.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("developers.subtitle")}</p>
      </div>

      <DeveloperTierTable title={t("developers.tierA")} developers={tierA} />
      <DeveloperTierTable title={t("developers.tierB")} developers={tierB} />

      <div className="space-y-3">
        <h2 className="text-lg font-semibold">{t("developers.discovered")}</h2>
        {candidates.length === 0 ? (
          <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
            {t("developers.noCandidates")}
          </p>
        ) : (
          <div className="rounded-xl border border-border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("developers.name")}</TableHead>
                  <TableHead className="hidden md:table-cell">{t("developers.municipality")}</TableHead>
                  <TableHead className="hidden md:table-cell">{t("developers.discoveredFrom")}</TableHead>
                  <TableHead>{t("developers.status")}</TableHead>
                  <TableHead>{t("developers.discoveredAt")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {candidates.map((candidate) => (
                  <TableRow key={candidate.id}>
                    <TableCell className="font-medium">
                      <a
                        href={candidate.discovered_url}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center gap-1 hover:underline"
                      >
                        {candidate.developer_name}
                        <ExternalLink className="size-3" />
                      </a>
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {candidate.municipality ?? "—"}
                    </TableCell>
                    <TableCell className="hidden font-mono text-xs text-muted-foreground md:table-cell">
                      {candidate.discovered_from_source}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={cn("text-[10px] uppercase", CANDIDATE_STATUS_BADGE[candidate.status])}
                      >
                        {candidate.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatRelativeTime(candidate.discovered_at, locale)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>
    </div>
  );
}
