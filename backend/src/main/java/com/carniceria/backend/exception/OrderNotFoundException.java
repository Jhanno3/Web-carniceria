package com.carniceria.backend.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("No existe un pedido con id " + id);
    }
}
