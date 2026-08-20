import { NextResponse } from "next/server";
import { isDeveloperCandidateMutableStatus, setDeveloperCandidateStatus } from "@/lib/queries";
import { parseId } from "@/lib/api-utils";

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = await params;
  const parsed = parseId(rawId);
  if ("error" in parsed) return parsed.error;

  const body = await request.json().catch(() => null);
  const status = body?.status;
  if (!isDeveloperCandidateMutableStatus(status)) {
    return NextResponse.json(
      { error: `Invalid status: "${status}". Expected one of ACCEPTED, REJECTED, IMPLEMENTED, BLOCKED.` },
      { status: 400 }
    );
  }

  const updated = setDeveloperCandidateStatus(parsed.id, status);
  if (!updated) {
    return NextResponse.json({ error: `Developer candidate ${parsed.id} not found` }, { status: 404 });
  }
  return NextResponse.json({ ok: true, status });
}
