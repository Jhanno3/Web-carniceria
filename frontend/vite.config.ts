/// <reference types="vitest/config" />
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";

const resolvePath = (relative: string) => fileURLToPath(new URL(relative, import.meta.url));

export default defineConfig({
  plugins: [tailwindcss()],
  build: {
    rollupOptions: {
      input: {
        main: resolvePath("./index.html"),
        login: resolvePath("./login.html"),
        registro: resolvePath("./registro.html"),
        perfil: resolvePath("./perfil.html"),
        checkout: resolvePath("./checkout.html"),
        admin: resolvePath("./admin.html"),
      },
    },
  },
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
  },
});
