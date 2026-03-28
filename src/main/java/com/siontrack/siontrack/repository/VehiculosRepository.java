package com.siontrack.siontrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.siontrack.siontrack.models.Vehiculos;

public interface VehiculosRepository extends JpaRepository<Vehiculos, Integer>{

     List<Vehiculos> findByPlacaIgnoreCase(String placa);

     @Query("SELECT v FROM Vehiculos v JOIN FETCH v.clientes WHERE LOWER(v.placa) LIKE LOWER(CONCAT('%', :placa, '%'))")
     List<Vehiculos> buscarPorPlacaContiene(@Param("placa") String placa);
}
