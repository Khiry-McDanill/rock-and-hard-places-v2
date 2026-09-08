import { build } from "esbuild";
import { spawnSync } from "node:child_process";
import { mkdirSync, rmSync } from "node:fs";
const directory = new URL("../node_modules/.cache/rhp-tests/", import.meta.url);
mkdirSync(directory, { recursive: true });
const output = new URL("workspace.test.mjs", directory);
try {
  await build({
    entryPoints: [new URL("workspace.test.tsx", import.meta.url).pathname],
    outfile: output.pathname,
    bundle: true,
    platform: "node",
    format: "esm",
    packages: "external",
    jsx: "automatic",
  });
  const result = spawnSync(process.execPath, ["--test", output.pathname], {
    stdio: "inherit",
  });
  process.exitCode = result.status ?? 1;
} finally {
  rmSync(directory, { recursive: true, force: true });
}
