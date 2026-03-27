package com.siontrack.siontrack.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Clientes;

@Repository
public interface ClienteRepository extends JpaRepository<Clientes, Integer> {

    @Query("SELECT COUNT(c) > 0 FROM Clientes c WHERE c.cedula_ruc = :cedulaRuc")
    boolean existsByCedula_ruc(@Param("cedulaRuc") String cedulaRuc);

    @Query("SELECT c FROM Clientes c WHERE c.cedula_ruc = :cedulaRuc")
    Optional<Clientes> findByCedulaRuc(@Param("cedulaRuc") String cedulaRuc);

    Optional<Clientes> findByNombreIgnoreCase(String nombre);

    Optional<Clientes> findByTelefonos_Telefono(String telefono);

    @Query("SELECT DISTINCT c FROM Clientes c JOIN c.telefonos t WHERE t.telefono = :telefono")
    List<Clientes> buscarPorTelefonos_Telefono(@Param("telefono") String telefono);
}
