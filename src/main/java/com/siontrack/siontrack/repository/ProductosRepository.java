package com.siontrack.siontrack.repository;

import java.time.LocalDate;
import java.util.List;
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

    // Productos que necesitan reabastecimiento (cantidad <= 1.5 * stock_minimo)
    @Query("SELECT p FROM Productos p JOIN FETCH p.inventario i " +
           "WHERE i.stock_minimo > 0 AND i.cantidad_disponible <= i.stock_minimo * 1.5")
    List<Productos> findProductosNecesitanRestock();

    // Query optimizada para reportes: trae inventario y proveedor en una sola consulta
    @Query("SELECT p FROM Productos p " +
           "LEFT JOIN FETCH p.inventario " +
           "LEFT JOIN FETCH p.proveedor " +
           "ORDER BY p.producto_id ASC")
    List<Productos> findAllParaReporte();

    // Reporte filtrado por rango de fechas de compra
    @Query("SELECT p FROM Productos p " +
           "LEFT JOIN FETCH p.inventario " +
           "LEFT JOIN FETCH p.proveedor " +
           "WHERE p.fecha_compra BETWEEN :desde AND :hasta " +
           "ORDER BY p.producto_id ASC")
    List<Productos> findAllParaReportePorFechas(@Param("desde") LocalDate desde,
                                                @Param("hasta") LocalDate hasta);
}