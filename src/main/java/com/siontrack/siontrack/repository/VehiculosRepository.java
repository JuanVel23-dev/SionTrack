package com.siontrack.siontrack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siontrack.siontrack.models.Vehiculos;

public interface VehiculosRepository extends JpaRepository<Vehiculos, Integer>{

     List<Vehiculos> findByMarcaIgnoreCase(String marca);
}
