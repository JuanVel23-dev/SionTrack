-- =================================================================
-- Script de migracion de seguridad e integridad - SionTrack
-- Ejecutar manualmente contra la base de datos PostgreSQL
-- =================================================================

-- ============================================
-- CHECK CONSTRAINTS - Prevenir datos invalidos
-- ============================================

-- Precios no negativos en productos
ALTER TABLE siontrack.productos
    ADD CONSTRAINT chk_precio_compra_positivo CHECK (precio_compra >= 0);

ALTER TABLE siontrack.productos
    ADD CONSTRAINT chk_precio_venta_positivo CHECK (precio_venta >= 0);

-- Cantidades no negativas en inventario
ALTER TABLE siontrack.inventario
    ADD CONSTRAINT chk_cantidad_disponible_positiva CHECK (cantidad_disponible >= 0);

ALTER TABLE siontrack.inventario
    ADD CONSTRAINT chk_stock_minimo_positivo CHECK (stock_minimo >= 0);

-- Cantidad positiva en detalle de servicio
ALTER TABLE siontrack.detalle_servicio
    ADD CONSTRAINT chk_detalle_cantidad_positiva CHECK (cantidad > 0);

ALTER TABLE siontrack.detalle_servicio
    ADD CONSTRAINT chk_precio_unitario_positivo CHECK (precio_unitario_congelado >= 0);

-- Monto positivo en pagos
ALTER TABLE siontrack.pagos
    ADD CONSTRAINT chk_monto_positivo CHECK (monto > 0);

-- Total no negativo en servicios
ALTER TABLE siontrack.servicios
    ADD CONSTRAINT chk_total_positivo CHECK (total >= 0);

-- Valores validos para tipo_cliente
ALTER TABLE siontrack.clientes
    ADD CONSTRAINT chk_tipo_cliente CHECK (tipo_cliente IN ('persona', 'empresa'));

-- Valores validos para metodo_pago
ALTER TABLE siontrack.pagos
    ADD CONSTRAINT chk_metodo_pago CHECK (metodo_pago IN ('efectivo', 'transferencia', 'tarjeta_debito', 'tarjeta_credito', 'nequi', 'daviplata'));

-- Valores validos para estado de notificaciones
ALTER TABLE siontrack.notificaciones
    ADD CONSTRAINT chk_estado_notificacion CHECK (estado IN ('pendiente', 'enviado', 'fallido', 'cancelado'));

-- ============================================
-- UNIQUE CONSTRAINTS - Prevenir duplicados
-- ============================================

-- Evitar correos duplicados por cliente
ALTER TABLE siontrack.cliente_correos
    ADD CONSTRAINT uq_cliente_correo UNIQUE (cliente_id, correo);

-- Evitar telefonos duplicados por cliente
ALTER TABLE siontrack.cliente_telefonos
    ADD CONSTRAINT uq_cliente_telefono UNIQUE (cliente_id, telefono);

-- ============================================
-- INDICES - Mejorar rendimiento de consultas
-- ============================================

-- Notificaciones se filtran frecuentemente por estado
CREATE INDEX IF NOT EXISTS idx_notificaciones_estado
    ON siontrack.notificaciones(estado);

-- Servicios se filtran por estado y fecha
CREATE INDEX IF NOT EXISTS idx_servicios_fecha
    ON siontrack.servicios(fecha_servicio);

-- Productos se buscan por nombre
CREATE INDEX IF NOT EXISTS idx_productos_nombre
    ON siontrack.productos(nombre);

-- Clientes se buscan por nombre
CREATE INDEX IF NOT EXISTS idx_clientes_nombre
    ON siontrack.clientes(nombre);
