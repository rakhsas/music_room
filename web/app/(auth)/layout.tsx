import { Spotlight } from "@/components/ui/spotlight";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative flex min-h-screen w-full items-center justify-center overflow-hidden bg-background px-4">
      <Spotlight className="-top-40 left-0 md:left-60 md:-top-20" fill="var(--color-primary)" />
      <Spotlight className="top-10 left-full h-[80vh] w-[50vw]" fill="var(--color-purple)" />

      <div className="relative z-10 w-full max-w-sm">
        <div className="mb-8 flex items-center justify-center gap-2 text-2xl font-extrabold text-text-primary">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-primary via-primary-deep to-purple text-background shadow-lg shadow-primary/30">
            M
          </span>
          Music Room
        </div>
        {children}
      </div>
    </div>
  );
}
