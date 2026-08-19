import { NextResponse } from "next/server";
import { setNote } from "@/lib/queries";

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const body = await request.json().catch(() => null);

  if (!body || typeof body.note !== "string") {
    return NextResponse.json({ error: "Expected { note: string }" }, { status: 400 });
  }

  setNote(Number(id), body.note);
  return NextResponse.json({ ok: true });
}
