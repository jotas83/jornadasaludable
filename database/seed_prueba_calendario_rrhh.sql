-- Seed de prueba: jornada CORREGIDA para Pedro (11111111A) 
-- Para probar la vista de calendario RRHH en Joomla
-- Crea una jornada sin salida (CORREGIDA) para el día actual

INSERT INTO js5_js_jornadas 
    (uuid, user_id, fecha, hora_inicio, hora_fin,
     minutos_trabajados, minutos_pausa, minutos_extra,
     estado, sync_status, created_at, updated_at)
SELECT 
    UUID(), u.id, CURDATE(),
    TIMESTAMP(CURDATE(), '08:00:00'),
    NULL,
    0, 0, 0,
    'CORREGIDA', 'SYNCED',
    NOW(), NOW()
FROM js5_js_users u
WHERE u.nif = '11111111A';