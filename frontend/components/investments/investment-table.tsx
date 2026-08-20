"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Building2, Info } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import type { MessageKey } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ExpandableTableRow, ExpandChevron } from "@/components/expandable-table-row";
import { ScoreBadge } from "@/components/investments/score-badge";
import { cn, formatArea, formatPrice, formatRelativeTime } from "@/lib/utils";
import { NEW_THRESHOLD_MS } from "@/lib/constants";
import { CONFIDENCE_BADGE_CLASS, SOURCE_CATEGORY_BADGE_CLASS } from "@/lib/badge-styles";
import type { DuplicateLink } from "@/components/investments/use-duplicate-grouping";
import type { InvestmentWithState } from "@/lib/types";

const COLUMNS_COUNT = 8;

interface InvestmentTableProps {
  investments: InvestmentWithState[];
  representativeIdFor: (investment: InvestmentWithState) => number;
  siblingsFor: (investment: InvestmentWithState) => Array<{ investment: InvestmentWithState; confidence: DuplicateLink["confidence"] }>;
}

/**
 * The investments data table itself: header, rows, and the "other sources"
 * expanded panel for cross-source duplicates. Filtering/sorting happens
 * upstream (`useInvestmentFilters`) - this component only renders whatever
 * list it's given, and owns just the expand/collapse UI state.
 */
