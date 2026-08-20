"use client";

import type { ReactNode } from "react";
import { ChevronRight } from "lucide-react";
import { TableRow, TableCell } from "@/components/ui/table";
import { cn } from "@/lib/utils";

interface ExpandableTableRowProps {
  isOpen: boolean;
  onToggle: () => void;
  columnsCount: number;
  data: unknown;
  /** Optional extra content rendered above the raw JSON dump when expanded. */
  expandedExtra?: ReactNode;
  children: ReactNode;
}

/** A table row that can be clicked to reveal a full-width JSON detail row beneath it. */
export function ExpandableTableRow({
  isOpen,
  onToggle,
  columnsCount,
  data,
  expandedExtra,
  children,
}: ExpandableTableRowProps) {
  return (
    <>
      <TableRow onClick={onToggle} className="cursor-pointer">
        {children}
      </TableRow>
      {isOpen ? (
        <TableRow className="hover:bg-transparent">
          <TableCell colSpan={columnsCount} className="whitespace-normal bg-muted/30 p-0">
            {expandedExtra}
            <pre className="max-h-96 overflow-auto p-4 font-mono text-xs leading-relaxed text-foreground/90">
              {JSON.stringify(data, null, 2)}
            </pre>
          </TableCell>
        </TableRow>
      ) : null}
    </>
  );
}

export function ExpandChevron({ open }: { open: boolean }) {
  return <ChevronRight className={cn("size-4 text-muted-foreground transition-transform", open && "rotate-90")} />;
}
