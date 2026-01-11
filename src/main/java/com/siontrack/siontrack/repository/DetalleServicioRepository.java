package com.siontrack.siontrack.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.DTO.Response.ProductoPopularDTO;
import com.siontrack.siontrack.models.Detalle_Servicio;

@Repository
public interface DetalleServicioRepository extends JpaRepository<Detalle_Servicio, Integer> {

    @Query("SELECT new com.siontrack.siontrack.DTO.Response.ProductoPopularDTO(" +
           "d.producto.producto_id, " +
           "d.producto.nombre, " +
           "d.producto.categoria, " +
           "SUM(d.cantidad)) " + 
           "FROM Detalle_Servicio d " +
           "WHERE d.tipo = 'PRODUCTO' " + 
           "AND d.servicio.fecha_servicio >= :fechaInicio " + 
           "GROUP BY d.producto.producto_id, d.producto.nombre, d.producto.categoria " +
           "ORDER BY SUM(d.cantidad) DESC")
    List<ProductoPopularDTO> encontrarProductsoPopulares(@Param("fechaInicio") LocalDate fehcaInicio, Pageable pageable);

}
