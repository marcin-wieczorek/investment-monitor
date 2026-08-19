import { NextResponse } from "next/server";
import { getInvestment } from "@/lib/queries";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const investment = getInvestment(Number(id));

  if (!investment) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  return NextResponse.json(investment);
}
