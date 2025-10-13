package com.siontrack.siontrack.models;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "cliente_correos")
public class Cliente_Correos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    private int correo_id;

    @Getter @Setter
    private String correo; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id" , nullable = false)
    @JsonBackReference
    @Getter @Setter
    private Clientes clientes;

}
