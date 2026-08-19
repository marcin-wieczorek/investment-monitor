import { NextResponse } from "next/server";
import { setArchived } from "@/lib/queries";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const body = await request.json().catch(() => ({ archived: true }));
  const archived = body?.archived !== false;

  setArchived(Number(id), archived);
  return NextResponse.json({ ok: true, archived });
}
