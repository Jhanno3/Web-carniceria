import "../styles/app.css";
import { supabase } from "../lib/supabaseClient";
import { getMyProfile, updateMyProfile } from "../lib/api";
import { validateProfileForm } from "../lib/validation";

const loading = document.getElementById("loading") as HTMLParagraphElement;
const form = document.getElementById("profile-form") as HTMLFormElement;
const firstNameInput = document.getElementById("firstName") as HTMLInputElement;
const lastNameInput = document.getElementById("lastName") as HTMLInputElement;
const phoneInput = document.getElementById("phone") as HTMLInputElement;
const addressTextInput = document.getElementById("addressText") as HTMLInputElement;
const addressLatInput = document.getElementById("addressLat") as HTMLInputElement;
const addressLngInput = document.getElementById("addressLng") as HTMLInputElement;
const firstNameError = document.getElementById("firstName-error") as HTMLParagraphElement;
const lastNameError = document.getElementById("lastName-error") as HTMLParagraphElement;
const phoneError = document.getElementById("phone-error") as HTMLParagraphElement;
const geolocateError = document.getElementById("geolocate-error") as HTMLParagraphElement;
const formError = document.getElementById("form-error") as HTMLParagraphElement;
const formSuccess = document.getElementById("form-success") as HTMLParagraphElement;
const submitBtn = document.getElementById("submit-btn") as HTMLButtonElement;
const geolocateBtn = document.getElementById("geolocate-btn") as HTMLButtonElement;
const logoutBtn = document.getElementById("logout-btn") as HTMLButtonElement;

function showError(el: HTMLElement, message: string) {
  el.textContent = message;
  el.classList.remove("hidden");
}

function hideError(el: HTMLElement) {
  el.textContent = "";
  el.classList.add("hidden");
}

async function init() {
  const { data } = await supabase.auth.getSession();
  if (!data.session) {
    window.location.href = "/login.html";
    return;
  }

  try {
    const profile = await getMyProfile();
    firstNameInput.value = profile.firstName ?? "";
    lastNameInput.value = profile.lastName ?? "";
    phoneInput.value = profile.phone ?? "";
    addressTextInput.value = profile.addressText ?? "";
    addressLatInput.value = profile.addressLat?.toString() ?? "";
    addressLngInput.value = profile.addressLng?.toString() ?? "";
    loading.classList.add("hidden");
    form.classList.remove("hidden");
  } catch (error) {
    loading.textContent = "No pudimos cargar tu perfil. Recargá la página.";
  }
}

async function reverseGeocode(lat: number, lng: number): Promise<string | null> {
  const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&accept-language=es`;
  const response = await fetch(url, { headers: { Accept: "application/json" } });
  if (!response.ok) return null;
  const body = await response.json();
  return body.display_name ?? null;
}

geolocateBtn.addEventListener("click", () => {
  hideError(geolocateError);

  if (!("geolocation" in navigator)) {
    showError(geolocateError, "Tu navegador no soporta geolocalización. Escribí la dirección a mano.");
    return;
  }

  geolocateBtn.disabled = true;
  geolocateBtn.textContent = "Buscando ubicación…";

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const { latitude, longitude } = position.coords;
      addressLatInput.value = latitude.toString();
      addressLngInput.value = longitude.toString();

      try {
        const address = await reverseGeocode(latitude, longitude);
        if (address) addressTextInput.value = address;
      } catch {
        // La ubicación ya se guardó; si falla el geocoding, el usuario completa la dirección a mano.
      } finally {
        geolocateBtn.disabled = false;
        geolocateBtn.textContent = "Usar mi ubicación actual";
      }
    },
    () => {
      showError(geolocateError, "No pudimos acceder a tu ubicación. Escribí la dirección a mano.");
      geolocateBtn.disabled = false;
      geolocateBtn.textContent = "Usar mi ubicación actual";
    },
  );
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  [firstNameError, lastNameError, phoneError, formError].forEach(hideError);
  formSuccess.classList.add("hidden");

  const input = {
    firstName: firstNameInput.value,
    lastName: lastNameInput.value,
    phone: phoneInput.value,
  };
  const errors = validateProfileForm(input);

  if (errors.firstName) showError(firstNameError, errors.firstName);
  if (errors.lastName) showError(lastNameError, errors.lastName);
  if (errors.phone) showError(phoneError, errors.phone);
  if (errors.firstName || errors.lastName || errors.phone) return;

  submitBtn.disabled = true;
  submitBtn.textContent = "Guardando…";

  try {
    await updateMyProfile({
      firstName: input.firstName.trim(),
      lastName: input.lastName.trim(),
      phone: input.phone.trim(),
      addressText: addressTextInput.value.trim() || null,
      addressLat: addressLatInput.value ? Number(addressLatInput.value) : null,
      addressLng: addressLngInput.value ? Number(addressLngInput.value) : null,
    });
    formSuccess.textContent = "Perfil actualizado. Volviendo al inicio…";
    formSuccess.classList.remove("hidden");
    setTimeout(() => {
      window.location.href = "/";
    }, 900);
    return;
  } catch (error) {
    showError(formError, error instanceof Error ? error.message : "No pudimos guardar los cambios.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Guardar";
  }
});

logoutBtn.addEventListener("click", async () => {
  await supabase.auth.signOut();
  window.location.href = "/login.html";
});

init();
