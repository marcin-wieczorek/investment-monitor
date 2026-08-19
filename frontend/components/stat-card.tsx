import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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

const toneClasses: Record<NonNullable<StatCardProps["tone"]>, string> = {
  default: "text-foreground",
  success: "text-emerald-500 dark:text-emerald-400",
  warning: "text-amber-500 dark:text-amber-400",
  danger: "text-rose-500 dark:text-rose-400",
};

const iconToneClasses: Record<NonNullable<StatCardProps["tone"]>, string> = {
  default: "bg-muted text-muted-foreground",
  success: "bg-emerald-500/10 text-emerald-500 dark:text-emerald-400",
  warning: "bg-amber-500/10 text-amber-500 dark:text-amber-400",
  danger: "bg-rose-500/10 text-rose-500 dark:text-rose-400",
};

export function StatCard({ label, value, hint, tone = "default", icon: Icon }: StatCardProps) {
  return (
    <Card className="transition-colors hover:border-foreground/15">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-normal text-muted-foreground">{label}</CardTitle>
        {Icon ? (
          <div className={cn("flex size-8 items-center justify-center rounded-lg", iconToneClasses[tone])}>
            <Icon className="size-4" />
          </div>
        ) : null}
      </CardHeader>
      <CardContent>
        <div className={cn("font-mono text-3xl font-medium tabular-nums", toneClasses[tone])}>
          {value}
        </div>
        {hint ? <p className="mt-1 text-xs text-muted-foreground">{hint}</p> : null}
      </CardContent>
    </Card>
  );
}
