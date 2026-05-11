-- =============================================================================
--  Seed pruebas Capítulo 5 — datos de validación de alertas y burnout
--  Prefijo: js5_js_
--  Trabajadores: Pedro Martínez (11111111A), Laura Sánchez (22222222B),
--                Miguel Torres (33333333C)
--  Credenciales: password "test1234" (bcrypt cost 12)
--
--  Idempotente: DELETE inicial por NIF cascadea jornadas/fichajes/pausas/
--  alertas/burnout/documentos via FK ON DELETE CASCADE.
--
--  Códigos de alerta (catálogo js5_js_alertas_tipos):
--    JORNADA_EXCEDIDA      → "jornada diaria excesiva"        (Pedro)
--    PAUSA_OMITIDA         → "pausa diaria no realizada"      (Pedro)
--    SIN_DESCANSO_SEMANAL  → "descanso semanal no respetado"  (Laura)
--    HORAS_EXTRA_LIMITE    → "límite anual horas extras"      (Miguel)
--    RIESGO_BURNOUT        → "riesgo burnout detectado"       (Miguel/Laura)
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 0. Limpieza idempotente
-- -----------------------------------------------------------------------------
DELETE FROM js5_js_users WHERE nif IN ('11111111A', '22222222B', '33333333C');

-- -----------------------------------------------------------------------------
-- 1. Variables base — empresa, sector, hash bcrypt e IDs de tipos de alerta
-- -----------------------------------------------------------------------------
SET @empresa_id := (SELECT MIN(id) FROM js5_js_empresas);
SET @sector_id  := (SELECT MIN(id) FROM js5_js_sectores);
SET @bcrypt     := '$2y$12$k88seOXqpqLK1nj8WDxRUeCMZQaO3RNM25B4ChYIAnos5268nNQiy';
SET @today      := CURDATE();

SET @t_jornada  := (SELECT id FROM js5_js_alertas_tipos WHERE codigo = 'JORNADA_EXCEDIDA');
SET @t_pausa    := (SELECT id FROM js5_js_alertas_tipos WHERE codigo = 'PAUSA_OMITIDA');
SET @t_semanal  := (SELECT id FROM js5_js_alertas_tipos WHERE codigo = 'SIN_DESCANSO_SEMANAL');
SET @t_extras   := (SELECT id FROM js5_js_alertas_tipos WHERE codigo = 'HORAS_EXTRA_LIMITE');
SET @t_burnout  := (SELECT id FROM js5_js_alertas_tipos WHERE codigo = 'RIESGO_BURNOUT');

-- -----------------------------------------------------------------------------
-- 2. Trabajadores
-- -----------------------------------------------------------------------------
INSERT INTO js5_js_users
  (uuid, nif, nombre, apellidos, email, idioma, password_hash, activo, created_at)
VALUES
  (UUID(), '11111111A', 'Pedro',  'Martínez', 'pedro.martinez@test.local',  'es-ES', @bcrypt, 1, NOW()),
  (UUID(), '22222222B', 'Laura',  'Sánchez',  'laura.sanchez@test.local',   'es-ES', @bcrypt, 1, NOW()),
  (UUID(), '33333333C', 'Miguel', 'Torres',   'miguel.torres@test.local',   'es-ES', @bcrypt, 1, NOW());

SET @pedro_id  := (SELECT id FROM js5_js_users WHERE nif = '11111111A');
SET @laura_id  := (SELECT id FROM js5_js_users WHERE nif = '22222222B');
SET @miguel_id := (SELECT id FROM js5_js_users WHERE nif = '33333333C');

-- -----------------------------------------------------------------------------
-- 3. Contratos vigentes (jornada completa 40 h/semana, indefinidos)
-- -----------------------------------------------------------------------------
INSERT INTO js5_js_contratos
  (uuid, user_id, empresa_id, tipo, jornada_tipo, horas_semanales, fecha_inicio, vigente, created_at)
