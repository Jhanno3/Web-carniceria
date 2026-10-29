package com.carniceria.backend.exception;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(UUID id) {
        super("No existe un perfil con id " + id);
    }
}
