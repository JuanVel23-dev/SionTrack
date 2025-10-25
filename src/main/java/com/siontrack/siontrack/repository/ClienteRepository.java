package com.siontrack.siontrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Clientes;

@Repository
public interface ClienteRepository extends JpaRepository<Clientes, Integer>{
}
