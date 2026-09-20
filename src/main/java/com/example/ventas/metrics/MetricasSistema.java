package com.example.ventas.metrics;

import com.example.ventas.repository.ClienteRepository;
import com.example.ventas.repository.ProductoRepository;
import com.example.ventas.repository.StockRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricasSistema {

    private final com.sun.management.OperatingSystemMXBean osBean =
            ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);

    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final StockRepository stockRepository;

    private final AtomicLong productos = new AtomicLong();
    private final AtomicLong clientes = new AtomicLong();
    private final AtomicLong stocks = new AtomicLong();

    public MetricasSistema(MeterRegistry registry,
                           ProductoRepository productoRepository,
                           ClienteRepository clienteRepository,
                           StockRepository stockRepository) {
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.stockRepository = stockRepository;

        Gauge.builder("ventas.ram.total.bytes", this, MetricasSistema::ramTotal)
                .description("Memoria RAM total del sistema en bytes")
                .register(registry);
        Gauge.builder("ventas.ram.libre.bytes", this, MetricasSistema::ramLibre)
                .description("Memoria RAM libre del sistema en bytes")
                .register(registry);
        Gauge.builder("ventas.ram.usada.bytes", this, MetricasSistema::ramUsada)
                .description("Memoria RAM usada del sistema en bytes")
                .register(registry);

        Gauge.builder("ventas.db.conteo.productos", productos, AtomicLong::get)
                .description("Numero de filas en la tabla productos")
                .register(registry);
        Gauge.builder("ventas.db.conteo.clientes", clientes, AtomicLong::get)
                .description("Numero de filas en la tabla clientes")
                .register(registry);
        Gauge.builder("ventas.db.conteo.stock", stocks, AtomicLong::get)
                .description("Numero de filas en la tabla stock")
                .register(registry);
        Gauge.builder("ventas.db.conteo.global", this,
                        m -> m.productos.get() + m.clientes.get() + m.stocks.get())
                .description("Numero total de elementos almacenados en la base de datos")
                .register(registry);
    }

    @Scheduled(fixedDelay = 30000)
    public void actualizarConteos() {
        productos.set(productoRepository.count());
        clientes.set(clienteRepository.count());
        stocks.set(stockRepository.count());
    }

    private double ramTotal() {
        return osBean.getTotalMemorySize();
    }

    private double ramLibre() {
        return osBean.getFreeMemorySize();
    }

    private double ramUsada() {
        return osBean.getTotalMemorySize() - osBean.getFreeMemorySize();
    }
}