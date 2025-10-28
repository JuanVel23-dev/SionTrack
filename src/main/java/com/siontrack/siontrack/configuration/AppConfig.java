package com.siontrack.siontrack.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.DetalleServicioResponseDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;
import com.siontrack.siontrack.models.Detalle_Servicio;
import com.siontrack.siontrack.models.Productos;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // --- Configuration to Skip ID during Update ---
        modelMapper.createTypeMap(ProductosRequestDTO.class, Productos.class)
                .addMappings(mapper -> {
                    // Tell ModelMapper to NEVER write to the 'producto_id' field of the Productos
                    // entity
                    mapper.skip(Productos::setProducto_id); // Assumes your setter is setProducto_id
                    // If your field is just 'id', use mapper.skip(Productos::setId);
                });

        // --- Custom Mapping Rule for DetalleServicio -> DTO ---
        modelMapper.createTypeMap(Detalle_Servicio.class, DetalleServicioResponseDTO.class)
                .addMappings(mapper -> {
                    // Map entity.getProducto().getNombre() to dto.productoNombre
                    mapper.map(src -> src.getProducto().getNombre(), DetalleServicioResponseDTO::setNombre_producto);

                });

        // In AppConfig.java -> modelMapper() bean configuration
        modelMapper.createTypeMap(Productos.class, ProductosResponseDTO.class)
                .addMappings(mapper -> {
                    // Explicitly map from the nested entity to the flat DTO field
                    mapper.map(src -> src.getInventario().getCantidad_disponible(),
                            ProductosResponseDTO::setCantidad_disponible);
                    // ... other mappings ...
                });

        return modelMapper;

    }

}
