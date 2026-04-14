package com.siontrack.siontrack.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.siontrack.siontrack.DTO.Request.ClienteRequestDTO;
import com.siontrack.siontrack.DTO.Request.ProductosRequestDTO;
import com.siontrack.siontrack.DTO.Response.DetalleServicioResponseDTO;
import com.siontrack.siontrack.DTO.Response.ProductosResponseDTO;
import com.siontrack.siontrack.models.Clientes;
import com.siontrack.siontrack.models.Detalle_Servicio;
import com.siontrack.siontrack.models.Productos;

/**
 * Configuración central de ModelMapper con estrategia STRICT.
 *
 * <p>La estrategia STRICT requiere que los nombres de getter/setter coincidan exactamente,
 * lo que evita mapeos incorrectos entre campos con nombres similares pero semánticamente
 * distintos. Como consecuencia, los campos que no coinciden por nombre deben mapearse
 * de forma explícita aquí o manualmente en los servicios.
 *
 * <p>Mappings registrados:
 * <ul>
 *   <li>{@link ProductosRequestDTO} → {@link Productos}: salta {@code producto_id} y
 *       mapea {@code codigo_producto} → {@code codigoProducto}.</li>
 *   <li>{@link Detalle_Servicio} → {@link DetalleServicioResponseDTO}: mapea el nombre
 *       del producto desde la relación {@code detalle → producto → nombre}.</li>
 *   <li>{@link Productos} → {@link ProductosResponseDTO}: mapea {@code inventario.cantidad_disponible}
 *       y {@code codigoProducto} → {@code codigo_producto}.</li>
 *   <li>{@link ClienteRequestDTO} → {@link Clientes}: salta las colecciones de contacto
 *       (teléfonos, correos, direcciones, vehículos) y {@code recibe_notificaciones} para
 *       que se gestionen manualmente en {@code ClienteServicios}.</li>
 * </ul>
 */
@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        modelMapper.createTypeMap(ProductosRequestDTO.class, Productos.class)
                .addMappings(mapper -> {
                    mapper.skip(Productos::setProducto_id);
                    mapper.map(src -> src.getCodigo_producto(), Productos::setCodigoProducto);
                });

        modelMapper.createTypeMap(Detalle_Servicio.class, DetalleServicioResponseDTO.class)
                .addMappings(mapper -> {
                    mapper.map(src -> src.getProducto().getNombre(), DetalleServicioResponseDTO::setNombre_producto);
                });

        modelMapper.createTypeMap(Productos.class, ProductosResponseDTO.class)
                .addMappings(mapper -> {
                    mapper.map(src -> src.getInventario().getCantidad_disponible(),
                            ProductosResponseDTO::setCantidad_disponible);
                    mapper.map(Productos::getCodigoProducto, ProductosResponseDTO::setCodigo_producto);
                });

        modelMapper.createTypeMap(ClienteRequestDTO.class, Clientes.class)
            .addMappings(mapper -> {
                mapper.skip(Clientes::setCliente_id);
                mapper.skip(Clientes::setTelefonos);
                mapper.skip(Clientes::setCorreos);
                mapper.skip(Clientes::setDirecciones);
                mapper.skip(Clientes::setVehiculos);
                // recibe_notificaciones se asigna manualmente en ClienteServicios
                // para evitar conflictos entre getRecibeNotificaciones() y setRecibe_notificaciones()
                mapper.skip(Clientes::setRecibe_notificaciones);
            });

        return modelMapper;
    }
}
