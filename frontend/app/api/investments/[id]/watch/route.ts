import { NextResponse } from "next/server";
import { investmentExists, setWatched } from "@/lib/queries";
import { parseId } from "@/lib/api-utils";

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  const parsed = parseId(rawId);
  if ("error" in parsed) return parsed.error;

  const body = await request.json().catch(() => ({ watched: true }));
  const watched = body?.watched !== false;

  if (!investmentExists(parsed.id)) {
    return NextResponse.json({ error: `Investment ${parsed.id} not found` }, { status: 404 });
  }

  setWatched(parsed.id, watched);
  return NextResponse.json({ ok: true, watched });
}
