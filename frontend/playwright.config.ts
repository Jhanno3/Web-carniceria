import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: false,
  retries: 0,
  use: {
    baseURL: "http://localhost:5174",
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5174",
    reuseExistingServer: true,
  },
});