VALUES
  (UUID(), @pedro_id,  @empresa_id, 'INDEFINIDO', 'COMPLETA', 40.00, DATE_SUB(@today, INTERVAL 365 DAY), 1, NOW()),
  (UUID(), @laura_id,  @empresa_id, 'INDEFINIDO', 'COMPLETA', 40.00, DATE_SUB(@today, INTERVAL 365 DAY), 1, NOW()),
  (UUID(), @miguel_id, @empresa_id, 'INDEFINIDO', 'COMPLETA', 40.00, DATE_SUB(@today, INTERVAL 365 DAY), 1, NOW());

SET @pedro_contrato  := (SELECT id FROM js5_js_contratos WHERE user_id = @pedro_id  AND vigente = 1);
SET @laura_contrato  := (SELECT id FROM js5_js_contratos WHERE user_id = @laura_id  AND vigente = 1);
SET @miguel_contrato := (SELECT id FROM js5_js_contratos WHERE user_id = @miguel_id AND vigente = 1);

-- =============================================================================
-- 4. JORNADAS — 30 días con timestamps reales relativos a CURDATE()
-- =============================================================================

-- 4.1 Pedro: 22 días laborables (lun-vie), 10.5 h seguidas SIN PAUSA
--      08:00 → 18:30  ⇒ jornada > 9 h y > 6 h sin pausa
INSERT INTO js5_js_jornadas
  (uuid, user_id, contrato_id, fecha, hora_inicio, hora_fin,
   minutos_trabajados, minutos_pausa, minutos_extra, estado, sync_status, created_at)
SELECT UUID(), @pedro_id, @pedro_contrato,
       DATE_SUB(@today, INTERVAL d DAY),
       TIMESTAMP(DATE_SUB(@today, INTERVAL d DAY), '08:00:00'),
       TIMESTAMP(DATE_SUB(@today, INTERVAL d DAY), '18:30:00'),
       630, 0, 90, 'CERRADA', 'SYNCED', NOW()
FROM (
  SELECT 29 AS d UNION SELECT 28 UNION SELECT 26 UNION SELECT 25 UNION SELECT 24
  UNION SELECT 22 UNION SELECT 21 UNION SELECT 19 UNION SELECT 18 UNION SELECT 17
  UNION SELECT 15 UNION SELECT 14 UNION SELECT 12 UNION SELECT 11 UNION SELECT 10
  UNION SELECT 8  UNION SELECT 7  UNION SELECT 5  UNION SELECT 4  UNION SELECT 3
  UNION SELECT 1  UNION SELECT 0
) AS dias_pedro;

-- 4.2 Laura: 30 días SEGUIDOS (sin descanso semanal). Jornada 8 h normal con pausa.
--      09:00 → 18:00 con 60 min de pausa de comida
INSERT INTO js5_js_jornadas
  (uuid, user_id, contrato_id, fecha, hora_inicio, hora_fin,
   minutos_trabajados, minutos_pausa, minutos_extra, estado, sync_status, created_at)
SELECT UUID(), @laura_id, @laura_contrato,
       DATE_SUB(@today, INTERVAL d DAY),
       TIMESTAMP(DATE_SUB(@today, INTERVAL d DAY), '09:00:00'),
       TIMESTAMP(DATE_SUB(@today, INTERVAL d DAY), '18:00:00'),
       480, 60, 0, 'CERRADA', 'SYNCED', NOW()
FROM (
  SELECT 29 AS d UNION SELECT 28 UNION SELECT 27 UNION SELECT 26 UNION SELECT 25
  UNION SELECT 24 UNION SELECT 23 UNION SELECT 22 UNION SELECT 21 UNION SELECT 20
  UNION SELECT 19 UNION SELECT 18 UNION SELECT 17 UNION SELECT 16 UNION SELECT 15
  UNION SELECT 14 UNION SELECT 13 UNION SELECT 12 UNION SELECT 11 UNION SELECT 10
  UNION SELECT 9  UNION SELECT 8  UNION SELECT 7  UNION SELECT 6  UNION SELECT 5
  UNION SELECT 4  UNION SELECT 3  UNION SELECT 2  UNION SELECT 1  UNION SELECT 0
) AS dias_laura;

