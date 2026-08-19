"use client";

import Image from "next/image";
import Link from "next/link";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { formatArea, formatRelativeTime } from "@/lib/utils";
import type { InvestmentWithState } from "@/lib/types";

const NEW_THRESHOLD_MS = 7 * 24 * 60 * 60 * 1000;

interface InvestmentCardProps {
  investment: InvestmentWithState;
}

export function InvestmentCard({ investment }: InvestmentCardProps) {
  const { t, locale } = useI18n();

  const isNew = Date.now() - new Date(investment.first_seen_at).getTime() < NEW_THRESHOLD_MS;
  const houseArea = formatArea(investment.house_area_min, investment.house_area_max);
  const plotArea = formatArea(investment.plot_area_min, investment.plot_area_max);

  return (
    <Link href={`/investments/${investment.id}`}>
      <Card className="group flex flex-row items-stretch gap-0 overflow-hidden p-0 transition-colors hover:border-foreground/20">
        <div className="relative h-24 w-32 shrink-0 bg-muted">
          {investment.image_url ? (
            <Image
              src={investment.image_url}
              alt={investment.name}
              fill
              sizes="128px"
              className="object-cover"
              unoptimized
            />
          ) : (
            <div className="flex h-full items-center justify-center text-xs text-muted-foreground">
              {t("investments.notPublished")}
            </div>
          )}
          {isNew ? (
            <Badge className="absolute left-1.5 top-1.5 bg-emerald-500 text-white hover:bg-emerald-500">
              {t("investments.new")}
            </Badge>
          ) : null}
        </div>
        <div className="flex min-w-0 flex-1 flex-col justify-center gap-1 px-4 py-3">
          <div className="flex items-baseline justify-between gap-2">
            <h3 className="truncate font-medium">{investment.name}</h3>
            <span className="shrink-0 text-xs text-muted-foreground">
              {formatRelativeTime(investment.first_seen_at, locale)}
            </span>
          </div>
          <p className="truncate text-sm text-muted-foreground">
            {investment.developer} · {investment.location ?? t("investments.unknownLocation")}
          </p>
          <div className="flex gap-3 text-xs text-muted-foreground">
            {houseArea ? <span>{houseArea}</span> : null}
            {plotArea ? <span>· {t("investments.plotArea")}: {plotArea}</span> : null}
          </div>
        </div>
      </Card>
    </Link>
  );
}
