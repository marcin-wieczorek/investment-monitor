import { NextResponse } from "next/server";
import { getScoringPreferences, saveScoringPreferences } from "@/lib/queries";
import { triggerRescore } from "@/lib/rescore";
import type { ScoringProfile } from "@/lib/types";

const PROPERTY_TYPES = ["TERRACED", "SEMI_DETACHED", "DETACHED", "APARTMENT"];
const LOCATION_TIERS = ["S", "A", "B"];

function isValidRange(value: unknown): value is { min: number | null; max: number | null } | null {
  if (value === null) return true;
  if (typeof value !== "object") return false;
  const range = value as Record<string, unknown>;
  const validBound = (b: unknown) => b === null || typeof b === "number";
  return validBound(range.min) && validBound(range.max);
}

function validateProfile(body: unknown): body is ScoringProfile {
  if (!body || typeof body !== "object") return false;
  const profile = body as Record<string, unknown>;
  if (typeof profile.name !== "string" || !profile.name) return false;
  if (
    !Array.isArray(profile.preferredPropertyTypes) ||
    !profile.preferredPropertyTypes.every((v) => PROPERTY_TYPES.includes(v as string))
  ) {
    return false;
  }
  if (
    !Array.isArray(profile.preferredLocationTiers) ||
    !profile.preferredLocationTiers.every((v) => LOCATION_TIERS.includes(v as string))
  ) {
    return false;
  }
  if (!isValidRange(profile.houseAreaRange)) return false;
  if (!isValidRange(profile.plotAreaRange)) return false;
  if (!isValidRange(profile.priceRange)) return false;
  if (typeof profile.largePlotPreferred !== "boolean") return false;
  if (profile.maxDistanceFromPoznanKm !== null && typeof profile.maxDistanceFromPoznanKm !== "number") return false;
  return true;
}

export async function GET() {
  return NextResponse.json(getScoringPreferences());
}

export async function PUT(request: Request) {
  const body = await request.json().catch(() => null);
  if (!validateProfile(body)) {
    return NextResponse.json({ error: "Invalid scoring profile payload" }, { status: 400 });
  }

  saveScoringPreferences(body);
  const rescoreResult = await triggerRescore();

  return NextResponse.json({ ok: true, profile: body, rescore: rescoreResult });
}
