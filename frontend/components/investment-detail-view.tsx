"use client";

import { useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { ArrowLeft, Archive, ArchiveRestore, Clock, ExternalLink, Home, LandPlot, MapPin } from "lucide-react";
import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";
import { formatArea, formatRelativeTime } from "@/lib/utils";
import type { InvestmentWithState } from "@/lib/types";

export function InvestmentDetailView({ investment }: { investment: InvestmentWithState }) {
  const { t, locale } = useI18n();
  const router = useRouter();

  const [note, setNote] = useState(investment.note ?? "");
  const [savingNote, setSavingNote] = useState(false);
  const [noteSaved, setNoteSaved] = useState(false);
  const [archived, setArchived] = useState(investment.archived);
  const [togglingArchive, setTogglingArchive] = useState(false);

  const houseArea = formatArea(investment.house_area_min, investment.house_area_max, t);
  const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max, t);

  async function saveNote() {
    setSavingNote(true);
    setNoteSaved(false);
    try {
      await fetch(`/api/investments/${investment.id}/note`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ note }),
      });
      setNoteSaved(true);
      router.refresh();
    } finally {
      setSavingNote(false);
    }
  }

  async function toggleArchive() {
    setTogglingArchive(true);
    try {
      const next = !archived;
      await fetch(`/api/investments/${investment.id}/archive`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ archived: next }),
      });
      setArchived(next);
      router.refresh();
    } finally {
      setTogglingArchive(false);
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
                render={<a href={investment.url} target="_blank" rel="noreferrer" />}
              >
                <ExternalLink className="size-4" />
                {t("investments.visitSite")}
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
            {investment.units ? (
              <Badge variant="secondary">
                {investment.units} {t("investments.units")}
              </Badge>
            ) : null}
          </div>

          <Separator />

          <div className="space-y-2">
            <Textarea
              value={note}
              onChange={(e) => {
                setNote(e.target.value);
                setNoteSaved(false);
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
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
