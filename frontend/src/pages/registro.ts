import "../styles/app.css";
import { supabase } from "../lib/supabaseClient";
import { validateRegisterForm } from "../lib/validation";

const form = document.getElementById("register-form") as HTMLFormElement;
const emailInput = document.getElementById("email") as HTMLInputElement;
const passwordInput = document.getElementById("password") as HTMLInputElement;
const emailError = document.getElementById("email-error") as HTMLParagraphElement;
const passwordError = document.getElementById("password-error") as HTMLParagraphElement;
const formError = document.getElementById("form-error") as HTMLParagraphElement;
const formSuccess = document.getElementById("form-success") as HTMLParagraphElement;
const submitBtn = document.getElementById("submit-btn") as HTMLButtonElement;

function showError(el: HTMLElement, message: string) {
  el.textContent = message;
  el.classList.remove("hidden");
}

function hideError(el: HTMLElement) {
  el.textContent = "";
  el.classList.add("hidden");
}

supabase.auth.getSession().then(({ data }) => {
  if (data.session) {
    window.location.href = "/perfil.html";
  }
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  [emailError, passwordError, formError].forEach(hideError);
  formSuccess.classList.add("hidden");

  const email = emailInput.value.trim();
  const password = passwordInput.value;
  const errors = validateRegisterForm({ email, password });

  if (errors.email) showError(emailError, errors.email);
  if (errors.password) showError(passwordError, errors.password);
  if (errors.email || errors.password) return;

  submitBtn.disabled = true;
  submitBtn.textContent = "Creando cuenta…";

  const { data, error } = await supabase.auth.signUp({ email, password });

  submitBtn.disabled = false;
  submitBtn.textContent = "Crear cuenta";

  if (error) {
    showError(formError, error.message);
    return;
  }

  if (data.session) {
    // Confirmación de email deshabilitada (entorno de desarrollo): ya queda logueado.
    window.location.href = "/perfil.html";
    return;
  }

  form.reset();
  formSuccess.textContent = "Te mandamos un mail para confirmar la cuenta. Confirmá y después iniciá sesión.";
  formSuccess.classList.remove("hidden");
});
