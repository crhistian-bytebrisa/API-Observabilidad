package com.example.ventas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockRequest(
        @NotNull(message = "El id del producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @PositiveOrZero(message = "La cantidad no puede ser negativa")
        Integer cantidad
) {
}