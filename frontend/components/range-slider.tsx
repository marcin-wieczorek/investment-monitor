"use client";

import { Slider } from "@/components/ui/slider";

interface RangeSliderProps {
  label: string;
  min: number;
  max: number;
  step?: number;
  value: [number, number];
  onChange: (value: [number, number]) => void;
  formatValue?: (value: number) => string;
  fromLabel: string;
  toLabel: string;
}

/**
 * A labelled two-handle range slider ("od - do") used by the investments
 * filter panel for area (m²) and price ranges. Wraps the Base UI Slider
 * primitive, which natively supports an array value for range selection.
 */
export function RangeSlider({
  label,
  min,
  max,
  step = 1,
  value,
  onChange,
  formatValue = (v) => String(v),
  fromLabel,
  toLabel,
}: RangeSliderProps) {
  if (min >= max) return null;

  return (
    <div className="min-w-[220px] flex-1 space-y-2">
      <div className="flex items-center justify-between text-xs">
        <span className="font-medium text-foreground">{label}</span>
        <span className="text-muted-foreground">
          {fromLabel} {formatValue(value[0])} {toLabel} {formatValue(value[1])}
        </span>
      </div>
      <Slider
        min={min}
        max={max}
        step={step}
        value={value}
        onValueChange={(next) => {
          if (Array.isArray(next) && next.length === 2) {
            onChange([next[0], next[1]]);
          }
        }}
      />
    </div>
  );
}
