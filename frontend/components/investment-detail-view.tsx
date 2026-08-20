"use client";

import { useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Archive,
  ArchiveRestore,
  Clock,
  ExternalLink,
  Home,
  Info,
  LandPlot,
  MapPin,
  Star,
  Wallet,
} from "lucide-react";
import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";
import { JsonAccordion } from "@/components/json-accordion";
import { cn, dataCompleteness, formatArea, formatPrice, formatRelativeTime, LOW_COMPLETENESS_THRESHOLD } from "@/lib/utils";
import { CONFIDENCE_BADGE_CLASS } from "@/lib/badge-styles";
import type { CorrelationRow, InvestmentDuplicateRow, InvestmentWithState, SourceEvidenceRow } from "@/lib/types";

interface InvestmentDetailViewProps {
  investment: InvestmentWithState;
  evidence: SourceEvidenceRow[];
  correlations: CorrelationRow[];
  duplicates: InvestmentDuplicateRow[];
}

function ScoreBar({ label, value }: { label: string; value: number | null }) {
  const { t } = useI18n();
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-mono tabular-nums text-muted-foreground">
          {value == null ? t("investments.notPublished") : `${Math.round(value * 100)}%`}
        </span>
      </div>
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div
          className={cn(
            "h-full rounded-full",
            value == null ? "bg-transparent" : value >= 0.66 ? "bg-emerald-500" : value >= 0.4 ? "bg-amber-500" : "bg-rose-500"
          )}
          style={{ width: `${Math.round((value ?? 0) * 100)}%` }}
        />
      </div>
    </div>
  );
}

function groupEvidenceByField(evidence: SourceEvidenceRow[]): Map<string, SourceEvidenceRow[]> {
  const groups = new Map<string, SourceEvidenceRow[]>();
  evidence.forEach((item) => {
    const existing = groups.get(item.field_name);
    if (existing) existing.push(item);
    else groups.set(item.field_name, [item]);
  });
  return groups;
}

