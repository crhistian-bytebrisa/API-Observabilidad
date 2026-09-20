package com.example.ventas.service;

import com.example.ventas.dto.ClienteRequest;
import com.example.ventas.dto.ClienteResponse;
import com.example.ventas.exception.RecursoNoEncontradoException;
import com.example.ventas.metrics.VentasMetrics;
import com.example.ventas.model.Cliente;
import com.example.ventas.repository.ClienteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final VentasMetrics metrics;

    public ClienteService(ClienteRepository repository, VentasMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        validarEmailUnico(request.email());
        Cliente cliente = new Cliente(request.nombre(), request.email(), request.telefono(), request.direccion());
        ClienteResponse respuesta = toResponse(repository.save(cliente));
        metrics.clienteCreado();
        return respuesta;
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente existente = obtenerEntidad(id);
        validarEmailUnicoExcepto(request.email(), id);
        if (request.nombre() != null) {
            existente.setNombre(request.nombre());
        }
        if (request.email() != null) {
            existente.setEmail(request.email());
        }
        if (request.telefono() != null) {
            existente.setTelefono(request.telefono());
        }
        if (request.direccion() != null) {
            existente.setDireccion(request.direccion());
        }
        return toResponse(repository.save(existente));
    }

    @Transactional
    public void eliminar(Long id) {
        obtenerEntidad(id);
        repository.deleteById(id);
        metrics.clienteEliminado();
    }

    public Cliente obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente con id " + id + " no encontrado"));
    }

    private void validarEmailUnico(String email) {
        if (email != null && repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un cliente con el email " + email);
        }
    }

    private void validarEmailUnicoExcepto(String email, Long id) {
        if (email != null) {
            repository.findByEmail(email)
                    .filter(cliente -> !cliente.getId().equals(id))
                    .ifPresent(cliente -> {
                        throw new IllegalArgumentException("Ya existe un cliente con el email " + email);
                    });
        }
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion()
        );
    }
}