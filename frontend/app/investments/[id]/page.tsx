import { notFound } from "next/navigation";
import { getInvestment } from "@/lib/queries";
import { InvestmentDetailView } from "@/components/investment-detail-view";

export const dynamic = "force-dynamic";

export default async function InvestmentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const investment = getInvestment(Number(id));

  if (!investment) {
    notFound();
  }

  return <InvestmentDetailView investment={investment} />;
}
