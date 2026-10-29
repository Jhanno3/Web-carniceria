// Mismas reglas que model.Profile en el backend (ver constitution.md sección 7): acá solo
// dan feedback rápido en el formulario, la validación real sigue siendo la del backend.

const PHONE_PATTERN = /^[0-9+()\-\s]{6,20}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidPhone(phone: string): boolean {
  return PHONE_PATTERN.test(phone);
}

export function isValidEmail(email: string): boolean {
  return EMAIL_PATTERN.test(email);
}

export interface ProfileFormInput {
  firstName: string;
  lastName: string;
  phone: string;
}

export interface ProfileFormErrors {
  firstName?: string;
  lastName?: string;
  phone?: string;
}

export function validateProfileForm(input: ProfileFormInput): ProfileFormErrors {
  const errors: ProfileFormErrors = {};

  if (!input.firstName.trim()) {
    errors.firstName = "El nombre es obligatorio.";
  }
  if (!input.lastName.trim()) {
    errors.lastName = "El apellido es obligatorio.";
  }
  if (!input.phone.trim()) {
    errors.phone = "El teléfono es obligatorio.";
  } else if (!isValidPhone(input.phone.trim())) {
    errors.phone = "Formato de teléfono inválido.";
  }

  return errors;
}

export interface RegisterFormInput {
  email: string;
  password: string;
}

export interface RegisterFormErrors {
  email?: string;
  password?: string;
}

export function validateRegisterForm(input: RegisterFormInput): RegisterFormErrors {
  const errors: RegisterFormErrors = {};

  if (!isValidEmail(input.email)) {
    errors.email = "Ingresá un email válido.";
  }
  if (input.password.length < 6) {
    errors.password = "La contraseña tiene que tener al menos 6 caracteres.";
  }

  return errors;
}
