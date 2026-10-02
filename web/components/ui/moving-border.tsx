"use client";

import { useRef } from "react";
import { motion, useAnimationFrame, useMotionTemplate, useMotionValue, useTransform } from "motion/react";
import { cn } from "@/lib/cn";

// Aceternity UI's Moving Border - a dot travels the button's own outline path (via SVG
// getPointAtLength) and a radial-gradient glow follows it, producing an animated border.
export function MovingBorderButton({
  children,
  as: Tag = "button",
  duration = 2500,
  className,
  containerClassName,
  borderRadius = "9999px",
  ...rest
}: {
  children: React.ReactNode;
  as?: React.ElementType;
  duration?: number;
  className?: string;
  containerClassName?: string;
  borderRadius?: string;
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <Tag
      className={cn("relative overflow-hidden bg-transparent p-[1.5px]", containerClassName)}
      style={{ borderRadius }}
      {...rest}
    >
      <div className="absolute inset-0" style={{ borderRadius }}>
        <MovingBorder duration={duration}>
          <div className="h-16 w-16 bg-[radial-gradient(var(--color-accent)_40%,transparent_60%)] opacity-90" />
        </MovingBorder>
      </div>
      <div
        className={cn(
          "relative flex items-center justify-center bg-primary px-4 py-2 text-sm font-semibold text-background",
          className,
        )}
        style={{ borderRadius: `calc(${borderRadius} - 1.5px)` }}
      >
        {children}
      </div>
    </Tag>
  );
}

function MovingBorder({ children, duration = 2500 }: { children: React.ReactNode; duration?: number }) {
  const pathRef = useRef<SVGRectElement>(null);
  const progress = useMotionValue(0);

  useAnimationFrame((time) => {
    const length = pathRef.current?.getTotalLength();
    if (!length) return;
    const pxPerMs = length / duration;
    progress.set((time * pxPerMs) % length);
  });

  const x = useTransform(progress, (val) => pathRef.current?.getPointAtLength(val).x ?? 0);
  const y = useTransform(progress, (val) => pathRef.current?.getPointAtLength(val).y ?? 0);
  const transform = useMotionTemplate`translateX(${x}px) translateY(${y}px) translateX(-50%) translateY(-50%)`;

  return (
    <>
      <svg preserveAspectRatio="none" className="absolute h-full w-full" width="100%" height="100%">
        <rect fill="none" width="100%" height="100%" rx="9999" ry="9999" ref={pathRef} />
      </svg>
      <motion.div style={{ position: "absolute", top: 0, left: 0, display: "inline-block", transform }}>
        {children}
      </motion.div>
    </>
  );
}
