import { describe, expect, it } from "vitest";
import { isValidEmail, isValidPhone, validateProfileForm, validateRegisterForm } from "./validation";

describe("isValidPhone", () => {
  it("rechaza un teléfono con formato inválido", () => {
    expect(isValidPhone("no-es-un-telefono")).toBe(false);
  });

  it("acepta un teléfono con formato válido", () => {
    expect(isValidPhone("+54 11 1234-5678")).toBe(true);
  });
});

describe("validateProfileForm", () => {
  it("exige nombre, apellido y teléfono", () => {
    const errors = validateProfileForm({ firstName: "", lastName: "", phone: "" });
    expect(errors.firstName).toBeDefined();
    expect(errors.lastName).toBeDefined();
    expect(errors.phone).toBeDefined();
  });

  it("marca teléfono inválido aunque esté presente", () => {
    const errors = validateProfileForm({ firstName: "Juan", lastName: "Perez", phone: "abc" });
    expect(errors.phone).toBeDefined();
    expect(errors.firstName).toBeUndefined();
  });

  it("no tiene errores con datos válidos", () => {
    const errors = validateProfileForm({ firstName: "Juan", lastName: "Perez", phone: "+5491112345678" });
    expect(errors).toEqual({});
  });
});

describe("validateRegisterForm", () => {
  it("rechaza email inválido y contraseña corta", () => {
    const errors = validateRegisterForm({ email: "no-es-un-email", password: "123" });
    expect(errors.email).toBeDefined();
    expect(errors.password).toBeDefined();
  });

  it("acepta email y contraseña válidos", () => {
    const errors = validateRegisterForm({ email: "test@example.com", password: "123456" });
    expect(errors).toEqual({});
  });
});

describe("isValidEmail", () => {
  it("rechaza strings sin arroba", () => {
    expect(isValidEmail("test.example.com")).toBe(false);
  });
});
