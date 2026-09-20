package com.example.ventas.service;

import com.example.ventas.dto.ProductoRequest;
import com.example.ventas.dto.ProductoResponse;
import com.example.ventas.exception.RecursoNoEncontradoException;
import com.example.ventas.metrics.VentasMetrics;
import com.example.ventas.model.Producto;
import com.example.ventas.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final VentasMetrics metrics;

    public ProductoService(ProductoRepository repository, VentasMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto(request.nombre(), request.descripcion(), request.precio());
        ProductoResponse respuesta = toResponse(repository.save(producto));
        metrics.productoCreado();
        return respuesta;
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto existente = obtenerEntidad(id);
        if (request.nombre() != null) {
            existente.setNombre(request.nombre());
        }
        if (request.descripcion() != null) {
            existente.setDescripcion(request.descripcion());
        }
        if (request.precio() != null) {
            existente.setPrecio(request.precio());
        }
        return toResponse(repository.save(existente));
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerEntidad(id);
        repository.deleteById(id);
        metrics.productoEliminado();
    }

    public Producto obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto con id " + id + " no encontrado"));
    }

    private ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio()
        );
    }
}