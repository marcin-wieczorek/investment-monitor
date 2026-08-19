import { NextResponse } from "next/server";
import { getInvestment } from "@/lib/queries";
import { parseId } from "@/lib/api-utils";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  const parsed = parseId(rawId);
  if ("error" in parsed) return parsed.error;

  const investment = getInvestment(parsed.id);

  if (!investment) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  return NextResponse.json(investment);
}
