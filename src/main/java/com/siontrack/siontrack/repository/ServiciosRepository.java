package com.siontrack.siontrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Servicios;

@Repository
public interface ServiciosRepository extends JpaRepository <Servicios, Integer> {
    

}
