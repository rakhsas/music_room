"use client";

import { motion } from "motion/react";
import { cn } from "@/lib/cn";

// Aceternity UI's BackgroundGradient, trimmed to a looping animated glow ring - used behind the
// currently-playing track's artwork.
export function BackgroundGradient({
  children,
  className,
  containerClassName,
}: {
  children: React.ReactNode;
  className?: string;
  containerClassName?: string;
}) {
  return (
    <div className={cn("group relative", containerClassName)}>
      <motion.div
        initial={{ backgroundPosition: "0 50%" }}
        animate={{ backgroundPosition: ["0% 50%", "100% 50%", "0% 50%"] }}
        transition={{ duration: 6, repeat: Infinity, repeatType: "reverse" }}
        style={{ backgroundSize: "400% 400%" }}
        className={cn(
          "absolute -inset-1 rounded-lg opacity-70 blur-md transition duration-500 will-change-transform group-hover:opacity-100",
          "bg-[linear-gradient(115deg,var(--color-primary),var(--color-accent),var(--color-purple),var(--color-electric-blue))]",
        )}
      />
      <div className={cn("relative", className)}>{children}</div>
    </div>
  );
}
