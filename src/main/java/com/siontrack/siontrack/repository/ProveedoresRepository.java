package com.siontrack.siontrack.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.siontrack.siontrack.models.Proveedores;

public interface ProveedoresRepository extends JpaRepository <Proveedores, Integer> {

    @Query(value = "SELECT p FROM Proveedores p ORDER BY p.proveedor_id DESC",
           countQuery = "SELECT COUNT(p) FROM Proveedores p")
    Page<Proveedores> findAllOrderByIdDesc(Pageable pageable);

    Optional<Proveedores> findByNombreIgnoreCase(String nombre);

}
