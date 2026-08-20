import { DEFAULT_INVESTMENT_LIMIT, listInvestmentDuplicates, listInvestments } from "@/lib/queries";
import { InvestmentsView } from "@/components/investments-view";

export const dynamic = "force-dynamic";

export default function InvestmentsPage() {
  const investments = listInvestments({ includeArchived: true });
  const duplicates = listInvestmentDuplicates();
  // Hitting the cap exactly is the signal that `DEFAULT_INVESTMENT_LIMIT`
  // actually truncated the result set - see lib/queries.ts.
  const possiblyTruncated = investments.length === DEFAULT_INVESTMENT_LIMIT;
  return (
    <InvestmentsView investments={investments} duplicates={duplicates} possiblyTruncated={possiblyTruncated} />
  );
}
