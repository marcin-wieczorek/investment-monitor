"use client";

import { useI18n } from "@/lib/i18n";
import { Button } from "@/components/ui/button";

export function LocaleToggle() {
  const { locale, setLocale } = useI18n();

  return (
    <Button
      variant="ghost"
      size="sm"
      onClick={() => setLocale(locale === "en" ? "pl" : "en")}
      className="font-mono text-xs uppercase"
    >
      {locale === "en" ? "PL" : "EN"}
    </Button>
  );
}
