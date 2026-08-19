"use client";

import Image from "next/image";
import Link from "next/link";
import { Home, LandPlot, MapPin } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { formatArea, formatRelativeTime } from "@/lib/utils";
import { NEW_THRESHOLD_MS } from "@/lib/constants";
import type { InvestmentWithState } from "@/lib/types";

interface InvestmentCardProps {
  investment: InvestmentWithState;
}

export function InvestmentCard({ investment }: InvestmentCardProps) {
  const { t, locale } = useI18n();

  const isNew = Date.now() - new Date(investment.first_seen_at).getTime() < NEW_THRESHOLD_MS;
  const houseArea = formatArea(investment.house_area_min, investment.house_area_max, t);
  const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max, t);

  return (
    <Link href={`/investments/${investment.id}`} className="block">
      <Card className="group flex flex-row items-stretch gap-0 overflow-hidden p-0 transition-all hover:border-foreground/20 hover:shadow-sm">
        <div className="relative h-32 w-40 shrink-0 bg-muted sm:h-36 sm:w-48">
          {investment.image_url ? (
            <Image
              src={investment.image_url}
              alt={investment.name}
              fill
              sizes="192px"
              className="object-cover transition-transform duration-300 group-hover:scale-105"
              unoptimized
            />
          ) : (
            <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
              {t("investments.notPublished")}
            </div>
          )}
          {isNew ? (
            <Badge className="absolute left-2 top-2 gap-1 bg-emerald-500 text-white hover:bg-emerald-500">
              <span className="size-1.5 animate-pulse rounded-full bg-white" />
              {t("investments.new")}
            </Badge>
          ) : null}
        </div>

        <div className="flex min-w-0 flex-1 flex-col justify-center gap-2.5 px-5 py-4">
          <div className="flex items-start justify-between gap-3">
            <h3 className="text-base font-semibold leading-tight tracking-tight">
              {investment.name}
            </h3>
            <span className="shrink-0 whitespace-nowrap text-xs text-muted-foreground">
              {formatRelativeTime(investment.first_seen_at, locale)}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
            <span className="font-medium text-foreground/80">{investment.developer}</span>
            <span className="inline-flex items-center gap-1">
              <MapPin className="size-3.5 shrink-0" />
              {investment.location ?? t("investments.unknownLocation")}
            </span>
          </div>

          {(houseArea || plotArea) ? (
            <div className="flex flex-wrap gap-2 pt-0.5">
              {houseArea ? (
                <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-muted/50 px-2 py-1 text-xs text-muted-foreground">
                  <Home className="size-3.5" />
                  {houseArea}
                </span>
              ) : null}
              {plotArea ? (
                <span className="inline-flex items-center gap-1.5 rounded-md border border-border bg-muted/50 px-2 py-1 text-xs text-muted-foreground">
                  <LandPlot className="size-3.5" />
                  {plotArea}
                </span>
              ) : null}
            </div>
          ) : null}
        </div>
      </Card>
    </Link>
  );
}
