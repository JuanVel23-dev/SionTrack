package com.siontrack.siontrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Usuarios;

@Repository
public interface UsuariosRepository extends JpaRepository<Usuarios, Integer>{
    
    Optional<Usuarios> findByNombreusuario(String nombreUsuario);

    boolean existsByNombreusuario(String nombreUsuario);

}
