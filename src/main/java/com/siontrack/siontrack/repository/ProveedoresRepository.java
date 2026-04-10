package com.siontrack.siontrack.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.siontrack.siontrack.models.Proveedores;

public interface ProveedoresRepository extends JpaRepository <Proveedores, Integer> {

    @Query(value = "SELECT p FROM Proveedores p ORDER BY p.proveedor_id DESC",
           countQuery = "SELECT COUNT(p) FROM Proveedores p")
    Page<Proveedores> findAllOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT p FROM Proveedores p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.nombre_contacto) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.telefono) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY p.proveedor_id DESC",
           countQuery = "SELECT COUNT(p) FROM Proveedores p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.nombre_contacto) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.telefono) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Proveedores> buscarPaginado(@Param("q") String termino, Pageable pageable);

    Optional<Proveedores> findByNombreIgnoreCase(String nombre);

    public boolean existsByNombreIgnoreCase(String nombre);

    // Proveedores que tuvieron al menos un producto comprado en el rango de fechas
    @Query("SELECT DISTINCT p FROM Proveedores p " +
           "JOIN p.productos prod " +
           "WHERE prod.fecha_compra BETWEEN :desde AND :hasta " +
           "ORDER BY p.proveedor_id ASC")
    List<Proveedores> findProveedoresConProductosEnRango(@Param("desde") LocalDate desde,
                                                         @Param("hasta") LocalDate hasta);
}
