"use client";

import dynamic from "next/dynamic";
import type { ApexOptions } from "apexcharts";
import { useI18n } from "@/lib/i18n";
import type { MonitoringRunRow } from "@/lib/types";

const ReactApexChart = dynamic(() => import("react-apexcharts"), { ssr: false });

interface NewInvestmentsChartProps {
  /** Runs ordered most-recent-first, as returned by listRuns(). */
  runs: MonitoringRunRow[];
}

export function NewInvestmentsChart({ runs }: NewInvestmentsChartProps) {
  const { t, locale } = useI18n();
  const chronological = [...runs].reverse();

  const categories = chronological.map((run) =>
    new Date(run.started_at).toLocaleDateString(locale, { day: "2-digit", month: "short" })
  );
  const series = [
    {
      name: t("dashboard.newInvestments"),
      data: chronological.map((run) => run.new_investments),
    },
  ];

  const options: ApexOptions = {
    chart: {
      type: "area",
      height: 260,
      fontFamily: "var(--font-geist-sans), sans-serif",
      toolbar: { show: false },
    },
    colors: ["#10b981"],
    stroke: { curve: "smooth", width: 2 },
    fill: {
      type: "gradient",
      gradient: { opacityFrom: 0.4, opacityTo: 0 },
    },
    dataLabels: { enabled: false },
    grid: {
      xaxis: { lines: { show: false } },
      yaxis: { lines: { show: true } },
    },
    xaxis: {
      categories,
      axisBorder: { show: false },
      axisTicks: { show: false },
    },
    yaxis: {
      labels: { formatter: (value) => Math.round(value).toString() },
    },
    tooltip: { theme: "light" },
  };

  if (chronological.length === 0) {
    return (
      <p className="flex h-64 items-center justify-center text-sm text-muted-foreground">
        {t("history.noRuns")}
      </p>
    );
  }

  return <ReactApexChart options={options} series={series} type="area" height={260} />;
}
