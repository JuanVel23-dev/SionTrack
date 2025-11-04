package com.siontrack.siontrack.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Clientes;

@Repository
public interface ClienteRepository extends JpaRepository<Clientes, Integer>{

    @Query("SELECT COUNT(c) > 0 FROM Clientes c WHERE c.cedula_ruc = :cedulaRuc")
    boolean existsByCedula_ruc(@Param("cedulaRuc") String cedulaRuc);
}
