-- =============================================================================
-- Seed: José Pérez García (44444444D) — perfil "despistado"
-- Corregido: campo validada_at en lugar de validada
--            estado CORREGIDA en lugar de INCOMPLETA (no existe en el enum)
-- =============================================================================

SET NAMES utf8mb4;
SET @today = CURDATE();

SELECT MIN(id) INTO @empresa_id FROM js5_js_empresas WHERE deleted_at IS NULL;
SELECT MIN(id) INTO @sector_id  FROM js5_js_sectores;
SELECT id INTO @t_incompleto FROM js5_js_alertas_tipos WHERE codigo = 'FICHAJE_INCOMPLETO' LIMIT 1;

SET @bcrypt = '$2y$12$k88seOl/DSGYlhpHKV.H6OVfOzCEr0QH5fhJSLYiYaSXK6IXbg6my';

-- Limpieza
DELETE FROM js5_js_users WHERE nif = '44444444D';

-- Usuario
INSERT INTO js5_js_users
    (uuid, nif, nombre, apellidos, email, password_hash, idioma, activo, created_at, updated_at)
VALUES (
    UUID(), '44444444D', 'José', 'Pérez García', 'jose@test.com',
    @bcrypt, 'es-ES', 1, NOW(), NOW()
);
SELECT LAST_INSERT_ID() INTO @jose_id;

-- Contrato
INSERT INTO js5_js_contratos
    (uuid, user_id, empresa_id, tipo, jornada_tipo, horas_semanales,
     fecha_inicio, vigente, created_at, updated_at)
VALUES (
    UUID(), @jose_id, @empresa_id,
    'INDEFINIDO', 'COMPLETA', 40,
    DATE_SUB(@today, INTERVAL 60 DAY),
    1, NOW(), NOW()
);

-- SEMANA -4: VALIDADAS (verde) — validada_at relleno
INSERT INTO js5_js_jornadas
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, validada_at, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id,
    DATE_SUB(@today, INTERVAL (28 + (4 - seq)) DAY),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (28 + (4 - seq)) DAY), '08:00:00'),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (28 + (4 - seq)) DAY), '17:00:00'),
    480, 60, 0,
    'VALIDADA', NOW(), 'SYNCED',
    NOW(), NOW()
FROM (SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d
WHERE DAYOFWEEK(DATE_SUB(@today, INTERVAL (28 + (4 - seq)) DAY)) NOT IN (1, 7);

-- SEMANA -3: CERRADAS (azul)
INSERT INTO js5_js_jornadas
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id,
    DATE_SUB(@today, INTERVAL (21 + (4 - seq)) DAY),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (21 + (4 - seq)) DAY), '08:30:00'),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (21 + (4 - seq)) DAY), '17:30:00'),
    480, 60, 0,
    'CERRADA', 'SYNCED',
    NOW(), NOW()
FROM (SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d
WHERE DAYOFWEEK(DATE_SUB(@today, INTERVAL (21 + (4 - seq)) DAY)) NOT IN (1, 7);

-- SEMANA -2: Lunes/Martes/Miércoles CORREGIDA (sin hora_fin = olvidó fichar salida)
-- Usamos CORREGIDA porque INCOMPLETA no existe en el enum
INSERT INTO js5_js_jornadas
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id,
    DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY), '09:00:00'),
    NULL,
    0, 0, 0,
    'CORREGIDA', 'SYNCED',
    NOW(), NOW()
FROM (SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2) d
WHERE DAYOFWEEK(DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY)) NOT IN (1, 7);

-- SEMANA -2: Jueves/Viernes CERRADAS
INSERT INTO js5_js_jornadas
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id,
    DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY), '09:00:00'),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY), '18:00:00'),
    480, 60, 0,
    'CERRADA', 'SYNCED',
    NOW(), NOW()
FROM (SELECT 3 AS seq UNION SELECT 4) d
WHERE DAYOFWEEK(DATE_SUB(@today, INTERVAL (14 + (4 - seq)) DAY)) NOT IN (1, 7);

-- SEMANA -1: CERRADAS (azul)
INSERT INTO js5_js_jornadas
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id,
    DATE_SUB(@today, INTERVAL (7 + (4 - seq)) DAY),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (7 + (4 - seq)) DAY), '08:00:00'),
    TIMESTAMP(DATE_SUB(@today, INTERVAL (7 + (4 - seq)) DAY), '17:00:00'),
    480, 60, 0,
    'CERRADA', 'SYNCED',
    NOW(), NOW()
FROM (SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d
WHERE DAYOFWEEK(DATE_SUB(@today, INTERVAL (7 + (4 - seq)) DAY)) NOT IN (1, 7);

-- HOY: ABIERTA (amarillo)
INSERT INTO js5_js_jornadas
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, sync_status, created_at, updated_at)
VALUES (
    UUID(), @jose_id, @today,
    TIMESTAMP(@today, '08:00:00'),
    NULL, 0, 0, 0,
    'ABIERTA', 'SYNCED',
    NOW(), NOW()
);

-- Fichajes ENTRADA para todas las jornadas
INSERT INTO js5_js_fichajes
    (uuid, user_id, jornada_id, tipo, timestamp_evento,
     latitud, longitud, metodo, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id, j.id, 'ENTRADA', j.hora_inicio,
    40.4168, -3.7038, 'MANUAL', 'SYNCED', NOW(), NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @jose_id AND j.hora_inicio IS NOT NULL;

-- Fichajes SALIDA solo para jornadas con hora_fin
INSERT INTO js5_js_fichajes
    (uuid, user_id, jornada_id, tipo, timestamp_evento,
     latitud, longitud, metodo, sync_status, created_at, updated_at)
SELECT
    UUID(), @jose_id, j.id, 'SALIDA', j.hora_fin,
    40.4168, -3.7038, 'MANUAL', 'SYNCED', NOW(), NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @jose_id AND j.hora_fin IS NOT NULL;

-- Verificación
SELECT
    '44444444D'                             AS nif,
    'Pérez, José'                           AS trabajador,
    COUNT(*)                                AS total_jornadas,
    SUM(estado = 'VALIDADA')                AS validadas,
    SUM(estado = 'CERRADA')                 AS cerradas,
    SUM(estado = 'CORREGIDA')               AS corregidas,
    SUM(estado = 'ABIERTA')                 AS abiertas
FROM js5_js_jornadas
WHERE user_id = @jose_id;