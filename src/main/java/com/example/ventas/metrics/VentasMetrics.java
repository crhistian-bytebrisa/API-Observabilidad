package com.example.ventas.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class VentasMetrics {

    private final Counter productosCreados;
    private final Counter productosEliminados;
    private final Counter clientesCreados;
    private final Counter clientesEliminados;
    private final Counter stockOperaciones;

    public VentasMetrics(MeterRegistry registry) {
        this.productosCreados = Counter.builder("ventas.productos.creados")
                .description("Numero total de productos creados")
                .register(registry);
        this.productosEliminados = Counter.builder("ventas.productos.eliminados")
                .description("Numero total de productos eliminados")
                .register(registry);
        this.clientesCreados = Counter.builder("ventas.clientes.creados")
                .description("Numero total de clientes creados")
                .register(registry);
        this.clientesEliminados = Counter.builder("ventas.clientes.eliminados")
                .description("Numero total de clientes eliminados")
                .register(registry);
        this.stockOperaciones = Counter.builder("ventas.stock.operaciones")
                .description("Numero total de operaciones sobre stock (crear/actualizar/eliminar)")
                .register(registry);
    }

    public void productoCreado() {
        productosCreados.increment();
    }

    public void productoEliminado() {
        productosEliminados.increment();
    }

    public void clienteCreado() {
        clientesCreados.increment();
    }

    public void clienteEliminado() {
        clientesEliminados.increment();
    }

    public void stockOperacion() {
        stockOperaciones.increment();
    }
}