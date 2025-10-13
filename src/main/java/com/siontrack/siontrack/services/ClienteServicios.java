package com.siontrack.siontrack.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.repository.ClienteRepository;

@Service
public class ClienteServicios {

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Clientes> obtenerListaClientes() {
        return clienteRepository.findAll();
    }

    public Optional<Clientes> getClienteById(int id) {
        return clienteRepository.findById(id);
    }

    public Clientes saveCliente(Clientes cliente) {
        return clienteRepository.save(cliente);
    }

    public void deleteCliente(int id) {
        clienteRepository.deleteById(id);
    }

}
