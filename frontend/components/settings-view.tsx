"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, RotateCcw, Save } from "lucide-react";
import { useI18n } from "@/lib/i18n";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
import type { ScoringProfile } from "@/lib/types";
import { DEFAULT_SCORING_PROFILE } from "@/lib/types";

const PROPERTY_TYPES = ["TERRACED", "SEMI_DETACHED", "DETACHED", "APARTMENT"] as const;
const LOCATION_TIERS = ["S", "A", "B"] as const;

interface SettingsViewProps {
  initialProfile: ScoringProfile;
}

function toggleInArray<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
}

export function SettingsView({ initialProfile }: SettingsViewProps) {
  const { t, tEnum } = useI18n();
  const router = useRouter();

  const [profile, setProfile] = useState<ScoringProfile>(initialProfile);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function setRange(
    field: "houseAreaRange" | "plotAreaRange" | "priceRange",
    bound: "min" | "max",
    raw: string
  ) {
    const value = raw === "" ? null : Number(raw);
    setProfile((prev) => ({
      ...prev,
      [field]: { ...(prev[field] ?? { min: null, max: null }), [bound]: value },
    }));
  }

  async function save() {
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      const response = await fetch("/api/preferences", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(profile),
      });
      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        throw new Error(data.error ?? `HTTP ${response.status}`);
      }
      setSaved(true);
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  function resetToDefault() {
    setProfile(DEFAULT_SCORING_PROFILE);
    setSaved(false);
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{t("settings.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("settings.subtitle")}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.propertyTypes")}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-4">
          {PROPERTY_TYPES.map((type) => (
            <label key={type} className="flex items-center gap-2 text-sm">
              <Switch
                checked={profile.preferredPropertyTypes.includes(type)}
                onCheckedChange={() =>
                  setProfile((prev) => ({
                    ...prev,
                    preferredPropertyTypes: toggleInArray(prev.preferredPropertyTypes, type),
                  }))
                }
              />
              {tEnum("propertyType", type)}
            </label>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.locationTiers")}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-4">
          {LOCATION_TIERS.map((tier) => (
            <label key={tier} className="flex items-center gap-2 text-sm">
              <Switch
                checked={profile.preferredLocationTiers.includes(tier)}
                onCheckedChange={() =>
                  setProfile((prev) => ({
                    ...prev,
                    preferredLocationTiers: toggleInArray(prev.preferredLocationTiers, tier),
                  }))
                }
              />
              {t(`settings.tierLabel.${tier}`)}
            </label>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.houseArea")}</CardTitle>
        </CardHeader>
        <CardContent className="flex gap-3">
          <Input
            type="number"
            placeholder={t("filters.from")}
            value={profile.houseAreaRange?.min ?? ""}
            onChange={(e) => setRange("houseAreaRange", "min", e.target.value)}
          />
          <Input
            type="number"
            placeholder={t("filters.to")}
            value={profile.houseAreaRange?.max ?? ""}
            onChange={(e) => setRange("houseAreaRange", "max", e.target.value)}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.plotArea")}</CardTitle>
        </CardHeader>
        <CardContent className="flex gap-3">
          <Input
            type="number"
            placeholder={t("filters.from")}
            value={profile.plotAreaRange?.min ?? ""}
            onChange={(e) => setRange("plotAreaRange", "min", e.target.value)}
          />
          <Input
            type="number"
            placeholder={t("filters.to")}
            value={profile.plotAreaRange?.max ?? ""}
            onChange={(e) => setRange("plotAreaRange", "max", e.target.value)}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.price")}</CardTitle>
        </CardHeader>
        <CardContent className="flex gap-3">
          <Input
            type="number"
            placeholder={t("filters.from")}
            value={profile.priceRange?.min ?? ""}
            onChange={(e) => setRange("priceRange", "min", e.target.value)}
          />
          <Input
            type="number"
            placeholder={t("filters.to")}
            value={profile.priceRange?.max ?? ""}
            onChange={(e) => setRange("priceRange", "max", e.target.value)}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t("settings.largePlotPreferred")}</CardTitle>
        </CardHeader>
        <CardContent>
          <label className="flex items-center gap-2 text-sm">
            <Switch
              checked={profile.largePlotPreferred}
              onCheckedChange={(checked) =>
                setProfile((prev) => ({ ...prev, largePlotPreferred: Boolean(checked) }))
              }
            />
            {t("settings.largePlotPreferredHint")}
          </label>
        </CardContent>
      </Card>

      <Separator />

      <div className="flex items-center gap-3">
        <Button onClick={save} disabled={saving}>
          {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
          {saving ? t("settings.saving") : t("settings.save")}
        </Button>
        <Button variant="outline" onClick={resetToDefault} disabled={saving}>
          <RotateCcw className="size-4" />
          {t("settings.resetToDefault")}
        </Button>
        {saved ? <span className="text-sm text-emerald-500">{t("settings.saved")}</span> : null}
        {error ? <span className="text-sm text-rose-500">{error}</span> : null}
      </div>
      <p className="text-xs text-muted-foreground">{t("settings.rescoreNotice")}</p>
    </div>
  );
}