-- 4.3 Miguel: 26 días (6 días/semana, salta 4 "domingos"). Jornadas de 11 h efectivas.
--      08:00 → 20:00 con 60 min de pausa  ⇒ 660 min trabajados, 180 min extra/día
INSERT INTO js5_js_jornadas
  (uuid, user_id, contrato_id, fecha, hora_inicio, hora_fin,
   minutos_trabajados, minutos_pausa, minutos_extra, estado, sync_status, created_at)
SELECT UUID(), @miguel_id, @miguel_contrato,
       DATE_SUB(@today, INTERVAL d DAY),
       TIMESTAMP(DATE_SUB(@today, INTERVAL d DAY), '08:00:00'),
       TIMESTAMP(DATE_SUB(@today, INTERVAL d DAY), '20:00:00'),
       660, 60, 180, 'CERRADA', 'SYNCED', NOW()
FROM (
  SELECT 29 AS d UNION SELECT 28 UNION SELECT 27 UNION SELECT 25 UNION SELECT 24
  UNION SELECT 23 UNION SELECT 22 UNION SELECT 21 UNION SELECT 20 UNION SELECT 18
  UNION SELECT 17 UNION SELECT 16 UNION SELECT 15 UNION SELECT 14 UNION SELECT 13
  UNION SELECT 11 UNION SELECT 10 UNION SELECT 9  UNION SELECT 8  UNION SELECT 7
  UNION SELECT 6  UNION SELECT 4  UNION SELECT 3  UNION SELECT 2  UNION SELECT 1
  UNION SELECT 0
) AS dias_miguel;

-- =============================================================================
-- 5. FICHAJES — 1 ENTRADA + 1 SALIDA por jornada, derivados de hora_inicio/fin
-- =============================================================================

-- 5.1 ENTRADAS
INSERT INTO js5_js_fichajes
  (uuid, jornada_id, user_id, empresa_id, tipo, timestamp_evento,
   metodo, sync_status, created_at)
SELECT UUID(), j.id, j.user_id, @empresa_id, 'ENTRADA', j.hora_inicio,
       'MANUAL', 'SYNCED', NOW()
FROM js5_js_jornadas j
WHERE j.user_id IN (@pedro_id, @laura_id, @miguel_id)
  AND j.hora_inicio IS NOT NULL;

-- 5.2 SALIDAS
INSERT INTO js5_js_fichajes
  (uuid, jornada_id, user_id, empresa_id, tipo, timestamp_evento,
   metodo, sync_status, created_at)
SELECT UUID(), j.id, j.user_id, @empresa_id, 'SALIDA', j.hora_fin,
       'MANUAL', 'SYNCED', NOW()
FROM js5_js_jornadas j
WHERE j.user_id IN (@pedro_id, @laura_id, @miguel_id)
  AND j.hora_fin IS NOT NULL;

-- =============================================================================
-- 6. PAUSAS — Pedro NINGUNA (PAUSA_OMITIDA), Laura y Miguel pausa de comida 60 min
-- =============================================================================
INSERT INTO js5_js_pausas
  (uuid, jornada_id, tipo, inicio, fin, minutos, computa_jornada, sync_status, created_at)
SELECT UUID(), j.id, 'COMIDA',
       TIMESTAMP(j.fecha, '13:00:00'),
       TIMESTAMP(j.fecha, '14:00:00'),
       60, 0, 'SYNCED', NOW()
FROM js5_js_jornadas j
WHERE j.user_id IN (@laura_id, @miguel_id);

-- =============================================================================
-- 7. HORAS EXTRA — Miguel acumula 26 × 180 min = 4680 min ≈ 78 h en el mes
-- =============================================================================
INSERT INTO js5_js_horas_extra
  (uuid, jornada_id, user_id, minutos, tipo, compensacion, estado,
   aceptada_trabajador, created_at)
