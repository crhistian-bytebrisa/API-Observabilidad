package com.example.ventas.dto;

public record ClienteResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String direccion
) {
}