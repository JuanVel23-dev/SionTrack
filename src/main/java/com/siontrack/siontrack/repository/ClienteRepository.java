package com.siontrack.siontrack.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.siontrack.siontrack.models.Clientes;

@Repository
public interface ClienteRepository extends JpaRepository<Clientes, Integer> {

    @Query(value = "SELECT c FROM Clientes c ORDER BY c.cliente_id DESC",
           countQuery = "SELECT COUNT(c) FROM Clientes c")
    Page<Clientes> findAllOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT c FROM Clientes c WHERE " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.cedula_ruc) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY c.cliente_id DESC",
           countQuery = "SELECT COUNT(c) FROM Clientes c WHERE " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.cedula_ruc) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Clientes> buscarPaginado(@Param("q") String termino, Pageable pageable);

    @Query("SELECT COUNT(c) > 0 FROM Clientes c WHERE c.cedula_ruc = :cedulaRuc")
    boolean existsByCedula_ruc(@Param("cedulaRuc") String cedulaRuc);

    @Query("SELECT c FROM Clientes c WHERE c.cedula_ruc = :cedulaRuc")
    Optional<Clientes> findByCedulaRuc(@Param("cedulaRuc") String cedulaRuc);

    Optional<Clientes> findByNombreIgnoreCase(String nombre);

    Optional<Clientes> findByTelefonos_Telefono(String telefono);

    @Query("SELECT DISTINCT c FROM Clientes c JOIN c.telefonos t WHERE t.telefono = :telefono")
    List<Clientes> buscarPorTelefonos_Telefono(@Param("telefono") String telefono);

    @Query("SELECT DISTINCT c FROM Clientes c JOIN c.telefonos t " +
            "WHERE (c.consentimiento_procesado IS NULL OR c.consentimiento_procesado = false) " +
            "AND t.telefono IS NOT NULL")
    List<Clientes> findClientesPendientesDeConsentimiento();

    @Query(value = "SELECT DISTINCT c FROM Clientes c JOIN c.telefonos t " +
            "WHERE (c.consentimiento_procesado IS NULL OR c.consentimiento_procesado = false) " +
            "AND t.telefono IS NOT NULL " +
            "ORDER BY c.cliente_id DESC",
           countQuery = "SELECT COUNT(DISTINCT c) FROM Clientes c JOIN c.telefonos t " +
            "WHERE (c.consentimiento_procesado IS NULL OR c.consentimiento_procesado = false) " +
            "AND t.telefono IS NOT NULL")
    Page<Clientes> findClientesPendientesDeConsentimientoPaginado(Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Clientes c JOIN c.telefonos t " +
            "WHERE (c.consentimiento_procesado IS NULL OR c.consentimiento_procesado = false) " +
            "AND t.telefono IS NOT NULL " +
            "AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.cedula_ruc) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY c.cliente_id DESC",
           countQuery = "SELECT COUNT(DISTINCT c) FROM Clientes c JOIN c.telefonos t " +
            "WHERE (c.consentimiento_procesado IS NULL OR c.consentimiento_procesado = false) " +
            "AND t.telefono IS NOT NULL " +
            "AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(c.cedula_ruc) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Clientes> buscarPendientesPaginado(@Param("q") String termino, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT c) FROM Clientes c JOIN c.telefonos t " +
            "WHERE (c.consentimiento_procesado IS NULL OR c.consentimiento_procesado = false) " +
            "AND t.telefono IS NOT NULL")
    long countClientesPendientesDeConsentimiento();
}
