package com.example.ventas.repository;

import com.example.ventas.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByEmail(String email);

    Optional<Cliente> findByEmail(String email);
}