SELECT UUID(), j.id, j.user_id, j.minutos_extra,
       'ORDINARIA', 'PENDIENTE', 'PENDIENTE', 0, NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @miguel_id
  AND j.minutos_extra > 0;

-- También las horas extra diarias de Pedro (90 min × 22 días = 1980 min ≈ 33 h)
INSERT INTO js5_js_horas_extra
  (uuid, jornada_id, user_id, minutos, tipo, compensacion, estado,
   aceptada_trabajador, created_at)
SELECT UUID(), j.id, j.user_id, j.minutos_extra,
       'ORDINARIA', 'PENDIENTE', 'PENDIENTE', 0, NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @pedro_id
  AND j.minutos_extra > 0;

-- =============================================================================
-- 8. ALERTAS pre-generadas (las que el motor evaluador habría producido)
-- =============================================================================

-- 8.1 Pedro — JORNADA_EXCEDIDA por cada jornada > 540 min
INSERT INTO js5_js_alertas
  (uuid, user_id, tipo_id, jornada_id, fecha_evento, mensaje,
   valor_detectado, leida, notificada_push, created_at)
SELECT UUID(), j.user_id, @t_jornada, j.id,
       j.hora_fin,
       CONCAT('Jornada de ', ROUND(j.minutos_trabajados/60, 1),
              ' h supera el límite legal de 9 h (Art. 34.3 ET).'),
       CONCAT(j.minutos_trabajados, ' min'),
       0, 1, NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @pedro_id
  AND j.minutos_trabajados > 540;

-- 8.2 Pedro — PAUSA_OMITIDA por jornada > 360 min sin pausas
INSERT INTO js5_js_alertas
  (uuid, user_id, tipo_id, jornada_id, fecha_evento, mensaje,
   valor_detectado, leida, notificada_push, created_at)
SELECT UUID(), j.user_id, @t_pausa, j.id,
       j.hora_fin,
       'Jornada continuada de más de 6 h sin descanso de 15 min (Art. 34.4 ET).',
       '0 min', 0, 1, NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @pedro_id
  AND j.minutos_trabajados > 360
  AND j.minutos_pausa  = 0;

-- 8.3 Laura — SIN_DESCANSO_SEMANAL: 4 alertas (una por semana sin descanso)
INSERT INTO js5_js_alertas
  (uuid, user_id, tipo_id, jornada_id, fecha_evento, mensaje,
   valor_detectado, leida, notificada_push, created_at)
SELECT UUID(), @laura_id, @t_semanal, j.id,
       TIMESTAMP(j.fecha, '20:00:00'),
       'Más de 6 días consecutivos trabajados sin descanso semanal (Art. 37.1 ET).',
       '7 días', 0, 1, NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @laura_id
  AND j.fecha IN (
    DATE_SUB(@today, INTERVAL 23 DAY),
    DATE_SUB(@today, INTERVAL 16 DAY),
    DATE_SUB(@today, INTERVAL  9 DAY),
    DATE_SUB(@today, INTERVAL  2 DAY)
  );

-- 8.4 Miguel — HORAS_EXTRA_LIMITE: alerta única en el último día
INSERT INTO js5_js_alertas
  (uuid, user_id, tipo_id, jornada_id, fecha_evento, mensaje,
   valor_detectado, leida, notificada_push, created_at)
SELECT UUID(), @miguel_id, @t_extras, j.id,
       TIMESTAMP(j.fecha, '20:00:00'),
       'Acumulación de 78 h extraordinarias en 30 días, próximo al tope anual (Art. 35.2 ET).',
       '78 h / 80 h', 0, 1, NOW()
FROM js5_js_jornadas j
WHERE j.user_id = @miguel_id
  AND j.fecha = DATE_SUB(@today, INTERVAL 0 DAY);

-- 8.5 Miguel — RIESGO_BURNOUT
INSERT INTO js5_js_alertas
  (uuid, user_id, tipo_id, jornada_id, fecha_evento, mensaje,
   valor_detectado, leida, notificada_push, created_at)
