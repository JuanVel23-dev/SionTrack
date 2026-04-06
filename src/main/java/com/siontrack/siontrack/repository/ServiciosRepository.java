package com.siontrack.siontrack.repository;


import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Servicios;

@Repository
public interface ServiciosRepository extends JpaRepository <Servicios, Integer> {

    @Query(value = "SELECT s FROM Servicios s ORDER BY s.servicio_id DESC",
           countQuery = "SELECT COUNT(s) FROM Servicios s")
    Page<Servicios> findAllOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT s FROM Servicios s WHERE " +
           "LOWER(s.clientes.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.tipo_servicio) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "(s.vehiculos IS NOT NULL AND LOWER(s.vehiculos.placa) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY s.servicio_id DESC",
           countQuery = "SELECT COUNT(s) FROM Servicios s WHERE " +
           "LOWER(s.clientes.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.tipo_servicio) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "(s.vehiculos IS NOT NULL AND LOWER(s.vehiculos.placa) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Servicios> buscarPaginado(@Param("q") String termino, Pageable pageable);

    @Query("SELECT COUNT(s) > 0 FROM Servicios s WHERE s.clientes.cliente_id = :clienteId " +
           "AND (:vehiculoId IS NULL OR s.vehiculos.vehiculo_id = :vehiculoId) " +
           "AND s.fecha_servicio = :fecha AND s.tipo_servicio = :tipoServicio")
    boolean existsDuplicado(@Param("clienteId") Integer clienteId,
                            @Param("vehiculoId") Integer vehiculoId,
                            @Param("fecha") LocalDate fecha,
                            @Param("tipoServicio") String tipoServicio);

}
