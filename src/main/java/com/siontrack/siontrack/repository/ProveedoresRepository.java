package com.siontrack.siontrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siontrack.siontrack.models.Proveedores;

public interface ProveedoresRepository extends JpaRepository <Proveedores, Integer> {

    Optional<Proveedores> findByNombreIgnoreCase(String nombre);

}