export function InvestmentTable({ investments, representativeIdFor, siblingsFor }: InvestmentTableProps) {
  const { t, locale } = useI18n();
  const [expandedId, setExpandedId] = useState<number | null>(null);

  if (investments.length === 0) {
    return (
      <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
        {t("investments.noResults")}
      </p>
    );
  }

  const visibleIds = new Set(investments.map((i) => i.id));

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("investments.title")}</TableHead>
            <TableHead className="hidden md:table-cell">{t("investments.source")}</TableHead>
            <TableHead className="hidden md:table-cell">{t("sources.title")}</TableHead>
            <TableHead className="hidden lg:table-cell">{t("investments.houseArea")}</TableHead>
            <TableHead className="hidden lg:table-cell">{t("investments.price")}</TableHead>
            <TableHead>
              <span className="inline-flex items-center gap-1">
                {t("investments.score")}
                <span title={t("investments.scoreExplanation")}>
                  <Info className="size-3.5 text-muted-foreground" />
                </span>
              </span>
            </TableHead>
            <TableHead>{t("investments.firstSeen")}</TableHead>
            <TableHead className="w-10" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {investments.map((investment) => {
            const representativeId = representativeIdFor(investment);
            if (representativeId !== investment.id && visibleIds.has(representativeId)) {
              // A more authoritative duplicate of this row is also visible - suppress this row,
              // its data still surfaces in the representative row's expanded "other sources" panel.
              return null;
            }

            return (
              <InvestmentRow
                key={investment.id}
                investment={investment}
                siblings={siblingsFor(investment)}
                isOpen={expandedId === investment.id}
                onToggle={() => setExpandedId((prev) => (prev === investment.id ? null : investment.id))}
                t={t}
                locale={locale}
              />
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

interface InvestmentRowProps {
  investment: InvestmentWithState;
  siblings: Array<{ investment: InvestmentWithState; confidence: DuplicateLink["confidence"] }>;
  isOpen: boolean;
  onToggle: () => void;
  t: (key: MessageKey) => string;
  locale: string;
}

function InvestmentRow({ investment, siblings, isOpen, onToggle, t, locale }: InvestmentRowProps) {
  const isNew = Date.now() - new Date(investment.first_seen_at).getTime() < NEW_THRESHOLD_MS;
  const houseArea = formatArea(investment.house_area_min, investment.house_area_max, t, locale);
  const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max, t, locale);
  const price = formatPrice(investment.price_min, investment.price_max, t, locale);

  return (
    <ExpandableTableRow
      isOpen={isOpen}
      onToggle={onToggle}
      columnsCount={COLUMNS_COUNT}
      data={investment}
      expandedExtra={siblings.length > 0 ? <OtherSourcesPanel siblings={siblings} t={t} /> : undefined}
    >
      <TableCell>
        <Link
          href={`/investments/${investment.id}`}
          onClick={(e) => e.stopPropagation()}
          className="flex items-center gap-3 whitespace-normal"
        >
          <div className="relative size-10 shrink-0 overflow-hidden rounded-lg bg-muted">
            {investment.image_url ? (
              <Image
                src={investment.image_url}
                alt={investment.name}
                fill
                sizes="40px"
                className="object-cover"
                unoptimized
              />
            ) : (
              <div className="flex h-full items-center justify-center">
                <Building2 className="size-4 text-muted-foreground" />
              </div>
            )}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <span className="truncate font-medium hover:underline">{investment.name}</span>
              {isNew ? <Badge className="bg-emerald-500 text-white hover:bg-emerald-500">{t("investments.new")}</Badge> : null}
              {investment.archived ? <Badge variant="secondary">{t("investments.archive")}</Badge> : null}
              {investment.watched ? (
                <Badge variant="secondary" className="border-amber-500/30 text-amber-500 dark:text-amber-400">
                  {t("investments.watched")}
                </Badge>
              ) : null}
              {investment.aggregator_only_discovery ? (
                <Badge variant="outline" className="border-orange-500/30 text-orange-500 dark:text-orange-400">
                  {t("investments.aggregatorOnly")}
                </Badge>
              ) : null}
              {siblings.length > 0 ? (
                <Badge variant="secondary" className="w-fit text-[10px]">
                  {t("investments.confirmedBySources").replace("{count}", String(siblings.length + 1))}
                </Badge>
              ) : null}
            </div>
            <span className="text-xs text-muted-foreground">{investment.location ?? t("investments.unknownLocation")}</span>
          </div>
        </Link>
      </TableCell>
      <TableCell className="hidden md:table-cell">
        <div className="flex flex-col gap-1">
          <span className="font-mono text-xs text-muted-foreground">{investment.source}</span>
          {investment.source_category ? (
            <Badge variant="outline" className={cn("w-fit text-[10px] uppercase", SOURCE_CATEGORY_BADGE_CLASS[investment.source_category])}>
              {t(`sources.${investment.source_category.toLowerCase()}` as "sources.developer")}
            </Badge>
          ) : null}
        </div>
      </TableCell>
      <TableCell className="hidden text-muted-foreground md:table-cell">{investment.developer}</TableCell>
      <TableCell className="hidden text-muted-foreground lg:table-cell">{houseArea ?? plotArea ?? "—"}</TableCell>
      <TableCell className="hidden text-muted-foreground lg:table-cell">{price ?? "—"}</TableCell>
      <TableCell>
        <ScoreBadge investment={investment} t={t} />
      </TableCell>
      <TableCell className="text-muted-foreground">{formatRelativeTime(investment.first_seen_at, locale)}</TableCell>
      <TableCell>
        <ExpandChevron open={isOpen} />
      </TableCell>
    </ExpandableTableRow>
  );
}

function OtherSourcesPanel({
  siblings,
  t,
}: {
  siblings: Array<{ investment: InvestmentWithState; confidence: DuplicateLink["confidence"] }>;
  t: (key: MessageKey) => string;
}) {
  const { tEnum } = useI18n();
  return (
    <div className="border-b border-border px-4 py-3">
      <h3 className="mb-2 text-xs font-medium text-muted-foreground">{t("investments.otherSources")}</h3>
      <ul className="space-y-1.5">
        {siblings.map(({ investment: sibling, confidence }) => (
          <li key={sibling.id} className="flex items-center gap-2 text-xs">
            <Badge variant="outline" className={cn("shrink-0 text-[10px]", CONFIDENCE_BADGE_CLASS[confidence])}>
              {tEnum("confidence", confidence)}
            </Badge>
            <span className="font-mono text-muted-foreground">{sibling.source}</span>
            <Link href={`/investments/${sibling.id}`} className="truncate hover:underline">
              {sibling.name}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
