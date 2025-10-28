package com.siontrack.siontrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siontrack.siontrack.models.Cliente_Correos;

public interface CorreosRepository extends JpaRepository<Cliente_Correos, Integer> {

}
