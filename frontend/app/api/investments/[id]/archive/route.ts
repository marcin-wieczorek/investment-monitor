import { NextResponse } from "next/server";
import { investmentExists, setArchived } from "@/lib/queries";
import { parseId } from "@/lib/api-utils";

// PUT, not POST: this is an idempotent "set archived state to X" operation,
// same shape as /watch and /note - calling it twice with the same body
// produces the same result, which is the defining property of PUT.
export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  const parsed = parseId(rawId);
  if ("error" in parsed) return parsed.error;

  const body = await request.json().catch(() => ({ archived: true }));
  const archived = body?.archived !== false;

  if (!investmentExists(parsed.id)) {
    return NextResponse.json({ error: `Investment ${parsed.id} not found` }, { status: 404 });
  }

  setArchived(parsed.id, archived);
  return NextResponse.json({ ok: true, archived });
}
