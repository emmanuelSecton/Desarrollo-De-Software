-- Restricción única: el número de factura no se repite dentro de un punto de venta.
-- Corresponde a @UniqueConstraint "uk_factura_venta_pto_numero" en FacturaVenta.java.
-- Hace falta aplicarla a mano porque spring.jpa.hibernate.ddl-auto=none.

-- 1) Verificar que no haya duplicados. Si esta consulta devuelve filas, corregirlas
--    ANTES del paso 2 (si no, el ALTER falla).
SELECT punto_venta_id, numero, COUNT(*) AS cantidad, GROUP_CONCAT(id) AS ids
FROM factura_venta
GROUP BY punto_venta_id, numero
HAVING COUNT(*) > 1;

-- 2) Crear la restricción.
ALTER TABLE factura_venta
    ADD CONSTRAINT uk_factura_venta_pto_numero UNIQUE (punto_venta_id, numero);
