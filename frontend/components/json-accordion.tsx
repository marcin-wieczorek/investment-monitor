"use client";

import { useState } from "react";
import { ChevronRight } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

interface JsonAccordionProps {
  data: unknown;
  label?: string;
  className?: string;
}

/**
 * Collapsible raw-data viewer. Renders a toggle button; when expanded, shows
 * the given data as formatted, syntax-free JSON inside a scrollable <pre>.
 */
export function JsonAccordion({ data, label, className }: JsonAccordionProps) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);

  return (
    <div className={className}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        aria-expanded={open}
      >
        <ChevronRight className={cn("size-3.5 transition-transform", open && "rotate-90")} />
        {label ?? t("common.rawData")}
      </button>
      {open ? (
        <pre className="mt-2 max-h-96 overflow-auto rounded-lg border border-border bg-muted/50 p-4 font-mono text-xs leading-relaxed text-foreground/90">
          {JSON.stringify(data, null, 2)}
        </pre>
      ) : null}
    </div>
  );
}
