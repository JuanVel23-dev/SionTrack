package com.siontrack.siontrack.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.models.Cliente_Correos;
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

    public Clientes actualizarCliente(Clientes clienteExistente, Clientes detalleCliente) {

        String nuevaCedulaRuc = detalleCliente.getCedula_ruc();
        if (nuevaCedulaRuc != null && !nuevaCedulaRuc.equals(clienteExistente.getCedula_ruc())) {
            clienteExistente.setCedula_ruc(nuevaCedulaRuc);
        }

        clienteExistente.setNombre(detalleCliente.getNombre());
        clienteExistente.setTipo_cliente(detalleCliente.getTipo_cliente());
        clienteExistente.setFecha_modificacion(LocalDate.now());

        if (detalleCliente.getCorreos() != null) {
            clienteExistente.getCorreos().removeIf(correoExistente -> detalleCliente.getCorreos().stream()
                    .noneMatch(nuevoCorreo -> nuevoCorreo.getCorreo().equals(correoExistente.getCorreo())));

            // Agrega los nuevos correos que no existen en la base de datos
            for (Cliente_Correos nuevoCorreo : detalleCliente.getCorreos()) {
                if (clienteExistente.getCorreos().stream()
                        .noneMatch(correoExistente -> correoExistente.getCorreo().equals(nuevoCorreo.getCorreo()))) {

                    nuevoCorreo.setClientes(clienteExistente); // Vincula al padre
                    clienteExistente.getCorreos().add(nuevoCorreo);
                }
            }
        } else {
            // Si el JSON no envía correos, los elimina todos de la base de datos
            clienteExistente.getCorreos().clear();
        }

        return clienteRepository.save(clienteExistente);
    }

   
}
