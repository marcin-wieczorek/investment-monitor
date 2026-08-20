"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import en from "@/messages/en.json";
import pl from "@/messages/pl.json";

export type Locale = "en" | "pl";

const dictionaries: Record<Locale, Record<string, unknown>> = { en, pl };

const STORAGE_KEY = "investment-monitor-locale";

/**
 * Recursively builds a union of every dot-separated path through the
 * message dictionary (e.g. `"investments.title"`, `"filters.sort.price_min"`).
 * Only descends into plain objects - leaf values (the translated strings
 * themselves) terminate the recursion. This makes `t()` reject a typo'd or
 * renamed key at compile time instead of silently falling back to the raw
 * key string at runtime.
 */
type MessagePath<T> = T extends string
  ? never
  : {
      [K in keyof T & string]: T[K] extends string ? K : `${K}.${MessagePath<T[K]>}`;
    }[keyof T & string];

export type MessageKey = MessagePath<typeof en>;

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: MessageKey) => string;
  /**
   * Translates a raw backend enum value (e.g. `signal_type: "WZ_DECISION"`,
   * `status: "READY_FOR_HANDOVER"`) via `enum.<category>.<value>` - falls
   * back to the raw value itself (not the dotted key) if no translation
   * exists yet, so a newly-added Kotlin enum constant never regresses to
   * showing a literal i18n key in the UI. `category`/`value` are plain
   * `string` (not `MessageKey`) because they come from the database at
   * runtime, not from the static message dictionary.
   */
  tEnum: (category: string, value: string) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

function resolve(dictionary: Record<string, unknown>, key: string): string | undefined {
  const value = key
    .split(".")
    .reduce<unknown>(
      (acc, segment) =>
        acc && typeof acc === "object" ? (acc as Record<string, unknown>)[segment] : undefined,
      dictionary
    );
  return typeof value === "string" ? value : undefined;
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("en");

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === "en" || stored === "pl") {
      setLocaleState(stored);
    }
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    window.localStorage.setItem(STORAGE_KEY, next);
  }, []);

  const t = useCallback(
    (key: MessageKey) => resolve(dictionaries[locale], key) ?? resolve(dictionaries.en, key) ?? key,
    [locale]
  );

  const tEnum = useCallback(
    (category: string, value: string) => {
      const key = `enum.${category}.${value}`;
      return resolve(dictionaries[locale], key) ?? resolve(dictionaries.en, key) ?? value;
    },
    [locale]
  );

  const value = useMemo(() => ({ locale, setLocale, t, tEnum }), [locale, setLocale, t, tEnum]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within an I18nProvider");
  }
  return context;
}
