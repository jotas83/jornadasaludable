SET NAMES utf8mb4;

-- Empresa de prueba
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

-- Centro de trabajo con geofence
INSERT IGNORE INTO `js5_js_centros_trabajo`
  (`empresa_id`, `codigo`, `nombre`, `direccion`, `cp`, `municipio`, `provincia`,
   `latitud`, `longitud`, `radio_geofence_m`, `activo`)
VALUES
  ((SELECT `id` FROM `js5_js_empresas` WHERE `cif` = 'B12345678'),
   'OFICINA_CENTRAL', 'Oficina Central Madrid',
   'Calle Falsa 123', '28001', 'Madrid', 'Madrid',
   40.4167754, -3.7037902, 150, 1);

-- Licencia activa
INSERT IGNORE INTO `js5_js_licencias`
  (`empresa_id`, `tipo`, `max_usuarios`, `fecha_inicio`, `fecha_fin`, `activa`)
VALUES
  ((SELECT `id` FROM `js5_js_empresas` WHERE `cif` = 'B12345678'),
   'PROFESIONAL', 50, '2026-01-01', '2027-12-31', 1);

-- Seed usuario de prueba: NIF 12345678A / Password: test1234
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

-- Contrato vigente: INDEFINIDO, 40h/semana
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