VALUES
  (UUID(), @miguel_id, @t_burnout, NULL, NOW(),
   'Evaluación periódica arroja nivel ALTO de riesgo de burnout (Ley 31/1995 PRL).',
   '78 puntos', 0, 1, NOW());

-- =============================================================================
-- 9. EVALUACIONES DE BURNOUT
-- =============================================================================
INSERT INTO js5_js_burnout_evaluaciones
  (user_id, fecha_evaluacion, horas_promedio_dia, dias_sin_descanso,
   jornadas_excesivas, puntuacion, nivel, created_at)
VALUES
  -- Laura: MODERADO (8 h/día pero 30 días sin descanso semanal)
  (@laura_id,  NOW(), 8.00, 30,  0, 55.00, 'MODERADO', NOW()),
  -- Miguel: ALTO (11 h/día efectivas + horas extra acumuladas)
  (@miguel_id, NOW(), 11.00, 0, 26, 78.00, 'ALTO',     NOW()),
  -- Pedro: MODERADO (jornadas excesivas sin pausa pero con descanso semanal)
  (@pedro_id,  NOW(), 10.50, 0, 22, 60.00, 'MODERADO', NOW());

-- =============================================================================
-- 10. DOCUMENTOS — un PDF mensual por trabajador (registro de jornada)
-- =============================================================================
INSERT INTO js5_js_documentos
  (uuid, user_id, tipo, periodo_desde, periodo_hasta,
   nombre_fichero, ruta_storage, tamano_bytes, hash_sha256, firmado, created_at)
VALUES
  (UUID(), @pedro_id,  'REGISTRO_JORNADA_MENSUAL',
   DATE_SUB(@today, INTERVAL 30 DAY), @today,
   CONCAT('jornada_11111111A_', DATE_FORMAT(@today, '%Y%m'), '.pdf'),
   CONCAT('documentos/', @pedro_id,  '/', YEAR(@today), '/registro_pedro_',  DATE_FORMAT(@today, '%Y%m'), '.pdf'),
   125678, REPEAT('a', 64), 0, NOW()),
  (UUID(), @laura_id,  'REGISTRO_JORNADA_MENSUAL',
   DATE_SUB(@today, INTERVAL 30 DAY), @today,
   CONCAT('jornada_22222222B_', DATE_FORMAT(@today, '%Y%m'), '.pdf'),
   CONCAT('documentos/', @laura_id,  '/', YEAR(@today), '/registro_laura_',  DATE_FORMAT(@today, '%Y%m'), '.pdf'),
   118432, REPEAT('b', 64), 0, NOW()),
  (UUID(), @miguel_id, 'REGISTRO_JORNADA_MENSUAL',
   DATE_SUB(@today, INTERVAL 30 DAY), @today,
   CONCAT('jornada_33333333C_', DATE_FORMAT(@today, '%Y%m'), '.pdf'),
   CONCAT('documentos/', @miguel_id, '/', YEAR(@today), '/registro_miguel_', DATE_FORMAT(@today, '%Y%m'), '.pdf'),
   142890, REPEAT('c', 64), 0, NOW());

-- =============================================================================
-- 11. SELECT DE VERIFICACIÓN
--     Ejecutar en el cliente MySQL después del seed para validar resultados.
-- =============================================================================

-- 11.1 Resumen de jornadas y minutos por trabajador
SELECT
  u.nif,
  CONCAT(u.apellidos, ', ', u.nombre)               AS trabajador,
  COUNT(j.id)                                       AS jornadas,
  SUM(CASE WHEN j.minutos_trabajados > 540 THEN 1 ELSE 0 END) AS jornadas_excedidas,
  SUM(CASE WHEN j.minutos_pausa = 0
            AND j.minutos_trabajados > 360 THEN 1 ELSE 0 END) AS jornadas_sin_pausa,
  SUM(j.minutos_extra)                              AS total_min_extra,
  ROUND(SUM(j.minutos_extra)/60, 1)                 AS total_h_extra
