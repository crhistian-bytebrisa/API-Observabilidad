package com.example.ventas.dto;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        Double precio
) {
}