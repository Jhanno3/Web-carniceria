import { supabase } from "../lib/supabaseClient";

// No importa app.css a propósito: index.html tiene su propio sistema de estilos
// (ver AGENTS.md), este módulo solo agrega comportamiento al botón de cuenta del header.

function render(isLoggedIn: boolean) {
  const authBtn = document.getElementById("authBtn") as HTMLAnchorElement | null;
  const authLabel = document.getElementById("authLabel") as HTMLSpanElement | null;
  if (!authBtn || !authLabel) return;

  if (isLoggedIn) {
    authBtn.href = "/perfil.html";
    authLabel.textContent = "Mi perfil";
    authBtn.setAttribute("aria-label", "Ir a mi perfil");
  } else {
    authBtn.href = "/login.html";
    authLabel.textContent = "Iniciar sesión";
    authBtn.setAttribute("aria-label", "Iniciar sesión");
  }
}

supabase.auth.getSession().then(({ data }) => render(Boolean(data.session)));
supabase.auth.onAuthStateChange((_event, session) => render(Boolean(session)));
