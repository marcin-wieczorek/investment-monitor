import { Badge } from "@/components/ui/badge";
import { cn, dataCompleteness, LOW_COMPLETENESS_THRESHOLD } from "@/lib/utils";
import type { MessageKey } from "@/lib/i18n";
import type { InvestmentWithState } from "@/lib/types";

function scoreBadgeClass(score: number): string {
  if (score >= 0.66) return "border-emerald-500/30 text-emerald-500 dark:text-emerald-400";
  if (score >= 0.4) return "border-amber-500/30 text-amber-500 dark:text-amber-400";
  return "border-rose-500/30 text-rose-500 dark:text-rose-400";
}

export function ScoreBadge({ investment, t }: { investment: InvestmentWithState; t: (key: MessageKey) => string }) {
  const score = investment.overall_score;
  if (score == null) {
    return <span className="text-xs text-muted-foreground">—</span>;
  }
  const completeness = dataCompleteness(investment);
  if (completeness < LOW_COMPLETENESS_THRESHOLD) {
    return (
      <Badge
        variant="outline"
        className="border-border text-muted-foreground"
        title={t("investments.insufficientDataTooltip")}
      >
        {t("investments.insufficientData")}
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className={cn("font-mono tabular-nums", scoreBadgeClass(score))}>
      {Math.round(score * 100)}%
    </Badge>
  );
}
