import { NextResponse } from "next/server";
import { investmentExists, setNote } from "@/lib/queries";
import { parseId } from "@/lib/api-utils";

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  const parsed = parseId(rawId);
  if ("error" in parsed) return parsed.error;

  const body = await request.json().catch(() => null);

  if (!body || typeof body.note !== "string") {
    return NextResponse.json({ error: "Expected { note: string }" }, { status: 400 });
  }

  if (!investmentExists(parsed.id)) {
    return NextResponse.json({ error: `Investment ${parsed.id} not found` }, { status: 404 });
  }

  setNote(parsed.id, body.note);
  return NextResponse.json({ ok: true });
}
