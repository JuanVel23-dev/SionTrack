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

        modelMapper.createTypeMap(ProductosRequestDTO.class, Productos.class)
                .addMappings(mapper -> {
                    mapper.skip(Productos::setProducto_id); 
                });

        modelMapper.createTypeMap(Detalle_Servicio.class, DetalleServicioResponseDTO.class)
                .addMappings(mapper -> {
                    mapper.map(src -> src.getProducto().getNombre(), DetalleServicioResponseDTO::setNombre_producto);

                });

        modelMapper.createTypeMap(Productos.class, ProductosResponseDTO.class)
                .addMappings(mapper -> {
                    mapper.map(src -> src.getInventario().getCantidad_disponible(),
                            ProductosResponseDTO::setCantidad_disponible);
                });

        return modelMapper;

    }

}
