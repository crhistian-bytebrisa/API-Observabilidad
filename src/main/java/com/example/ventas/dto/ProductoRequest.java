package com.example.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        Double precio
) {
}