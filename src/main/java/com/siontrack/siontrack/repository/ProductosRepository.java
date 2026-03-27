package com.siontrack.siontrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Productos;

@Repository
public interface ProductosRepository extends JpaRepository<Productos, Integer> {

    Optional<Productos> findByCodigoProducto(String codigoProducto);

    Optional<Productos> findByNombreIgnoreCase(String nombre);
}