FROM js5_js_users u
LEFT JOIN js5_js_jornadas j ON j.user_id = u.id
WHERE u.nif IN ('11111111A', '22222222B', '33333333C')
GROUP BY u.id, u.nif, u.apellidos, u.nombre
ORDER BY u.apellidos;

-- 11.2 Racha máxima de días consecutivos trabajados (Laura debería dar >= 7)
--      Estrategia clásica para "islas en MySQL": LAG → marcar saltos (>1 día) →
--      acumular en grupo con SUM ventana → agrupar y contar. Sin window
--      functions anidadas (cada nivel hace una sola).
SELECT
  u.nif,
  CONCAT(u.apellidos, ', ', u.nombre) AS trabajador,
  MAX(rachas.racha)                   AS dias_consecutivos_max
FROM (
  SELECT user_id, grupo, COUNT(*) AS racha
  FROM (
    SELECT
      user_id,
      fecha,
      SUM(CASE WHEN diff > 1 OR diff IS NULL THEN 1 ELSE 0 END)
        OVER (PARTITION BY user_id ORDER BY fecha) AS grupo
    FROM (
      SELECT
        user_id,
        fecha,
        DATEDIFF(fecha, LAG(fecha) OVER (PARTITION BY user_id ORDER BY fecha)) AS diff
      FROM js5_js_jornadas
      WHERE deleted_at IS NULL
    ) AS difs
  ) AS grupos
  GROUP BY user_id, grupo
) AS rachas
JOIN js5_js_users u ON u.id = rachas.user_id
WHERE u.nif IN ('11111111A', '22222222B', '33333333C')
GROUP BY u.id, u.nif, u.apellidos, u.nombre
ORDER BY u.apellidos;

-- 11.3 Alertas generadas por trabajador y tipo
SELECT
  u.nif,
  CONCAT(u.apellidos, ', ', u.nombre) AS trabajador,
  t.codigo                            AS alerta,
  t.severidad,
  COUNT(a.id)                         AS num_alertas
FROM js5_js_alertas a
JOIN js5_js_users         u ON u.id = a.user_id
JOIN js5_js_alertas_tipos t ON t.id = a.tipo_id
WHERE u.nif IN ('11111111A', '22222222B', '33333333C')
GROUP BY u.id, u.nif, u.apellidos, u.nombre, t.id, t.codigo, t.severidad
ORDER BY u.apellidos, t.codigo;

-- 11.4 Burnout y documentos generados
SELECT
  u.nif,
  CONCAT(u.apellidos, ', ', u.nombre) AS trabajador,
  b.nivel                             AS burnout_nivel,
  b.puntuacion                        AS burnout_puntos,
  COUNT(d.id)                         AS num_pdfs
FROM js5_js_users u
LEFT JOIN js5_js_burnout_evaluaciones b ON b.user_id = u.id
LEFT JOIN js5_js_documentos           d ON d.user_id = u.id AND d.deleted_at IS NULL
WHERE u.nif IN ('11111111A', '22222222B', '33333333C')
GROUP BY u.id, u.nif, u.apellidos, u.nombre, b.id, b.nivel, b.puntuacion
ORDER BY u.apellidos;

-- =============================================================================
-- Resultados esperados:
--
--   11.1 jornadas / excedidas / sin_pausa / total_h_extra
--          Pedro   22 / 22 / 22 / 33.0
--          Laura   30 /  0 /  0 /  0.0
--          Miguel  26 /  0 /  0 / 78.0
--
--   11.2 dias_consecutivos_max
--          Pedro    aprox 5
--          Laura    30
--          Miguel   aprox 6
--
--   11.3 alertas (mínimo)
--          Pedro    JORNADA_EXCEDIDA × 22, PAUSA_OMITIDA × 22
--          Laura    SIN_DESCANSO_SEMANAL × 4
--          Miguel   HORAS_EXTRA_LIMITE × 1, RIESGO_BURNOUT × 1
--
--   11.4 burnout_nivel / pdfs
--          Pedro    MODERADO / 1
--          Laura    MODERADO / 1
--          Miguel   ALTO     / 1
-- =============================================================================
