package com.example.ventas.dto;

public record StockResponse(
        Long id,
        Long productoId,
        String productoNombre,
        Integer cantidad
) {
}