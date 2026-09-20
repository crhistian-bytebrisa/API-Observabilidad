package com.example.ventas.repository;

import com.example.ventas.model.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    boolean existsByProductoId(Long productoId);

    Optional<Stock> findByProductoId(Long productoId);
}