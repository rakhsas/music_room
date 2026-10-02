"use client";

import { createContext, useContext, useEffect, useRef, useState } from "react";
import { cn } from "@/lib/cn";

// Aceternity UI's 3D Card Effect - mouse-tracked perspective tilt. CardItem elements read
// isMouseEntered from context and lift/rotate independently, giving the card body a parallax feel.
const MouseEnterContext = createContext<[boolean, (v: boolean) => void] | null>(null);

export function CardContainer({
  children,
  className,
  containerClassName,
}: {
  children: React.ReactNode;
  className?: string;
  containerClassName?: string;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isMouseEntered, setIsMouseEntered] = useState(false);

  function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
    const container = containerRef.current;
    if (!container) return;
    const { left, top, width, height } = container.getBoundingClientRect();
    const x = (e.clientX - left - width / 2) / 18;
    const y = (e.clientY - top - height / 2) / 18;
    container.style.transform = `rotateY(${x}deg) rotateX(${-y}deg)`;
  }

  function handleMouseLeave() {
    setIsMouseEntered(false);
    if (containerRef.current) containerRef.current.style.transform = "rotateY(0deg) rotateX(0deg)";
  }

  return (
    <MouseEnterContext.Provider value={[isMouseEntered, setIsMouseEntered]}>
      <div className={cn("flex items-center justify-center", containerClassName)} style={{ perspective: "800px" }}>
        <div
          ref={containerRef}
          onMouseEnter={() => setIsMouseEntered(true)}
          onMouseMove={handleMouseMove}
          onMouseLeave={handleMouseLeave}
          className={cn("relative flex items-center justify-center transition-transform duration-200 ease-out", className)}
          style={{ transformStyle: "preserve-3d" }}
        >
          {children}
        </div>
      </div>
    </MouseEnterContext.Provider>
  );
}

export function CardBody({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn("[transform-style:preserve-3d]", className)}>{children}</div>;
}

export function CardItem({
  as: Tag = "div",
  children,
  className,
  translateZ = 0,
  ...rest
}: {
  as?: React.ElementType;
  children: React.ReactNode;
  className?: string;
  translateZ?: number;
  // Polymorphic "as" (e.g. next/link's Link needs `href`) - a strict HTMLAttributes type can't
  // express "whatever props the chosen tag accepts", so this stays a permissive passthrough.
  [key: string]: unknown;
}) {
  const ref = useRef<HTMLElement>(null);
  const [isMouseEntered] = useMouseEnter();

  useEffect(() => {
    if (!ref.current) return;
    ref.current.style.transform = isMouseEntered ? `translateZ(${translateZ}px)` : "translateZ(0px)";
  }, [isMouseEntered, translateZ]);

  return (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any -- generic polymorphic "as" element, ref type varies with Tag
    <Tag ref={ref as any} className={cn("transition-transform duration-200 ease-out", className)} {...rest}>
      {children}
    </Tag>
  );
}

function useMouseEnter() {
  const ctx = useContext(MouseEnterContext);
  if (!ctx) throw new Error("useMouseEnter must be used within CardContainer");
  return ctx;
}
