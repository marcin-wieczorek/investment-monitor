import { cn } from "@/lib/utils";
import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface StatCardProps {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  tone?: "default" | "success" | "warning" | "danger";
  icon?: LucideIcon;
}

const iconToneClasses: Record<NonNullable<StatCardProps["tone"]>, string> = {
  default: "bg-muted text-muted-foreground",
  success: "bg-emerald-500/10 text-emerald-500 dark:text-emerald-400",
  warning: "bg-amber-500/10 text-amber-500 dark:text-amber-400",
  danger: "bg-rose-500/10 text-rose-500 dark:text-rose-400",
};

export function StatCard({ label, value, hint, tone = "default", icon: Icon }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 transition-colors hover:border-foreground/15 md:p-6">
      {Icon ? (
        <div className={cn("flex size-11 items-center justify-center rounded-xl", iconToneClasses[tone])}>
          <Icon className="size-5" />
        </div>
      ) : null}
      <div className="mt-4">
        <span className="text-sm text-muted-foreground">{label}</span>
        <div className="mt-1 font-mono text-2xl font-semibold tabular-nums text-foreground">
          {value}
        </div>
        {hint ? <p className="mt-1 text-xs text-muted-foreground">{hint}</p> : null}
      </div>
    </div>
  );
}
