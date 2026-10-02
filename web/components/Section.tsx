"use client";

import { motion } from "motion/react";

export function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-10">
      <div className="mb-4 flex items-center gap-2.5">
        <span className="h-5 w-1 rounded-full bg-gradient-to-b from-primary to-purple" />
        <h2 className="text-xl font-bold tracking-tight text-text-primary">{title}</h2>
      </div>
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: "easeOut" }}
        className="scrollbar-thin flex gap-5 overflow-x-auto pb-4"
      >
        {children}
      </motion.div>
    </section>
  );
}
