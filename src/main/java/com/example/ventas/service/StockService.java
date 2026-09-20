package com.example.ventas.service;

import com.example.ventas.dto.StockRequest;
import com.example.ventas.dto.StockResponse;
import com.example.ventas.exception.RecursoNoEncontradoException;
import com.example.ventas.metrics.VentasMetrics;
import com.example.ventas.model.Stock;
import com.example.ventas.repository.StockRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockRepository repository;
    private final ProductoService productoService;
    private final VentasMetrics metrics;

    public StockService(StockRepository repository, ProductoService productoService, VentasMetrics metrics) {
        this.repository = repository;
        this.productoService = productoService;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<StockResponse> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional
    public StockResponse crear(StockRequest request) {
        if (repository.existsByProductoId(request.productoId())) {
            throw new IllegalArgumentException("Ya existe stock para el producto con id " + request.productoId());
        }
        Stock stock = new Stock(productoService.obtenerEntidad(request.productoId()), request.cantidad());
        StockResponse respuesta = toResponse(repository.save(stock));
        metrics.stockOperacion();
        return respuesta;
    }

    @Transactional
    public StockResponse actualizar(Long id, StockRequest request) {
        Stock existente = obtenerEntidad(id);
        if (request.productoId() != null && !request.productoId().equals(existente.getProducto().getId())) {
            if (repository.existsByProductoId(request.productoId())) {
                throw new IllegalArgumentException("Ya existe stock para el producto con id " + request.productoId());
            }
            existente.setProducto(productoService.obtenerEntidad(request.productoId()));
        }
        if (request.cantidad() != null) {
            existente.setCantidad(request.cantidad());
        }
        StockResponse respuesta = toResponse(repository.save(existente));
        metrics.stockOperacion();
        return respuesta;
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerEntidad(id);
        repository.deleteById(id);
        metrics.stockOperacion();
    }

    private Stock obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Stock con id " + id + " no encontrado"));
    }

    private StockResponse toResponse(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getProducto().getId(),
                stock.getProducto().getNombre(),
                stock.getCantidad()
        );
    }
}