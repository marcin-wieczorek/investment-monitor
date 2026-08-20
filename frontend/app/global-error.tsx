"use client";

import { useEffect } from "react";

/**
 * Root-level error boundary (Next.js App Router convention). Only used
 * when an error escapes `app/error.tsx` - i.e. the failure happened in the
 * root layout itself (`Providers`, `AppShell`, font loading, ...). Because
 * this replaces the *entire* document (`<html>`/`<body>`), it cannot rely
 * on `I18nProvider`/`ThemeProvider` (they're part of what may have
 * failed) - kept deliberately dependency-free and inline-styled so it
 * still renders even if global CSS failed to load.
 */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <html lang="en">
      <body
        style={{
          display: "flex",
          minHeight: "100vh",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: "1rem",
          padding: "1.5rem",
          textAlign: "center",
          fontFamily: "system-ui, sans-serif",
          background: "#0a0a0a",
          color: "#e5e5e5",
        }}
      >
        <h1 style={{ fontSize: "1.25rem", fontWeight: 600 }}>Application error</h1>
        <p style={{ maxWidth: "28rem", fontSize: "0.875rem", color: "#a3a3a3" }}>
          A critical error occurred and the application could not recover. Reloading may fix this.
        </p>
        <button
          onClick={reset}
          style={{
            padding: "0.5rem 1rem",
            borderRadius: "0.5rem",
            background: "#e5e5e5",
            color: "#0a0a0a",
            fontSize: "0.875rem",
            fontWeight: 500,
            border: "none",
            cursor: "pointer",
          }}
        >
          Try again
        </button>
        {process.env.NODE_ENV === "development" ? (
          <pre
            style={{
              marginTop: "0.5rem",
              maxWidth: "40rem",
              overflow: "auto",
              whiteSpace: "pre-wrap",
              borderRadius: "0.375rem",
              border: "1px solid #262626",
              background: "#171717",
              padding: "0.75rem",
              textAlign: "left",
              fontSize: "0.75rem",
              color: "#a3a3a3",
            }}
          >
            {error.stack ?? error.message}
          </pre>
        ) : null}
      </body>
    </html>
  );
}
