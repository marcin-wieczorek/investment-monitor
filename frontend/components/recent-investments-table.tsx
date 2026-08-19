"use client";

import Image from "next/image";
import Link from "next/link";
import { Building2 } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatRelativeTime } from "@/lib/utils";
import { NEW_THRESHOLD_MS } from "@/lib/constants";
import type { InvestmentWithState } from "@/lib/types";

export function RecentInvestmentsTable({ investments }: { investments: InvestmentWithState[] }) {
  const { t, locale } = useI18n();

  if (investments.length === 0) {
    return (
      <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
        {t("dashboard.noInvestmentsYet")}
      </p>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t("investments.title")}</TableHead>
          <TableHead className="hidden sm:table-cell">{t("sources.title")}</TableHead>
          <TableHead className="text-right">{t("investments.firstSeen")}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {investments.map((investment) => {
          const isNew = Date.now() - new Date(investment.first_seen_at).getTime() < NEW_THRESHOLD_MS;
          return (
            <TableRow key={investment.id}>
              <TableCell>
                <Link
                  href={`/investments/${investment.id}`}
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
                      <span className="truncate font-medium">{investment.name}</span>
                      {isNew ? (
                        <Badge className="bg-emerald-500 text-white hover:bg-emerald-500">
                          {t("investments.new")}
                        </Badge>
                      ) : null}
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {investment.location ?? t("investments.unknownLocation")}
                    </span>
                  </div>
                </Link>
              </TableCell>
              <TableCell className="hidden text-muted-foreground sm:table-cell">
                {investment.developer}
              </TableCell>
              <TableCell className="text-right text-muted-foreground">
                {formatRelativeTime(investment.first_seen_at, locale)}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
