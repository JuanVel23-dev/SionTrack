package com.siontrack.siontrack.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configura la especificación OpenAPI 3.0 expuesta en {@code /v3/api-docs}.
 * La UI interactiva de Swagger está disponible en {@code /swagger-ui/index.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sionTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SionTrack API")
                        .description("""
                                API REST de SionTrack — sistema de gestión para Grupo Sion S.A.S.

                                Cubre la gestión de clientes, vehículos, productos, proveedores, servicios,
                                notificaciones por WhatsApp, importación masiva desde Excel/CSV y generación
                                de reportes en PDF.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Grupo Sion S.A.S")
                                .email("info@gruposion.com")))
                .tags(List.of(
                        new Tag().name("Clientes").description("Gestión de clientes y sus datos de contacto"),
                        new Tag().name("Vehículos").description("Vehículos asociados a clientes"),
                        new Tag().name("Productos").description("Catálogo de productos e inventario"),
                        new Tag().name("Proveedores").description("Proveedores de productos"),
                        new Tag().name("Servicios").description("Registro de servicios prestados"),
                        new Tag().name("Alertas de Stock").description("Alertas de productos con stock bajo"),
                        new Tag().name("Notificaciones").description("Consentimiento y envío de mensajes por WhatsApp"),
                        new Tag().name("Importación").description("Carga masiva de datos desde archivos Excel o CSV"),
                        new Tag().name("Reportes").description("Descarga de reportes en PDF"),
                        new Tag().name("Webhook").description("Recepción de mensajes entrantes de la API de WhatsApp (Meta)")
                ));
    }
}
