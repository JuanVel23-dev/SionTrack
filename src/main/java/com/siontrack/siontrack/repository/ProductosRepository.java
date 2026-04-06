package com.siontrack.siontrack.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Productos;

@Repository
public interface ProductosRepository extends JpaRepository<Productos, Integer> {

    @Query(value = "SELECT p FROM Productos p ORDER BY p.producto_id DESC",
           countQuery = "SELECT COUNT(p) FROM Productos p")
    Page<Productos> findAllOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT p FROM Productos p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.codigoProducto) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.categoria) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY p.producto_id DESC",
           countQuery = "SELECT COUNT(p) FROM Productos p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.codigoProducto) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.categoria) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Productos> buscarPaginado(@Param("q") String termino, Pageable pageable);

    Optional<Productos> findByCodigoProducto(String codigoProducto);

    Optional<Productos> findByNombreIgnoreCase(String nombre);
}