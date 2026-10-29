import "../styles/app.css";
import { supabase } from "../lib/supabaseClient";
import { isValidEmail } from "../lib/validation";

const form = document.getElementById("login-form") as HTMLFormElement;
const emailInput = document.getElementById("email") as HTMLInputElement;
const passwordInput = document.getElementById("password") as HTMLInputElement;
const emailError = document.getElementById("email-error") as HTMLParagraphElement;
const formError = document.getElementById("form-error") as HTMLParagraphElement;
const submitBtn = document.getElementById("submit-btn") as HTMLButtonElement;

function showError(el: HTMLElement, message: string) {
  el.textContent = message;
  el.classList.remove("hidden");
}

function hideError(el: HTMLElement) {
  el.textContent = "";
  el.classList.add("hidden");
}

// Si ya hay sesión activa, no tiene sentido mostrar el login de nuevo.
supabase.auth.getSession().then(({ data }) => {
  if (data.session) {
    window.location.href = "/perfil.html";
  }
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  hideError(emailError);
  hideError(formError);

  const email = emailInput.value.trim();
  const password = passwordInput.value;

  if (!isValidEmail(email)) {
    showError(emailError, "Ingresá un email válido.");
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = "Entrando…";

  const { error } = await supabase.auth.signInWithPassword({ email, password });

  if (error) {
    showError(formError, "Email o contraseña incorrectos.");
    submitBtn.disabled = false;
    submitBtn.textContent = "Entrar";
    return;
  }

  window.location.href = "/perfil.html";
});
