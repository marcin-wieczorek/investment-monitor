import { NextResponse } from "next/server";

/**
 * Parses and validates a route param expected to be a positive integer ID.
 * Returns either the parsed number or a ready-to-return 400 response.
 */
export function parseId(raw: string): { id: number } | { error: NextResponse } {
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) {
    return {
      error: NextResponse.json({ error: `Invalid id: "${raw}"` }, { status: 400 }),
    };
  }
  return { id };
}
