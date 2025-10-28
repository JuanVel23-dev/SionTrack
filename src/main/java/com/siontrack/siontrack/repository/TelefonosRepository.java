package com.siontrack.siontrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Cliente_Telefonos;

@Repository
public interface TelefonosRepository extends JpaRepository<Cliente_Telefonos, Integer>{

}
