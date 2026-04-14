# SionTrack

Sistema de gestión de clientes, servicios, inventario y notificaciones para **Grupo Sion S.A.S**.

---

## Tabla de contenidos

1. [Descripción general](#descripción-general)
2. [Stack tecnológico](#stack-tecnológico)
3. [Arquitectura](#arquitectura)
4. [Módulos principales](#módulos-principales)
5. [Requisitos previos](#requisitos-previos)
6. [Ejecución local](#ejecución-local)
7. [Variables de entorno](#variables-de-entorno)
8. [API REST — Swagger UI](#api-rest--swagger-ui)
9. [Importación masiva](#importación-masiva)
10. [Notificaciones por WhatsApp](#notificaciones-por-whatsapp)
11. [Generación de reportes PDF](#generación-de-reportes-pdf)
12. [Despliegue en Google Cloud Platform](#despliegue-en-google-cloud-platform)

---

## Descripción general

SionTrack es una aplicación web full-stack que permite a Grupo Sion S.A.S:

- Registrar y consultar clientes, vehículos, productos, proveedores y servicios.
- Controlar el inventario con alertas de stock mínimo.
- Enviar notificaciones de consentimiento, recordatorios de mantenimiento y promociones vía **WhatsApp Business API**.
- Importar datos masivos desde archivos **Excel (.xlsx / .xls)** o **CSV**.
- Generar reportes en **PDF** por rango de fechas.

La interfaz de usuario está construida con **Thymeleaf + Bootstrap**, y la lógica de negocio se expone también como una **API REST JSON** documentada con OpenAPI 3.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.7 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL (Cloud SQL en GCP) |
| Plantillas UI | Thymeleaf 3 + thymeleaf-layout-dialect |
| Seguridad | Spring Security 6 |
| Mapeo de objetos | ModelMapper 3.1.1 (estrategia STRICT) |
| PDF | iText 8.0.5 |
| Excel / CSV | Apache POI 5.2.5 |
| API REST docs | Springdoc OpenAPI 2.6.0 (Swagger UI) |
| WhatsApp | Meta WhatsApp Business Cloud API v22.0 |
| Infraestructura | Google Cloud Platform — Cloud SQL + Cloud Run |
| Build | Maven 3 |

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        Navegador / Cliente REST              │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP
┌───────────────────────────▼─────────────────────────────────┐
│                     Controllers (capa web)                   │
│  ├── *ViewController  →  Thymeleaf (vistas HTML)            │
│  └── *RestController  →  JSON (API REST)                    │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                       Services (lógica de negocio)           │
│  ClienteServicios · ProductosServicios · ServiciosService    │
│  ProveedoresService · ImportacionService · ReporteService    │
│  NotificacionesService · RecordatorioService                 │
│  WhatsAppService · WebhookService · UsuariosService          │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│              Repositories (Spring Data JPA)                  │
│  ClienteRepository · ProductosRepository · ...              │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│               Base de datos PostgreSQL                       │
│              (schema: siontrack)                            │
└─────────────────────────────────────────────────────────────┘
```

### Capas de configuración

| Clase | Propósito |
|---|---|
| `AppConfig` | Bean de `ModelMapper` con estrategia STRICT y TypeMaps explícitos |
| `SecurityConfig` | Spring Security: formulario de login, CSRF selectivo, rutas públicas |
| `OpenApiConfig` | Metadatos del spec OpenAPI (título, versión, contacto, tags) |
| `WhatsAppConfig` | Propiedades `@ConfigurationProperties` para la API de Meta |
| `GlobalExceptionHandler` | `@ControllerAdvice` que centraliza el manejo de excepciones |

---

## Módulos principales

### Clientes y vehículos
Gestión CRUD de clientes con colecciones de teléfonos, correos, direcciones y vehículos. Al crear un cliente se envía automáticamente una solicitud de consentimiento por WhatsApp si tiene teléfono registrado.

### Productos e inventario
Catálogo de productos con control de stock. Cada producto tiene un registro `Inventario` asociado. Las alertas de stock bajo se calculan con un puntaje de prioridad compuesto (urgencia × popularidad).

### Servicios
Registro de servicios con detalles de productos utilizados. El precio unitario se congela en el momento del registro (precio histórico). El consumo de stock se descuenta con redondeo `CEILING` (1.5 unidades → descuenta 2).

### Proveedores
CRUD de proveedores. Asociados a productos para rastrear compras.

### Importación masiva
Carga de datos desde Excel o CSV. Ver [Importación masiva](#importación-masiva) para el formato esperado de cada entidad.

### Notificaciones WhatsApp
Tres tipos de notificaciones: consentimiento, recordatorio de mantenimiento y promociones. Los recordatorios se programan automáticamente cuando un servicio supera los 8 meses sin actividad. Ver [Notificaciones por WhatsApp](#notificaciones-por-whatsapp).

### Reportes PDF
Descarga bajo demanda de reportes de clientes, productos, proveedores, servicios, notificaciones y productos populares. Ver [Generación de reportes PDF](#generación-de-reportes-pdf).

---

## Requisitos previos

- **JDK 21** (testeado con Eclipse Temurin 21)
- **Maven 3.9+**
- **PostgreSQL 14+** con la base de datos y schema `siontrack` creados
- (Opcional) **Cloud SQL Auth Proxy** para conectar con la instancia en GCP desde local

---

## Ejecución local

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd siontrack
```

### 2. Crear la base de datos

```sql
CREATE DATABASE "SIONTRACKDB";
\c SIONTRACKDB
CREATE SCHEMA siontrack;
```

### 3. Configurar el perfil local

El archivo `src/main/resources/application-local.properties` ya contiene valores de ejemplo para desarrollo local. Ajusta las credenciales de PostgreSQL según tu entorno:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/SIONTRACKDB?currentSchema=siontrack
spring.datasource.username=<tu_usuario>
spring.datasource.password=<tu_contraseña>
```

> **Importante:** `spring.cloud.gcp.sql.enabled=false` desactiva Cloud SQL Auth Proxy en local.

### 4. Ejecutar con el perfil local

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

La aplicación queda disponible en `http://localhost:8081`.

### 5. Credenciales de acceso inicial

El sistema usa Spring Security con autenticación por formulario. Las credenciales se gestionan en la tabla `siontrack.usuarios`. Asegúrate de tener al menos un usuario registrado con contraseña encriptada en BCrypt antes de iniciar sesión.

---

## Variables de entorno

En producción (Cloud Run), la aplicación espera las siguientes variables de entorno:

| Variable | Descripción |
|---|---|
| `PORT` | Puerto del servidor (default: `8081`) |
| `DATABASE_USERNAME` | Usuario de PostgreSQL |
| `DATABASE_PASSWORD` | Contraseña de PostgreSQL |
| `INSTANCE_CONNECTION_NAME` | Nombre de conexión de Cloud SQL (ej. `proyecto:region:instancia`) |
| `DATABASE_NAME` | Nombre de la base de datos (ej. `SIONTRACKDB`) |
| `DATABASE_SCHEMA` | Schema de la base de datos (ej. `siontrack`) |
| `WHATSAPP_TOKEN_ACCESO` | Token de acceso permanente de la app de Meta |
| `WHATSAPP_ID_NUMERO` | ID del número de teléfono en Meta Business |
| `WHATSAPP_WEBHOOK_TOKEN` | Token de verificación del webhook (configurado en Meta) |

---

## API REST — Swagger UI

Una vez levantada la aplicación, la documentación interactiva de la API está disponible en:

```
http://localhost:8081/swagger-ui/index.html
```

El spec OpenAPI en formato JSON se expone en:

```
http://localhost:8081/v3/api-docs
```

### Grupos de endpoints

| Tag | Prefijo base | Descripción |
|---|---|---|
| Clientes | `/api/clientes` | CRUD y búsqueda de clientes |
| Vehículos | `/api/vehiculos` | CRUD de vehículos por cliente |
| Productos | `/api/productos` | CRUD, búsqueda y alertas de stock |
| Proveedores | `/api/proveedores` | CRUD de proveedores |
| Servicios | `/api/servicios` | CRUD de servicios |
| Alertas de Stock | `/api/alertas` | Alertas paginadas y sin paginar |
| Notificaciones | `/api/notificaciones` | Consentimientos pendientes y masivo |
| Importación | `/api/importar` | Carga masiva desde Excel/CSV |
| Reportes | `/api/reportes` | Descarga de PDFs |
| Webhook | `/api/webhook` | Verificación y recepción de eventos Meta |

> Los endpoints de Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) están permitidos sin autenticación para facilitar la exploración. El webhook (`/api/webhook`) también es público porque Meta no puede incluir cookies de sesión.

---

## Importación masiva

Todos los endpoints de importación aceptan un archivo via `multipart/form-data` con el parámetro `archivo` (`.xlsx`, `.xls` o `.csv`). La primera fila debe contener los encabezados de columna.

### Clientes — `POST /api/importar/clientes`

| Columna | Obligatoria | Descripción |
|---|---|---|
| `nombre` | Sí | Nombre completo del cliente |
| `cedula_ruc` | No | Cédula o RUC |
| `tipo_cliente` | No | Tipo de cliente |
| `telefono` | No | Número de teléfono |
| `correo` | No | Correo electrónico |
| `direccion` | No | Dirección |
| `placa` | No | Placa del vehículo |
| `kilometraje_actual` | No | Kilometraje actual del vehículo |

Si el cliente ya existe (por cédula o nombre) se actualiza; de lo contrario se crea.

### Productos — `POST /api/importar/productos`

| Columna | Obligatoria | Descripción |
|---|---|---|
| `nombre` | Sí | Nombre del producto |
| `proveedor` | Sí | Nombre del proveedor (debe existir) |
| `codigo_producto` | No | Código SKU |
| `categoria` | No | Categoría del producto |
| `precio_compra` | No | Precio de compra |
| `precio_venta` | No | Precio de venta |
| `fecha_compra` | No | Fecha de compra (ISO: `yyyy-MM-dd`) |
| `cantidad_disponible` | No | Unidades en stock |
| `stock_minimo` | No | Umbral de alerta de stock bajo |

Si el producto ya existe se actualiza (upsert por nombre).

### Proveedores — `POST /api/importar/proveedores`

| Columna | Obligatoria | Descripción |
|---|---|---|
| `nombre` | Sí | Nombre del proveedor |
| `telefono` | No | Teléfono de contacto |
| `email` | No | Correo electrónico |
| `direccion` | No | Dirección |
| `nombre_contacto` | No | Nombre de la persona de contacto |

Los proveedores ya existentes se rechazan (no hay upsert).

### Servicios — `POST /api/importar/servicios`

| Columna | Obligatoria | Descripción |
|---|---|---|
| `cedula_ruc` o `cliente` | Sí | Identificador del cliente |
| `fecha_servicio` | Sí | Fecha del servicio (ISO o `dd/MM/yyyy`) |
| `placa` | No | Placa del vehículo |
| `tipo_servicio` | No | Tipo de servicio |
| `kilometraje_servicio` | No | Kilometraje al momento del servicio |
| `observaciones` | No | Observaciones del servicio |
| `codigo_producto` o `nombre_producto` | No | Producto utilizado en el detalle |
| `cantidad` | No | Cantidad utilizada |
| `precio_unitario` | No | Precio unitario (se congela al importar) |

Varias filas con la misma combinación `(cliente + vehículo + fecha + tipo)` se agrupan automáticamente en un único servicio con múltiples detalles.

### Stock — `POST /api/importar/stock`

| Columna | Obligatoria | Descripción |
|---|---|---|
| `producto_id` | Sí | ID del producto |
| `cantidad` | Sí | Cantidad a registrar |
| `operacion` | No | `AGREGAR` suma al stock actual; cualquier otro valor establece el stock directamente |

---

## Notificaciones por WhatsApp

SionTrack se integra con la **Meta WhatsApp Business Cloud API** para enviar tres tipos de mensajes usando plantillas aprobadas:

| Tipo | Plantilla | Descripción |
|---|---|---|
| `CONSENTIMIENTO` | `solicitud_consentimiento` | Solicita permiso al cliente para recibir notificaciones |
| `RECORDATORIO` | `recordatorio_mantenimiento` | Recuerda al cliente que debe llevar su vehículo a mantenimiento |
| `PROMOCION` | `promociones` | Envío masivo de promociones a clientes con consentimiento activo |

### Flujo de consentimiento

1. Al crear un cliente con teléfono, se envía automáticamente la plantilla de consentimiento.
2. El cliente responde "SÍ" o "NO" al número de WhatsApp.
3. Meta reenvía la respuesta al endpoint `POST /api/webhook`.
4. `WebhookService` clasifica la respuesta y actualiza el campo `recibe_notificaciones` en la entidad `Clientes`.

### Recordatorios automáticos

`RecordatorioService` se ejecuta diariamente (vía `@Scheduled`) y:
1. Detecta clientes cuyo último servicio fue hace más de 8 meses.
2. Envía el primer recordatorio si el servicio lleva entre 7 y 8 meses.
3. Envía un segundo recordatorio si el primer recordatorio fue hace más de 1 mes.

### Verificación del webhook

Al registrar la URL del webhook en Meta, el sistema responde al `GET /api/webhook` con el `hub.challenge` si el token coincide con `WHATSAPP_WEBHOOK_TOKEN`.

---

## Generación de reportes PDF

Todos los reportes se generan con **iText 8** y se devuelven como descarga (`Content-Disposition: attachment`). El nombre del archivo sigue el patrón `SionTrack_{tipo}_{yyyyMMdd}.pdf`.

| Endpoint | Parámetros | Descripción |
|---|---|---|
| `GET /api/reportes/clientes` | `fechaInicio`, `fechaFin` | Clientes registrados en el rango |
| `GET /api/reportes/productos` | `fechaInicio`, `fechaFin` | Productos comprados en el rango (stock bajo en rojo) |
| `GET /api/reportes/proveedores` | `fechaInicio`, `fechaFin` | Proveedores con compras en el rango |
| `GET /api/reportes/servicios` | `fechaInicio`, `fechaFin` | Servicios prestados en el rango |
| `GET /api/reportes/notificaciones` | `fechaInicio`, `fechaFin` | Recordatorios y promociones del rango |
| `GET /api/reportes/productos-populares` | `periodo` | Top 50 productos más vendidos |

Los valores válidos para `periodo` en el reporte de productos populares son: `semana`, `mes`, `trimestre`, `anio`, `general`.

Las fechas deben enviarse en formato ISO: `yyyy-MM-dd` (ej. `?fechaInicio=2025-01-01&fechaFin=2025-12-31`).

---

## Despliegue en Google Cloud Platform

La aplicación está configurada para ejecutarse en **Cloud Run** con **Cloud SQL (PostgreSQL)** como base de datos.

### Conexión a Cloud SQL

En producción se usa el conector de Google Cloud SQL para Spring Boot (`spring-cloud-gcp-starter-sql-postgresql`). La conexión se establece automáticamente a través del Cloud SQL Auth Proxy que se inyecta como sidecar en Cloud Run.

Configuración relevante en `application.properties`:

```properties
spring.cloud.gcp.sql.instance-connection-name=${INSTANCE_CONNECTION_NAME}
spring.cloud.gcp.sql.database-name=${DATABASE_NAME}
spring.jpa.properties.hibernate.default.schema=${DATABASE_SCHEMA}
spring.jpa.hibernate.ddl-auto=validate
```

> `ddl-auto=validate` garantiza que Hibernate no modifique el esquema en producción. Los cambios de esquema deben aplicarse manualmente con scripts SQL.

### Monitoreo

Los endpoints de Actuator expuestos son:

```
/actuator/health
/actuator/metrics
/actuator/info
```

Configurados en `application.properties`:

```properties
management.endpoints.web.exposure.include=health,metrics,info
```
