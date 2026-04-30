-- =============================================================================
--  Seed de pruebas — usuario completo + empresa + sector + licencia + contrato.
--
--  Listo para importar TAL CUAL en phpMyAdmin sobre la base de datos
--  jornadasaludable (la que carga los dumps con prefijo js5_js_).
--
--  Pre-requisito: haber importado antes admin/sql/install.mysql.utf8.sql
--  (o database/jornadasaludable_wamp.sql) — necesitamos el seed de sectores.
--
--  Credenciales del usuario de prueba:
--    NIF:      12345678A
--    Email:    test@jornadasaludable.app
--    Password: test1234
--
--  Hash bcrypt generado con:
--    php -r 'echo password_hash("test1234", PASSWORD_BCRYPT, ["cost"=>12]);'
--  (cost=12 ≈ 250 ms en CPU moderna; coincide con la constante DUMMY_HASH
--   del AuthController para anti-timing).
--
--  Las sentencias usan INSERT IGNORE para que reimportar no falle por
--  colisión con UNIQUE constraints (cif, nif, uuid, codigo).
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
--  1. Empresa de prueba (sector CONSTRUCCION ya seedeado por el install)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `js5_js_empresas`
  (`cif`, `razon_social`, `nombre_comercial`, `sector_id`,
   `direccion`, `cp`, `municipio`, `provincia`,
   `email`, `telefono`, `activo`)
VALUES
  ('B12345678',
   'JornadaSaludable Test S.L.',
   'JS Test',
   (SELECT `id` FROM `js5_js_sectores` WHERE `codigo` = 'CONSTRUCCION'),
   'Calle Falsa 123', '28001', 'Madrid', 'Madrid',
   'admin@jornadasaludable.test', '+34 600 000 000',
   1);

-- -----------------------------------------------------------------------------
--  2. Centro de trabajo (opcional pero útil para fichajes con geofence)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `js5_js_centros_trabajo`
  (`empresa_id`, `codigo`, `nombre`, `direccion`, `cp`, `municipio`, `provincia`,
   `latitud`, `longitud`, `radio_geofence_m`, `activo`)
VALUES
  ((SELECT `id` FROM `js5_js_empresas` WHERE `cif` = 'B12345678'),
   'OFICINA_CENTRAL', 'Oficina Central Madrid',
   'Calle Falsa 123', '28001', 'Madrid', 'Madrid',
   40.4167754, -3.7037902, 150, 1);

-- -----------------------------------------------------------------------------
--  3. Licencia activa para la empresa
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `js5_js_licencias`
  (`empresa_id`, `tipo`, `max_usuarios`, `fecha_inicio`, `fecha_fin`, `activa`)
VALUES
  ((SELECT `id` FROM `js5_js_empresas` WHERE `cif` = 'B12345678'),
   'PROFESIONAL', 50, '2026-01-01', '2027-12-31', 1);

-- -----------------------------------------------------------------------------
--  4. Trabajador de prueba (password = test1234, hash bcrypt cost=12)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `js5_js_users`
  (`uuid`, `nif`, `nombre`, `apellidos`, `email`, `telefono`, `idioma`,
   `password_hash`, `activo`)
VALUES
  ('f8b98f17-cd13-4afe-8fd8-00593193da88',
   '12345678A',
   'Jose',
   'Sánchez Pruebas',
   'test@jornadasaludable.app',
   '+34 600 111 222',
   'es-ES',
   '$2y$12$1MPKAldVaao2BNF0zNdX5Od1PkJmKUsfKjnVwOcRQsYVmLR0MUBmi',
   1);

-- -----------------------------------------------------------------------------
--  5. Contrato vigente (INDEFINIDO, jornada COMPLETA 40h/semana)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `js5_js_contratos`
  (`uuid`, `user_id`, `empresa_id`, `tipo`, `jornada_tipo`, `horas_semanales`,
   `fecha_inicio`, `fecha_fin`, `vigente`)
VALUES
  ('23aa3780-0f0d-4995-b061-b31cf6a9e26d',
   (SELECT `id` FROM `js5_js_users`    WHERE `nif` = '12345678A'),
   (SELECT `id` FROM `js5_js_empresas` WHERE `cif` = 'B12345678'),
   'INDEFINIDO',
   'COMPLETA',
   40.00,
   '2026-01-01',
   NULL,
   1);

-- =============================================================================
--  Verificación rápida — descomentar para comprobar tras importar:
-- =============================================================================
-- SELECT u.id, u.nif, u.email, u.activo,
--        c.tipo AS contrato_tipo, c.horas_semanales, c.vigente,
--        e.razon_social, e.cif,
--        l.tipo AS licencia, l.activa AS licencia_activa
-- FROM `js5_js_users` u
-- JOIN `js5_js_contratos` c ON c.user_id = u.id AND c.vigente = 1
-- JOIN `js5_js_empresas`  e ON e.id      = c.empresa_id
-- JOIN `js5_js_licencias` l ON l.empresa_id = e.id AND l.activa = 1
-- WHERE u.nif = '12345678A';

-- =============================================================================
--  Limpieza — descomentar para BORRAR el seed de prueba:
-- =============================================================================
-- DELETE FROM `js5_js_contratos`     WHERE `uuid` = '23aa3780-0f0d-4995-b061-b31cf6a9e26d';
-- DELETE FROM `js5_js_users`         WHERE `nif`  = '12345678A';
-- DELETE FROM `js5_js_licencias`     WHERE `empresa_id` IN (SELECT id FROM `js5_js_empresas` WHERE `cif` = 'B12345678');
-- DELETE FROM `js5_js_centros_trabajo` WHERE `empresa_id` IN (SELECT id FROM `js5_js_empresas` WHERE `cif` = 'B12345678');
-- DELETE FROM `js5_js_empresas`      WHERE `cif`  = 'B12345678';
