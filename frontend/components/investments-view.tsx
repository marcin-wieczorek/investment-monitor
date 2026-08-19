"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { InvestmentCard } from "@/components/investment-card";
import type { InvestmentWithState } from "@/lib/types";

interface InvestmentsViewProps {
  investments: InvestmentWithState[];
}

const ALL_DEVELOPERS = "__all__";

export function InvestmentsView({ investments }: InvestmentsViewProps) {
  const { t } = useI18n();
  const [search, setSearch] = useState("");
  const [developer, setDeveloper] = useState<string>(ALL_DEVELOPERS);
  const [showArchived, setShowArchived] = useState(false);

  const developers = useMemo(
    () => Array.from(new Set(investments.map((i) => i.developer))).sort(),
    [investments]
  );

  const filtered = useMemo(() => {
    return investments.filter((investment) => {
      if (!showArchived && investment.archived) return false;
      if (developer !== ALL_DEVELOPERS && investment.developer !== developer) return false;
      if (search) {
        const haystack = `${investment.name} ${investment.location ?? ""}`.toLowerCase();
        if (!haystack.includes(search.toLowerCase())) return false;
      }
      return true;
    });
  }, [investments, search, developer, showArchived]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("investments.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("investments.subtitle")}</p>
      </div>

      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-border bg-card/50 p-3">
        <div className="relative max-w-xs flex-1 min-w-[180px]">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={t("investments.searchLocation")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-8"
          />
        </div>
        <Select value={developer} onValueChange={(value) => setDeveloper(value ?? ALL_DEVELOPERS)}>
          <SelectTrigger className="w-48">
            <SelectValue>
              {(value: string) => (value === ALL_DEVELOPERS ? t("investments.allDevelopers") : value)}
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_DEVELOPERS}>{t("investments.allDevelopers")}</SelectItem>
            {developers.map((dev) => (
              <SelectItem key={dev} value={dev}>
                {dev}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <label className="flex items-center gap-2 text-sm text-muted-foreground">
          <Switch checked={showArchived} onCheckedChange={setShowArchived} />
          {t("investments.showArchived")}
        </label>
      </div>

      <p className="text-xs text-muted-foreground">
        {filtered.length} {t("investments.title").toLowerCase()}
      </p>

      {filtered.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
          {t("investments.noResults")}
        </p>
      ) : (
        <div className="space-y-3">
          {filtered.map((investment) => (
            <InvestmentCard key={investment.id} investment={investment} />
          ))}
        </div>
      )}
    </div>
  );
}
