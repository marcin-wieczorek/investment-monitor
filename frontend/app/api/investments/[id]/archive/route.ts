import { NextResponse } from "next/server";
import { setArchived } from "@/lib/queries";
import { parseId } from "@/lib/api-utils";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  const parsed = parseId(rawId);
  if ("error" in parsed) return parsed.error;

  const body = await request.json().catch(() => ({ archived: true }));
  const archived = body?.archived !== false;

  setArchived(parsed.id, archived);
  return NextResponse.json({ ok: true, archived });
}
