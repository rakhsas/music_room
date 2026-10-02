import type { NextConfig } from "next";
import { existsSync, readFileSync } from "node:fs";
import { parseEnv } from "node:util";

// One .env for the whole repo, at the root. Next only reads env files inside web/, so pull the
// NEXT_PUBLIC_* keys from ../.env here (only those: backend secrets never enter this process).
// Absent in the Docker build, where docker-compose passes them as build args. Real env vars win.
if (existsSync("../.env")) {
  for (const [key, value] of Object.entries(parseEnv(readFileSync("../.env", "utf8")))) {
    if (key.startsWith("NEXT_PUBLIC_")) process.env[key] ??= value;
  }
}

const nextConfig: NextConfig = {
  // Self-contained server in .next/standalone - keeps the Docker image free of node_modules.
  output: "standalone",
};

export default nextConfig;
