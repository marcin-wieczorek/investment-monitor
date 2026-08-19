import { listInvestments } from "@/lib/queries";
import { InvestmentsView } from "@/components/investments-view";

export const dynamic = "force-dynamic";

export default function InvestmentsPage() {
  const investments = listInvestments({ includeArchived: true });
  return <InvestmentsView investments={investments} />;
}
