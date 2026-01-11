package com.siontrack.siontrack.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.DTO.Request.DetalleServicioRequestDTO;
import com.siontrack.siontrack.DTO.Request.ServicioRequestDTO;
import com.siontrack.siontrack.DTO.Response.ServicioResponseDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Detalle_Servicio;
import com.siontrack.siontrack.models.Productos;
import com.siontrack.siontrack.models.Servicios;
import com.siontrack.siontrack.models.Vehiculos;
import com.siontrack.siontrack.repository.ClienteRepository;
import com.siontrack.siontrack.repository.ProductosRepository;
import com.siontrack.siontrack.repository.ServiciosRepository;
import com.siontrack.siontrack.repository.VehiculosRepository;

import jakarta.transaction.Transactional;

@Service
public class ServiciosService {

    @Autowired private ServiciosRepository serviciosRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VehiculosRepository vehiculoRepository;
    @Autowired private ProductosRepository productosRepository;
    @Autowired private ModelMapper modelMapper;

    @Transactional
    public ServicioResponseDTO crearServicio(ServicioRequestDTO dto) {

        // 1. Validar y Buscar Entidades Padre
        Clientes cliente = clienteRepository.findById(dto.getCliente_id())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + dto.getCliente_id()));

        Vehiculos vehiculo = vehiculoRepository.findById(dto.getVehiculo_id())
            .orElseThrow(() -> new RuntimeException("Vehículo no encontrado: " + dto.getVehiculo_id()));

        // 2. Crear Entidad Servicio Base
        Servicios servicio = new Servicios();
        servicio.setFecha_servicio(dto.getFecha_servicio());
        servicio.setKilometraje_servicio(dto.getKilometraje_servicio());
        servicio.setEstado(dto.getEstado() != null ? dto.getEstado() : "EN_PROCESO");
        servicio.setObservaciones(dto.getObservaciones());
        servicio.setCreado_en(LocalDateTime.now());
        
        // Asignar Relaciones
        servicio.setClientes(cliente);
        servicio.setVehiculos(vehiculo);

        // 3. Procesar Detalles y Calcular Total
        BigDecimal totalServicio = BigDecimal.ZERO;
        List<Detalle_Servicio> listaDetalles = new ArrayList<>();

        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            
            for (DetalleServicioRequestDTO detalleDto : dto.getDetalles()) {
                
                // Buscar el producto
                Productos producto = productosRepository.findById(detalleDto.getProducto_id())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleDto.getProducto_id()));

                // Crear entidad detalle
                Detalle_Servicio detalle = new Detalle_Servicio();
                detalle.setProducto(producto);
                detalle.setServicio(servicio); // Vincular al padre
                
                // Datos del detalle
                detalle.setCantidad(detalleDto.getCantidad());
                
                // PRECIO: Usar el enviado o el del producto actual (congelamiento de precio)
                BigDecimal precioFinal = (detalleDto.getPrecio_unitario_congelado() != null) 
                        ? detalleDto.getPrecio_unitario_congelado() 
                        : producto.getPrecio_venta();
                
                detalle.setPrecio_unitario_congelado(precioFinal);
                
                // Definir tipo (usando el Enum de tu entidad)
                // detalle.setTipo(Detalle_Servicio.tipoItem.valueOf(detalleDto.getTipoItem())); 

                // Lógica de Stock (Opcional: Descontar inventario)
                if (producto.getInventario() != null) {
                    int nuevaCantidad = producto.getInventario().getCantidad_disponible() - detalleDto.getCantidad().intValue();
                    if (nuevaCantidad < 0) throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
                    producto.getInventario().setCantidad_disponible(nuevaCantidad);
                }

                // Sumar al total (Precio * Cantidad)
                BigDecimal subtotal = precioFinal.multiply(detalleDto.getCantidad());
                totalServicio = totalServicio.add(subtotal);

                listaDetalles.add(detalle);
            }
        }

        servicio.setDetalles(listaDetalles);
        servicio.setTotal(totalServicio); // Asignar el total calculado

        // 4. Guardar (Cascade guardará los detalles)
        Servicios servicioGuardado = serviciosRepository.save(servicio);

        // 5. Retornar DTO
        return modelMapper.map(servicioGuardado, ServicioResponseDTO.class);
    }
    
    // Método para listar
    public List<ServicioResponseDTO> obtenerTodos() {
        return serviciosRepository.findAll().stream()
            .map(s -> modelMapper.map(s, ServicioResponseDTO.class))
            .collect(Collectors.toList());
    }
}