export function InvestmentDetailView({ investment, evidence, correlations, duplicates }: InvestmentDetailViewProps) {
  const { t, tEnum, locale } = useI18n();
  const router = useRouter();

  const [note, setNote] = useState(investment.note ?? "");
  const [savingNote, setSavingNote] = useState(false);
  const [noteSaved, setNoteSaved] = useState(false);
  const [noteError, setNoteError] = useState<string | null>(null);
  const [archived, setArchived] = useState(investment.archived);
  const [togglingArchive, setTogglingArchive] = useState(false);
  const [archiveError, setArchiveError] = useState<string | null>(null);
  const [watched, setWatched] = useState(investment.watched);
  const [togglingWatch, setTogglingWatch] = useState(false);
  const [watchError, setWatchError] = useState<string | null>(null);

  const houseArea = formatArea(investment.house_area_min, investment.house_area_max, t);
  const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max, t);
  const price = formatPrice(investment.price_min, investment.price_max, t);
  const evidenceByField = groupEvidenceByField(evidence);

  async function saveNote() {
    setSavingNote(true);
    setNoteSaved(false);
    setNoteError(null);
    try {
      const response = await fetch(`/api/investments/${investment.id}/note`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ note }),
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error ?? `HTTP ${response.status}`);
      }
      setNoteSaved(true);
      router.refresh();
    } catch (err) {
      setNoteError(err instanceof Error ? err.message : t("error.actionFailed"));
    } finally {
      setSavingNote(false);
    }
  }

  async function toggleArchive() {
    setTogglingArchive(true);
    setArchiveError(null);
    const next = !archived;
    try {
      const response = await fetch(`/api/investments/${investment.id}/archive`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ archived: next }),
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error ?? `HTTP ${response.status}`);
      }
      setArchived(next);
      router.refresh();
    } catch (err) {
      setArchiveError(err instanceof Error ? err.message : t("error.actionFailed"));
    } finally {
      setTogglingArchive(false);
    }
  }

  async function toggleWatch() {
    setTogglingWatch(true);
    setWatchError(null);
    const next = !watched;
    try {
      const response = await fetch(`/api/investments/${investment.id}/watch`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ watched: next }),
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error ?? `HTTP ${response.status}`);
      }
      setWatched(next);
      router.refresh();
    } catch (err) {
      setWatchError(err instanceof Error ? err.message : t("error.actionFailed"));
    } finally {
      setTogglingWatch(false);
    }
  }

  return (
    <div className="space-y-6">
      <Link
        href="/investments"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        {t("investments.title")}
      </Link>

      <Card className="overflow-hidden p-0">
        {investment.image_url ? (
          <div className="relative h-64 w-full bg-muted">
            <Image
              src={investment.image_url}
              alt={investment.name}
              fill
              sizes="768px"
              className="object-cover"
              unoptimized
            />
          </div>
        ) : null}

        <CardContent className="space-y-4 p-6">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">{investment.name}</h1>
              <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-muted-foreground">
                <span className="font-medium text-foreground/80">{investment.developer}</span>
                <span className="inline-flex items-center gap-1">
                  <MapPin className="size-3.5" />
                  {investment.location ?? t("investments.unknownLocation")}
                </span>
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                nativeButton={false}
                render={<a href={investment.url} target="_blank" rel="noreferrer" />}
              >
                <ExternalLink className="size-4" />
                {t("investments.visitSite")}
              </Button>
              <Button
                variant={watched ? "default" : "outline"}
                size="sm"
                onClick={toggleWatch}
                disabled={togglingWatch}
              >
                <Star className="size-4" />
                {watched ? t("investments.unwatch") : t("investments.watch")}
              </Button>
              <Button
                variant={archived ? "default" : "outline"}
                size="sm"
                onClick={toggleArchive}
                disabled={togglingArchive}
              >
                {archived ? (
                  <ArchiveRestore className="size-4" />
                ) : (
                  <Archive className="size-4" />
                )}
                {archived ? t("investments.unarchive") : t("investments.archive")}
              </Button>
            </div>
          </div>

          {archiveError || watchError ? (
            <p className="text-xs text-rose-500">{archiveError ?? watchError}</p>
          ) : null}

          <div className="flex flex-wrap gap-2">
            <Badge variant="secondary" className="gap-1">
              <Clock className="size-3.5" />
              {t("investments.firstSeen")}: {formatRelativeTime(investment.first_seen_at, locale)}
            </Badge>
            {houseArea ? (
              <Badge variant="secondary" className="gap-1">
                <Home className="size-3.5" />
                {houseArea}
              </Badge>
            ) : null}
            {plotArea ? (
              <Badge variant="secondary" className="gap-1">
                <LandPlot className="size-3.5" />
                {plotArea}
              </Badge>
            ) : null}
            {price ? (
              <Badge variant="secondary" className="gap-1">
                <Wallet className="size-3.5" />
                {price}
              </Badge>
            ) : null}
            {investment.plot_to_house_ratio != null ? (
              <Badge variant="secondary">
                {t("investments.plotToHouseRatio")}: {investment.plot_to_house_ratio.toFixed(1)}×
              </Badge>
            ) : null}
            {investment.units ? (
              <Badge variant="secondary">
                {investment.units} {t("investments.units")}
              </Badge>
            ) : null}
          </div>

          <Separator />

          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
                {t("investments.scoringBreakdown")}
                <span title={t("investments.scoreExplanation")}>
                  <Info className="size-3.5" />
                </span>
              </h2>
              <Badge variant="outline" className="gap-1 text-[10px] text-muted-foreground" title={t("investments.dataCompleteness")}>
                {t("investments.dataCompleteness")}: {Math.round(dataCompleteness(investment) * 6)}/6
              </Badge>
            </div>
            {investment.overall_score == null ? (
              <p className="text-xs text-muted-foreground">{t("investments.noScore")}</p>
            ) : dataCompleteness(investment) < LOW_COMPLETENESS_THRESHOLD ? (
              <p className="text-xs text-muted-foreground">{t("investments.insufficientDataTooltip")}</p>
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
                <ScoreBar label={t("investments.overallScore")} value={investment.overall_score} />
                <ScoreBar label={t("investments.houseAreaScore")} value={investment.house_area_score} />
                <ScoreBar label={t("investments.plotAreaScore")} value={investment.plot_area_score} />
                <ScoreBar label={t("investments.priceScoreLabel")} value={investment.price_score} />
              </div>
            )}
            <div className="flex flex-wrap gap-2">
              {investment.property_type_match != null ? (
                <Badge variant="outline" className={investment.property_type_match ? "border-emerald-500/30 text-emerald-500 dark:text-emerald-400" : "border-border text-muted-foreground"}>
                  {t("investments.propertyTypeMatch")}
                </Badge>
              ) : null}
              {investment.location_tier_match != null ? (
                <Badge variant="outline" className={investment.location_tier_match ? "border-emerald-500/30 text-emerald-500 dark:text-emerald-400" : "border-border text-muted-foreground"}>
                  {t("investments.locationTierMatch")}
                </Badge>
              ) : null}
              {investment.large_plot_bonus ? (
                <Badge variant="outline" className="border-emerald-500/30 text-emerald-500 dark:text-emerald-400">
                  {t("investments.largePlotBonus")}
                </Badge>
              ) : null}
            </div>
          </div>

          <Separator />

          <div className="space-y-2">
            <Textarea
              value={note}
              onChange={(e) => {
                setNote(e.target.value);
                setNoteSaved(false);
                setNoteError(null);
              }}
              placeholder={t("investments.notePlaceholder")}
              rows={4}
            />
            <div className="flex items-center gap-2">
              <Button size="sm" onClick={saveNote} disabled={savingNote}>
                {t("investments.saveNote")}
              </Button>
              {noteSaved ? (
                <span className="text-xs text-emerald-500">{t("investments.noteSaved")}</span>
              ) : null}
              {noteError ? <span className="text-xs text-rose-500">{noteError}</span> : null}
            </div>
          </div>

          <Separator />

          {evidenceByField.size > 0 ? (
            <div className="space-y-2">
              <h2 className="text-sm font-medium text-muted-foreground">{t("investments.evidence")}</h2>
              <div className="space-y-2">
                {Array.from(evidenceByField.entries()).map(([fieldName, items]) => {
                  const distinctSources = new Set(items.map((item) => item.source_id)).size;
                  return (
                    <div
                      key={fieldName}
                      className="space-y-1 rounded-md border border-border bg-muted/30 px-3 py-2 text-xs"
                    >
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                        <span className="font-mono font-medium text-foreground">{fieldName}</span>
                        <span className="text-muted-foreground">{items[0].field_value}</span>
                        {distinctSources > 1 ? (
                          <Badge variant="outline" className="border-emerald-500/30 text-[10px] text-emerald-500 dark:text-emerald-400">
                            {t("investments.confirmedBySources").replace("{count}", String(distinctSources))}
                          </Badge>
                        ) : null}
                      </div>
                      <div className="flex flex-wrap gap-x-3 gap-y-1">
                        {items.map((item) => (
                          <a
                            key={item.id}
                            href={item.url}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 text-muted-foreground hover:text-foreground hover:underline"
                          >
                            <Badge variant="outline" className="text-[10px] uppercase">
                              {item.source_category}
                            </Badge>
                            <span className="font-mono">{item.source_id}</span>
                            <span>{formatRelativeTime(item.captured_at, locale)}</span>
                            <ExternalLink className="size-3" />
                          </a>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <p className="text-xs text-muted-foreground">{t("investments.noEvidence")}</p>
          )}

          {correlations.length > 0 ? (
            <div className="space-y-2">
              <h2 className="text-sm font-medium text-muted-foreground">{t("investments.correlatedSignals")}</h2>
              <div className="space-y-1.5">
                {correlations.map((correlation) => (
                  <div
                    key={correlation.id}
                    className="flex flex-wrap items-start gap-x-3 gap-y-1 rounded-md border border-border bg-muted/30 px-3 py-2 text-xs"
                  >
                    <Badge variant="outline" className={CONFIDENCE_BADGE_CLASS[correlation.confidence]}>
                      {tEnum("confidence", correlation.confidence)}
                    </Badge>
                    <span className="min-w-0 flex-1">{correlation.signal_title}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-xs text-muted-foreground">{t("investments.noCorrelations")}</p>
          )}

          <Separator />

          {duplicates.length > 0 ? (
            <div className="space-y-2">
              <h2 className="text-sm font-medium text-muted-foreground">{t("investments.relatedListings")}</h2>
              <div className="space-y-1.5">
                {duplicates.map((duplicate) => {
                  const otherId = duplicate.investment_id_a === investment.id ? duplicate.investment_id_b : duplicate.investment_id_a;
                  const otherName = duplicate.investment_id_a === investment.id ? duplicate.investment_name_b : duplicate.investment_name_a;
                  const otherSource = duplicate.investment_id_a === investment.id ? duplicate.investment_source_b : duplicate.investment_source_a;
                  return (
                    <div
                      key={duplicate.id}
                      className="flex flex-wrap items-center gap-x-3 gap-y-1 rounded-md border border-border bg-muted/30 px-3 py-2 text-xs"
                    >
                      <Badge variant="outline" className={CONFIDENCE_BADGE_CLASS[duplicate.confidence]}>
                        {tEnum("confidence", duplicate.confidence)}
                      </Badge>
                      <span className="font-mono text-muted-foreground">{otherSource}</span>
                      <Link href={`/investments/${otherId}`} className="min-w-0 flex-1 truncate hover:underline">
                        {otherName}
                      </Link>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <p className="text-xs text-muted-foreground">{t("investments.noRelatedListings")}</p>
          )}

          <Separator />

          <JsonAccordion data={investment} />
        </CardContent>
      </Card>
    </div>
  );
}
