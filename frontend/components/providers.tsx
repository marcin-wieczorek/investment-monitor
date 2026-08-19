"use client";

import { ThemeProvider } from "next-themes";
import { I18nProvider } from "@/lib/i18n";
import { SidebarProvider } from "@/lib/sidebar-context";
import type { ReactNode } from "react";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="dark" enableSystem disableTransitionOnChange>
      <I18nProvider>
        <SidebarProvider>{children}</SidebarProvider>
      </I18nProvider>
    </ThemeProvider>
  );
}
