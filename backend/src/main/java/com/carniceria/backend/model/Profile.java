package com.carniceria.backend.model;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reglas de negocio del perfil de un usuario. Sin anotaciones de JPA ni de HTTP:
 * no conoce la base ni el framework web (ver constitution.md sección 7).
 */
public record Profile(
        UUID id,
        Role role,
        String firstName,
        String lastName,
        String phone,
        String addressText,
        Double addressLat,
        Double addressLng,
        Instant createdAt
) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,20}$");

    public Profile {
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Formato de teléfono inválido: " + phone);
        }
    }

    public boolean isComplete() {
        return isNotBlank(firstName) && isNotBlank(lastName) && isNotBlank(phone);
    }

    /**
     * Único punto para editar los datos de contacto. El id, el role y createdAt nunca cambian
     * por este camino: no existe ningún setter de role, ni acá ni en ningún otro lado del modelo.
     */
    public Profile withContactInfo(String firstName, String lastName, String phone,
                                    String addressText, Double addressLat, Double addressLng) {
        return new Profile(id, role, firstName, lastName, phone, addressText, addressLat, addressLng, createdAt);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
