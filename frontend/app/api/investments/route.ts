import { NextRequest, NextResponse } from "next/server";
import { listInvestments } from "@/lib/queries";

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;

  const investments = listInvestments({
    developer: searchParams.get("developer") ?? undefined,
    location: searchParams.get("location") ?? undefined,
    includeArchived: searchParams.get("includeArchived") === "true",
  });

  return NextResponse.json(investments);
}
