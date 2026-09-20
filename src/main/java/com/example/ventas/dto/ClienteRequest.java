package com.example.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String email,

        @Size(max = 50, message = "El telefono no puede superar 50 caracteres")
        String telefono,

        @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
        String direccion
) {
}