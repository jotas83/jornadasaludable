-- =============================================================================
--  JornadaSaludable — Esquema de base de datos
--  Motor: MySQL 8.0+ / InnoDB / utf8mb4
--  Backend: Plugin PHP para Joomla 5.x (JWT + TCPDF)
--  Cliente: Android Kotlin (MVVM + Room + WorkManager)
--  Esquema: 16 tablas (catálogos, dominio, sync, auditoría selectiva, licencias y métricas de burnout)
--
--  Convenciones
--  ------------
--  · Prefijo "js_" para todas las tablas. En despliegue Joomla, el plugin
--    aplicará el prefijo del sitio (#__) sustituyéndolo por "js_".
--  · Auditoría selectiva: created_at / updated_at / deleted_at solo en
--    tablas con valor legal probatorio (jornadas, fichajes, horas extra,
--    contratos, usuarios). Los catálogos llevan únicamente created_at.
--  · uuid CHAR(36) en tablas sincronizables — el cliente Room genera el
--    UUID en local y el servidor lo respeta (idempotencia de sync).
--  · sync_status / synced_at en filas que viajan desde el dispositivo.
--  · Soft delete mediante deleted_at IS NULL filtrado en las consultas.
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET time_zone = '+00:00';

DROP TABLE IF EXISTS `js5_js_licencias`;
DROP TABLE IF EXISTS `js5_js_burnout_evaluaciones`;
DROP TABLE IF EXISTS `js5_js_documentos`;
DROP TABLE IF EXISTS `js5_js_derechos`;
DROP TABLE IF EXISTS `js5_js_derechos_categorias`;
DROP TABLE IF EXISTS `js5_js_alertas`;
DROP TABLE IF EXISTS `js5_js_alertas_tipos`;
DROP TABLE IF EXISTS `js5_js_horas_extra`;
DROP TABLE IF EXISTS `js5_js_pausas`;
DROP TABLE IF EXISTS `js5_js_fichajes`;
DROP TABLE IF EXISTS `js5_js_jornadas`;
DROP TABLE IF EXISTS `js5_js_contratos`;
DROP TABLE IF EXISTS `js5_js_users`;
DROP TABLE IF EXISTS `js5_js_centros_trabajo`;
DROP TABLE IF EXISTS `js5_js_empresas`;
DROP TABLE IF EXISTS `js5_js_sectores`;

-- =============================================================================
--  1. js_sectores — Catálogo de sectores de actividad
-- =============================================================================
CREATE TABLE `js5_js_sectores` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `codigo`        VARCHAR(20)         NOT NULL COMMENT 'CONSTRUCCION, LIMPIEZA, HOSTELERIA, ...',
  `nombre`        VARCHAR(120)        NOT NULL,
  `cnae`          VARCHAR(10)         DEFAULT NULL COMMENT 'CNAE-2009',
  `convenio_ref`  VARCHAR(120)        DEFAULT NULL COMMENT 'Convenio colectivo de referencia',
  `jornada_max_diaria_min`   SMALLINT UNSIGNED NOT NULL DEFAULT 540 COMMENT 'Minutos. Default 9h Art. 34.3 ET',
  `jornada_max_semanal_min`  SMALLINT UNSIGNED NOT NULL DEFAULT 2400 COMMENT 'Minutos. Default 40h promedio anual',
  `descanso_entre_jornadas_min` SMALLINT UNSIGNED NOT NULL DEFAULT 720 COMMENT 'Minutos. Default 12h Art. 34.3 ET',
  `activo`        TINYINT(1)          NOT NULL DEFAULT 1,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sectores_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  2. js_empresas — Empresas / empleadores
-- =============================================================================
CREATE TABLE `js5_js_empresas` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `cif`           VARCHAR(20)         NOT NULL,
  `razon_social`  VARCHAR(200)        NOT NULL,
  `nombre_comercial` VARCHAR(200)     DEFAULT NULL,
  `sector_id`     INT UNSIGNED        NOT NULL,
  `joomla_user_id` INT UNSIGNED       DEFAULT NULL COMMENT 'FK lógica a #__users.id, admin de empresa opcional',
  `direccion`     VARCHAR(255)        DEFAULT NULL,
  `cp`            VARCHAR(10)         DEFAULT NULL,
  `municipio`     VARCHAR(120)        DEFAULT NULL,
  `provincia`     VARCHAR(120)        DEFAULT NULL,
  `email`         VARCHAR(190)        DEFAULT NULL,
  `telefono`      VARCHAR(30)         DEFAULT NULL,
  `activo`        TINYINT(1)          NOT NULL DEFAULT 1,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`    TIMESTAMP           NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_empresas_cif` (`cif`),
  KEY `ix_empresas_sector` (`sector_id`),
  KEY `ix_empresas_joomla_user` (`joomla_user_id`),
  CONSTRAINT `fk_empresas_sector` FOREIGN KEY (`sector_id`) REFERENCES `js5_js_sectores` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  3. js_centros_trabajo — Centros / obras / locales
-- =============================================================================
CREATE TABLE `js5_js_centros_trabajo` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `empresa_id`    INT UNSIGNED        NOT NULL,
  `codigo`        VARCHAR(40)         NOT NULL,
  `nombre`        VARCHAR(200)        NOT NULL,
  `direccion`     VARCHAR(255)        DEFAULT NULL,
  `cp`            VARCHAR(10)         DEFAULT NULL,
  `municipio`     VARCHAR(120)        DEFAULT NULL,
  `provincia`     VARCHAR(120)        DEFAULT NULL,
  `latitud`       DECIMAL(10,7)       DEFAULT NULL,
  `longitud`      DECIMAL(10,7)       DEFAULT NULL,
  `radio_geofence_m` SMALLINT UNSIGNED DEFAULT 150 COMMENT 'Radio geofence en metros',
  `activo`        TINYINT(1)          NOT NULL DEFAULT 1,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_centros_empresa_codigo` (`empresa_id`, `codigo`),
  KEY `ix_centros_geo` (`latitud`, `longitud`),
  CONSTRAINT `fk_centros_empresa` FOREIGN KEY (`empresa_id`) REFERENCES `js5_js_empresas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  4. js_users — Trabajadores (vinculados al usuario Joomla)
-- =============================================================================
CREATE TABLE `js5_js_users` (
  `id`              INT UNSIGNED       NOT NULL AUTO_INCREMENT,
  `joomla_user_id`  INT UNSIGNED       DEFAULT NULL COMMENT 'FK lógica a #__users.id',
  `uuid`            CHAR(36)           NOT NULL,
  `nif`             VARCHAR(20)        NOT NULL,
  `nombre`          VARCHAR(120)       NOT NULL,
  `apellidos`       VARCHAR(200)       NOT NULL,
  `email`           VARCHAR(190)       DEFAULT NULL,
  `telefono`        VARCHAR(30)        DEFAULT NULL,
  `idioma`          CHAR(5)            NOT NULL DEFAULT 'es-ES',
  `fecha_nacimiento` DATE              DEFAULT NULL,
  `nacionalidad`    VARCHAR(80)        DEFAULT NULL,
  `password_hash`   VARCHAR(255)       DEFAULT NULL COMMENT 'Hash propio si no usa Joomla',
  `jwt_refresh_token` VARCHAR(512)     DEFAULT NULL,
  `device_id`       VARCHAR(120)       DEFAULT NULL COMMENT 'Android device fingerprint',
  `push_token`      VARCHAR(255)       DEFAULT NULL COMMENT 'Token notificaciones push locales (NotificationCompat)',
  `last_login_at`   TIMESTAMP          NULL DEFAULT NULL,
  `last_sync_at`    TIMESTAMP          NULL DEFAULT NULL,
  `activo`          TINYINT(1)         NOT NULL DEFAULT 1,
  `created_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`      TIMESTAMP          NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_uuid` (`uuid`),
  UNIQUE KEY `uk_users_nif` (`nif`),
  UNIQUE KEY `uk_users_joomla` (`joomla_user_id`),
  KEY `ix_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  5. js_contratos — Relación laboral usuario ↔ empresa
-- =============================================================================
CREATE TABLE `js5_js_contratos` (
  `id`              INT UNSIGNED       NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)           NOT NULL,
  `user_id`         INT UNSIGNED       NOT NULL,
  `empresa_id`      INT UNSIGNED       NOT NULL,
  `tipo`            ENUM('INDEFINIDO','TEMPORAL','FIJO_DISCONTINUO','FORMACION','PRACTICAS') NOT NULL,
  `jornada_tipo`    ENUM('COMPLETA','PARCIAL') NOT NULL DEFAULT 'COMPLETA',
  `horas_semanales` DECIMAL(5,2)       NOT NULL DEFAULT 40.00,
  `fecha_inicio`    DATE               NOT NULL,
  `fecha_fin`       DATE               DEFAULT NULL,
  `vigente`         TINYINT(1)         NOT NULL DEFAULT 1,
  `created_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contratos_uuid` (`uuid`),
  KEY `ix_contratos_user` (`user_id`, `vigente`),
  KEY `ix_contratos_empresa` (`empresa_id`),
  CONSTRAINT `fk_contratos_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_contratos_empresa` FOREIGN KEY (`empresa_id`) REFERENCES `js5_js_empresas` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  6. js_jornadas — Jornada diaria (agregado de fichajes)
--  Cumplimiento Art. 12 RD-Ley 8/2019: registro diario de jornada.
-- =============================================================================
CREATE TABLE `js5_js_jornadas` (
  `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)           NOT NULL,
  `user_id`         INT UNSIGNED       NOT NULL,
  `contrato_id`     INT UNSIGNED       DEFAULT NULL,
  `centro_id`       INT UNSIGNED       DEFAULT NULL,
  `fecha`           DATE               NOT NULL,
  `hora_inicio`     DATETIME           DEFAULT NULL,
  `hora_fin`        DATETIME           DEFAULT NULL,
  `minutos_trabajados` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `minutos_pausa`   SMALLINT UNSIGNED  NOT NULL DEFAULT 0,
  `minutos_extra`   SMALLINT UNSIGNED  NOT NULL DEFAULT 0,
  `estado`          ENUM('ABIERTA','CERRADA','VALIDADA','CORREGIDA') NOT NULL DEFAULT 'ABIERTA',
  `observaciones`   TEXT               DEFAULT NULL,
  `firma_trabajador_hash` VARCHAR(128) DEFAULT NULL COMMENT 'SHA-256 de la firma',
  `validada_at`     TIMESTAMP          NULL DEFAULT NULL,
  `sync_status`     ENUM('PENDING','SYNCED','CONFLICT') NOT NULL DEFAULT 'PENDING',
  `synced_at`       TIMESTAMP          NULL DEFAULT NULL,
  `created_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`      TIMESTAMP          NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jornadas_uuid` (`uuid`),
  UNIQUE KEY `uk_jornadas_user_fecha` (`user_id`, `fecha`),
  KEY `ix_jornadas_estado` (`estado`),
  KEY `ix_jornadas_sync` (`sync_status`),
  CONSTRAINT `fk_jornadas_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_jornadas_contrato` FOREIGN KEY (`contrato_id`) REFERENCES `js5_js_contratos` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_jornadas_centro` FOREIGN KEY (`centro_id`) REFERENCES `js5_js_centros_trabajo` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  7. js_fichajes — Eventos individuales de entrada / salida
-- =============================================================================
CREATE TABLE `js5_js_fichajes` (
  `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)           NOT NULL,
  `jornada_id`      BIGINT UNSIGNED    NOT NULL,
  `user_id`         INT UNSIGNED       NOT NULL,
  `empresa_id`      INT UNSIGNED       DEFAULT NULL COMMENT 'Denormalización: empresa derivada del contrato vigente al fichar',
  `tipo`            ENUM('ENTRADA','SALIDA') NOT NULL,
  `timestamp_evento` DATETIME(3)       NOT NULL,
  `latitud`         DECIMAL(10,7)      DEFAULT NULL,
  `longitud`        DECIMAL(10,7)      DEFAULT NULL,
  `precision_gps_m` DECIMAL(6,2)       DEFAULT NULL,
  `dentro_geofence` TINYINT(1)         DEFAULT NULL,
  `metodo`          ENUM('MANUAL','AUTO_GEOFENCE','NFC','QR') NOT NULL DEFAULT 'MANUAL',
  `device_id`       VARCHAR(120)       DEFAULT NULL,
  `bateria_pct`     TINYINT UNSIGNED   DEFAULT NULL,
  `red_tipo`        ENUM('WIFI','MOBILE','OFFLINE') DEFAULT NULL,
  `observaciones`   VARCHAR(500)       DEFAULT NULL,
  `sync_status`     ENUM('PENDING','SYNCED','CONFLICT') NOT NULL DEFAULT 'PENDING',
  `synced_at`       TIMESTAMP          NULL DEFAULT NULL,
  `created_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`      TIMESTAMP          NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fichajes_uuid` (`uuid`),
  KEY `ix_fichajes_jornada` (`jornada_id`),
  KEY `ix_fichajes_user_ts` (`user_id`, `timestamp_evento`),
  KEY `ix_fichajes_empresa_ts` (`empresa_id`, `timestamp_evento`),
  KEY `ix_fichajes_sync` (`sync_status`),
  CONSTRAINT `fk_fichajes_jornada` FOREIGN KEY (`jornada_id`) REFERENCES `js5_js_jornadas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fichajes_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fichajes_empresa` FOREIGN KEY (`empresa_id`) REFERENCES `js5_js_empresas` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  8. js_pausas — Pausas y descansos dentro de la jornada
--  Art. 34.4 ET: 15 min cuando jornada > 6h.
-- =============================================================================
CREATE TABLE `js5_js_pausas` (
  `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)           NOT NULL,
  `jornada_id`      BIGINT UNSIGNED    NOT NULL,
  `tipo`            ENUM('BOCADILLO','COMIDA','DESCANSO_LEGAL','OTROS') NOT NULL DEFAULT 'DESCANSO_LEGAL',
  `inicio`          DATETIME(3)        NOT NULL,
  `fin`             DATETIME(3)        DEFAULT NULL,
  `latitud`         DECIMAL(10,7)      DEFAULT NULL,
  `longitud`        DECIMAL(10,7)      DEFAULT NULL,
  `minutos`         SMALLINT UNSIGNED  NOT NULL DEFAULT 0,
  `computa_jornada` TINYINT(1)         NOT NULL DEFAULT 0 COMMENT '1 si la pausa se considera tiempo efectivo',
  `sync_status`     ENUM('PENDING','SYNCED','CONFLICT') NOT NULL DEFAULT 'PENDING',
  `created_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pausas_uuid` (`uuid`),
  KEY `ix_pausas_jornada` (`jornada_id`),
  CONSTRAINT `fk_pausas_jornada` FOREIGN KEY (`jornada_id`) REFERENCES `js5_js_jornadas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
--  9. js_horas_extra — Horas extraordinarias declaradas
--  Art. 35 ET: máx. 80 h/año, voluntarias salvo pacto, compensables.
-- =============================================================================
CREATE TABLE `js5_js_horas_extra` (
  `id`              BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)           NOT NULL,
  `jornada_id`      BIGINT UNSIGNED    NOT NULL,
  `user_id`         INT UNSIGNED       NOT NULL,
  `minutos`         SMALLINT UNSIGNED  NOT NULL,
  `tipo`            ENUM('ORDINARIA','FESTIVA') NOT NULL DEFAULT 'ORDINARIA' COMMENT 'Categorización worker-facing: día laborable vs festivo',
  `compensacion`    ENUM('ECONOMICA','DESCANSO','MIXTA','PENDIENTE') NOT NULL DEFAULT 'PENDIENTE',
  `estado`          ENUM('PENDIENTE','APROBADA','COMPENSADA') NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Workflow: registrada → aprobada por responsable → compensada (pagada o disfrutada)',
  `importe_bruto`   DECIMAL(8,2)       DEFAULT NULL,
  `descanso_minutos` SMALLINT UNSIGNED DEFAULT NULL,
  `aceptada_trabajador` TINYINT(1)     NOT NULL DEFAULT 0,
  `observaciones`   VARCHAR(500)       DEFAULT NULL,
  `created_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`      TIMESTAMP          NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_horasextra_uuid` (`uuid`),
  KEY `ix_horasextra_user` (`user_id`),
  KEY `ix_horasextra_jornada` (`jornada_id`),
  CONSTRAINT `fk_horasextra_jornada` FOREIGN KEY (`jornada_id`) REFERENCES `js5_js_jornadas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_horasextra_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 10. js_alertas_tipos — Catálogo de tipos de alerta (6 tipos)
-- =============================================================================
CREATE TABLE `js5_js_alertas_tipos` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `codigo`        VARCHAR(40)         NOT NULL,
  `nombre`        VARCHAR(160)        NOT NULL,
  `descripcion`   VARCHAR(500)        NOT NULL,
  `severidad`     ENUM('INFORMATIVA','AVISO','GRAVE','CRITICA') NOT NULL DEFAULT 'AVISO',
  `referencia_legal` VARCHAR(160)     DEFAULT NULL,
  `umbral_valor`  INT                 DEFAULT NULL COMMENT 'Valor numérico del umbral',
  `umbral_unidad` VARCHAR(20)         DEFAULT NULL COMMENT 'min, h, dias',
  `activa`        TINYINT(1)          NOT NULL DEFAULT 1,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alertas_tipos_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 11. js_alertas — Alertas generadas para el trabajador
-- =============================================================================
CREATE TABLE `js5_js_alertas` (
  `id`            BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
  `uuid`          CHAR(36)            NOT NULL,
  `user_id`       INT UNSIGNED        NOT NULL,
  `tipo_id`       INT UNSIGNED        NOT NULL,
  `jornada_id`    BIGINT UNSIGNED     DEFAULT NULL,
  `fecha_evento`  DATETIME            NOT NULL,
  `mensaje`       VARCHAR(500)        NOT NULL,
  `valor_detectado` VARCHAR(60)       DEFAULT NULL,
  `leida`         TINYINT(1)          NOT NULL DEFAULT 0,
  `leida_at`      TIMESTAMP           NULL DEFAULT NULL,
  `accion_tomada` VARCHAR(255)        DEFAULT NULL,
  `notificada_push` TINYINT(1)        NOT NULL DEFAULT 0,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alertas_uuid` (`uuid`),
  KEY `ix_alertas_user_leida` (`user_id`, `leida`),
  KEY `ix_alertas_tipo` (`tipo_id`),
  KEY `ix_alertas_jornada` (`jornada_id`),
  CONSTRAINT `fk_alertas_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_alertas_tipo` FOREIGN KEY (`tipo_id`) REFERENCES `js5_js_alertas_tipos` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_alertas_jornada` FOREIGN KEY (`jornada_id`) REFERENCES `js5_js_jornadas` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 12. js_derechos_categorias — Categorías del módulo Mis Derechos
-- =============================================================================
CREATE TABLE `js5_js_derechos_categorias` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `codigo`        VARCHAR(40)         NOT NULL,
  `nombre`        VARCHAR(160)        NOT NULL,
  `descripcion`   VARCHAR(500)        DEFAULT NULL,
  `icono`         VARCHAR(60)         DEFAULT NULL,
  `orden`         SMALLINT UNSIGNED   NOT NULL DEFAULT 0,
  `activa`        TINYINT(1)          NOT NULL DEFAULT 1,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_derechos_cat_codigo` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 13. js_derechos — Artículos del módulo Mis Derechos
-- =============================================================================
CREATE TABLE `js5_js_derechos` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `categoria_id`  INT UNSIGNED        NOT NULL,
  `codigo`        VARCHAR(60)         NOT NULL COMMENT 'Ej. ART_34_ET',
  `titulo`        VARCHAR(255)        NOT NULL,
  `referencia_legal` VARCHAR(200)     NOT NULL,
  `resumen`       VARCHAR(500)        NOT NULL COMMENT 'Resumen plano para listado',
  `contenido_md`  MEDIUMTEXT          NOT NULL COMMENT 'Cuerpo en Markdown',
  `palabras_clave` VARCHAR(500)       DEFAULT NULL,
  `url_boe`       VARCHAR(500)        DEFAULT NULL COMMENT 'URL al texto oficial en el BOE',
  `idioma`        CHAR(5)             NOT NULL DEFAULT 'es-ES',
  `version`       VARCHAR(20)         NOT NULL DEFAULT '1.0',
  `vigente_desde` DATE                NOT NULL,
  `vigente_hasta` DATE                DEFAULT NULL,
  `orden`         SMALLINT UNSIGNED   NOT NULL DEFAULT 0,
  `publicado`     TINYINT(1)          NOT NULL DEFAULT 1,
  `consultas_count` INT UNSIGNED      NOT NULL DEFAULT 0 COMMENT 'Veces solicitado vía GET /derechos/{codigo}',
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_derechos_codigo_idioma` (`codigo`, `idioma`),
  KEY `ix_derechos_cat` (`categoria_id`, `orden`),
  FULLTEXT KEY `ft_derechos_busqueda` (`titulo`, `resumen`, `contenido_md`, `palabras_clave`),
  CONSTRAINT `fk_derechos_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `js5_js_derechos_categorias` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 14. js_documentos — Documentos PDF generados (TCPDF)
-- =============================================================================
CREATE TABLE `js5_js_documentos` (
  `id`            BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
  `uuid`          CHAR(36)            NOT NULL,
  `user_id`       INT UNSIGNED        NOT NULL,
  `tipo`          ENUM('REGISTRO_JORNADA_MENSUAL','RESUMEN_HORAS_EXTRA','SOLICITUD_VACACIONES','CERTIFICADO_DERECHOS','OTROS') NOT NULL,
  `periodo_desde` DATE                DEFAULT NULL,
  `periodo_hasta` DATE                DEFAULT NULL,
  `nombre_fichero` VARCHAR(200)       NOT NULL,
  `ruta_storage`  VARCHAR(500)        NOT NULL,
  `tamano_bytes`  INT UNSIGNED        DEFAULT NULL,
  `hash_sha256`   CHAR(64)            DEFAULT NULL,
  `firmado`       TINYINT(1)          NOT NULL DEFAULT 0,
  `firma_alg`     VARCHAR(40)         DEFAULT NULL COMMENT 'PAdES, ETSI...',
  `descargado`    TINYINT(1)          NOT NULL DEFAULT 0,
  `descargado_at` TIMESTAMP           NULL DEFAULT NULL,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at`    TIMESTAMP           NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_documentos_uuid` (`uuid`),
  KEY `ix_documentos_user_tipo` (`user_id`, `tipo`),
  CONSTRAINT `fk_documentos_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 15. js_burnout_evaluaciones — Evaluaciones de riesgo de burnout
--  Calculadas por el motor de WorkManager a partir de las jornadas del usuario.
-- =============================================================================
CREATE TABLE `js5_js_burnout_evaluaciones` (
  `id`                  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
  `user_id`             INT UNSIGNED   NOT NULL,
  `fecha_evaluacion`    DATETIME       NOT NULL,
  `horas_promedio_dia`  DECIMAL(4,2)   DEFAULT NULL,
  `dias_sin_descanso`   INT            DEFAULT NULL,
  `jornadas_excesivas`  INT            DEFAULT NULL,
  `puntuacion`          DECIMAL(5,2)   DEFAULT NULL COMMENT '0-100, agregado ponderado',
  `nivel`               ENUM('BAJO','MODERADO','ALTO','CRITICO') DEFAULT NULL,
  `created_at`          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_burnout_user_fecha` (`user_id`, `fecha_evaluacion`),
  KEY `ix_burnout_nivel` (`nivel`),
  CONSTRAINT `fk_burnout_user` FOREIGN KEY (`user_id`) REFERENCES `js5_js_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 16. js_licencias — Licencias B2B asociadas a la empresa contratante
-- =============================================================================
CREATE TABLE `js5_js_licencias` (
  `id`            INT UNSIGNED        NOT NULL AUTO_INCREMENT,
  `empresa_id`    INT UNSIGNED        NOT NULL,
  `tipo`          ENUM('BASICA','PROFESIONAL','ENTERPRISE') NOT NULL,
  `max_usuarios`  INT                 DEFAULT NULL,
  `fecha_inicio`  DATE                NOT NULL,
  `fecha_fin`     DATE                DEFAULT NULL,
  `activa`        TINYINT(1)          NOT NULL DEFAULT 1,
  `created_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_licencias_empresa_activa` (`empresa_id`, `activa`),
  CONSTRAINT `fk_licencias_empresa` FOREIGN KEY (`empresa_id`) REFERENCES `js5_js_empresas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
--  SEED DATA
-- =============================================================================

-- -----------------------------------------------------------------------------
--  Sectores
-- -----------------------------------------------------------------------------
INSERT INTO `js5_js_sectores` (`codigo`, `nombre`, `cnae`, `convenio_ref`, `jornada_max_diaria_min`, `jornada_max_semanal_min`, `descanso_entre_jornadas_min`) VALUES
('CONSTRUCCION', 'Construcción', '41', 'Convenio General del Sector de la Construcción', 540, 2400, 720),
('LIMPIEZA',     'Limpieza de edificios y locales', '81', 'Convenio Estatal de Limpieza de Edificios y Locales', 540, 2400, 720),
('HOSTELERIA',   'Hostelería', '56', 'Acuerdo Laboral Estatal de Hostelería (ALEH)', 540, 2400, 720);

-- -----------------------------------------------------------------------------
--  Tipos de alerta — 7 tipos
-- -----------------------------------------------------------------------------
INSERT INTO `js5_js_alertas_tipos` (`codigo`, `nombre`, `descripcion`, `severidad`, `referencia_legal`, `umbral_valor`, `umbral_unidad`) VALUES
('JORNADA_EXCEDIDA',
 'Jornada diaria excedida',
 'La jornada efectiva supera las 9 horas diarias. Solo es lícito superarlas mediante distribución irregular pactada por convenio o acuerdo.',
 'GRAVE',
 'Art. 34.3 Estatuto de los Trabajadores',
 540, 'min'),

('DESCANSO_INSUFICIENTE',
 'Descanso entre jornadas insuficiente',
 'No se han respetado las 12 horas mínimas de descanso entre el final de una jornada y el inicio de la siguiente.',
 'GRAVE',
 'Art. 34.3 Estatuto de los Trabajadores',
 720, 'min'),

('PAUSA_OMITIDA',
 'Pausa diaria no realizada',
 'En jornadas continuadas superiores a 6 horas debe disfrutarse un descanso mínimo de 15 minutos.',
 'AVISO',
 'Art. 34.4 Estatuto de los Trabajadores',
 15, 'min'),

('HORAS_EXTRA_LIMITE',
 'Límite anual de horas extraordinarias',
 'Se aproxima o supera el tope legal de 80 horas extraordinarias anuales.',
 'GRAVE',
 'Art. 35.2 Estatuto de los Trabajadores',
 80, 'h'),

('SIN_DESCANSO_SEMANAL',
 'Descanso semanal no respetado',
 'No se ha disfrutado el descanso semanal mínimo de 1,5 días ininterrumpidos (acumulables a 14 días en menores de 18).',
 'CRITICA',
 'Art. 37.1 Estatuto de los Trabajadores',
 36, 'h'),

('FICHAJE_INCOMPLETO',
 'Registro de jornada incompleto',
 'Falta el fichaje de entrada o de salida del día. El registro diario es obligatorio para empresa y trabajador.',
 'AVISO',
 'Art. 12 RD-Ley 8/2019',
 1, 'fichaje'),

('RIESGO_BURNOUT',
 'Riesgo de burnout detectado',
 'La evaluación periódica de carga laboral arroja un nivel ALTO o CRÍTICO. Conviene revisar jornada, descansos y solicitar evaluación de riesgos psicosociales.',
 'GRAVE',
 'Ley 31/1995 PRL, Arts. 14-15 (riesgos psicosociales)',
 75, 'puntos');

-- -----------------------------------------------------------------------------
--  Categorías del módulo Mis Derechos
-- -----------------------------------------------------------------------------
INSERT INTO `js5_js_derechos_categorias` (`codigo`, `nombre`, `descripcion`, `icono`, `orden`) VALUES
('JORNADA',     'Jornada y horario',     'Duración de la jornada, horario, distribución y descansos diarios.', 'ic_clock',     10),
('HORAS_EXTRA', 'Horas extraordinarias', 'Realización, compensación y límite anual de horas extras.',           'ic_extra_time',20),
('DESCANSOS',   'Descansos y vacaciones','Descanso semanal, festivos y vacaciones anuales retribuidas.',        'ic_beach',     30),
('REGISTRO',    'Registro horario',      'Obligación empresarial de registro diario de jornada.',               'ic_punch_card',40);

-- -----------------------------------------------------------------------------
--  Mis Derechos — seed (Arts. 34, 35, 37, 38 ET y Art. 12 RD-Ley 8/2019)
-- -----------------------------------------------------------------------------

-- Art. 34 ET — Jornada
INSERT INTO `js5_js_derechos`
(`categoria_id`, `codigo`, `titulo`, `referencia_legal`, `resumen`, `contenido_md`, `palabras_clave`, `idioma`, `version`, `vigente_desde`, `orden`, `publicado`)
VALUES
((SELECT id FROM js5_js_derechos_categorias WHERE codigo='JORNADA'),
 'ART_34_ET',
 'Artículo 34 ET — Jornada de trabajo',
 'Real Decreto Legislativo 2/2015, Art. 34',
 'La duración máxima de la jornada ordinaria es de 40 horas semanales de promedio en cómputo anual, con descanso mínimo de 12 h entre jornadas y 15 min de pausa cuando se superan 6 h continuadas.',
 '## Duración de la jornada\n\n- **Máximo 40 horas semanales** de trabajo efectivo de promedio en cómputo anual.\n- La distribución irregular podrá pactarse por convenio o acuerdo, pero **no podrá superar el 10 % de la jornada anual** sin pacto expreso.\n\n## Jornada diaria\n\n- **No se podrán realizar más de 9 horas ordinarias** de trabajo efectivo al día, salvo distribución irregular pactada.\n- Trabajadores menores de 18 años: máximo **8 horas diarias** incluidas las dedicadas a formación.\n\n## Descansos\n\n- Entre el final de una jornada y el comienzo de la siguiente mediarán, como mínimo, **12 horas**.\n- Cuando la jornada continuada exceda de **6 horas**, deberá disfrutarse un descanso mínimo de **15 minutos**.\n- Si se trata de menores de 18 años, este descanso será de **30 minutos** cuando la jornada exceda de 4 h 30 min.\n\n## Adaptación de jornada (conciliación)\n\nLas personas trabajadoras tienen derecho a **solicitar adaptaciones razonables** de la duración y distribución de la jornada para hacer efectivo su derecho a la conciliación de la vida familiar y laboral.',
 'jornada,40 horas,9 horas,descanso 12 horas,pausa 15 minutos,distribución irregular,conciliación',
 'es-ES', '1.0', '2024-01-01', 10, 1);

-- Art. 35 ET — Horas extraordinarias
INSERT INTO `js5_js_derechos`
(`categoria_id`, `codigo`, `titulo`, `referencia_legal`, `resumen`, `contenido_md`, `palabras_clave`, `idioma`, `version`, `vigente_desde`, `orden`, `publicado`)
VALUES
((SELECT id FROM js5_js_derechos_categorias WHERE codigo='HORAS_EXTRA'),
 'ART_35_ET',
 'Artículo 35 ET — Horas extraordinarias',
 'Real Decreto Legislativo 2/2015, Art. 35',
 'Son horas extras las que excedan la jornada ordinaria. Son voluntarias salvo pacto o fuerza mayor, se compensan en dinero o descanso, y el tope es de 80 al año.',
 '## Concepto\n\nSon horas extraordinarias las que se realicen **sobre la duración máxima de la jornada ordinaria** fijada de acuerdo con el artículo anterior.\n\n## Compensación\n\nMediante convenio colectivo o, en su defecto, contrato individual, se optará entre:\n\n1. **Abonarlas en la cuantía que se fije** (nunca inferior al valor de la hora ordinaria).\n2. **Compensarlas por tiempos equivalentes de descanso retribuido**.\n\nEn ausencia de pacto, se entenderá que las horas extras realizadas deberán ser compensadas mediante **descanso dentro de los 4 meses siguientes** a su realización.\n\n## Límite anual\n\nEl número de horas extraordinarias no podrá ser superior a **80 al año**, salvo:\n\n- Las compensadas con descanso dentro de los 4 meses siguientes (no computan a estos efectos).\n- Las trabajadas para **prevenir o reparar siniestros y otros daños extraordinarios y urgentes** (fuerza mayor).\n\n## Voluntariedad\n\nLa prestación de trabajo en horas extraordinarias es **voluntaria**, salvo:\n\n- Pacto en convenio colectivo o contrato individual de trabajo, dentro del límite legal.\n- Fuerza mayor.\n\n## Registro\n\nA efectos del cómputo, **la jornada de cada persona trabajadora se registrará día a día** y se totalizará en el periodo fijado para el abono de las retribuciones, entregando copia del resumen al trabajador junto con el recibo de salarios.',
 'horas extras,extraordinarias,80 horas,compensación,descanso,fuerza mayor,voluntariedad',
 'es-ES', '1.0', '2024-01-01', 10, 1);

-- Art. 37 ET — Descanso semanal, festivos y permisos
INSERT INTO `js5_js_derechos`
(`categoria_id`, `codigo`, `titulo`, `referencia_legal`, `resumen`, `contenido_md`, `palabras_clave`, `idioma`, `version`, `vigente_desde`, `orden`, `publicado`)
VALUES
((SELECT id FROM js5_js_derechos_categorias WHERE codigo='DESCANSOS'),
 'ART_37_ET',
 'Artículo 37 ET — Descanso semanal, fiestas y permisos',
 'Real Decreto Legislativo 2/2015, Art. 37',
 'Descanso semanal mínimo de día y medio ininterrumpido (2 días para menores), 14 fiestas anuales y permisos retribuidos por matrimonio, fallecimiento, traslado, deber inexcusable y otros.',
 '## Descanso semanal\n\nLas personas trabajadoras tendrán derecho a un descanso mínimo semanal, **acumulable por períodos de hasta 14 días, de día y medio ininterrumpido** que, como regla general, comprenderá:\n\n- La tarde del **sábado** o, en su caso, la mañana del **lunes**.\n- El **domingo completo**.\n\nLa duración del descanso semanal de los menores de 18 años será, como mínimo, de **dos días ininterrumpidos**.\n\n## Fiestas laborales\n\nLas fiestas laborales, retribuidas y no recuperables, no podrán exceder de **14 al año**, de las cuales **dos serán locales**.\n\n## Permisos retribuidos\n\nPrevia comunicación y justificación, la persona trabajadora podrá ausentarse del trabajo, con derecho a remuneración:\n\n- **15 días naturales** por matrimonio o registro de pareja de hecho.\n- **5 días** por accidente o enfermedad graves, hospitalización o intervención quirúrgica sin hospitalización que precise reposo domiciliario, del cónyuge, pareja de hecho o parientes hasta el 2.º grado.\n- **2 días** por el fallecimiento del cónyuge, pareja de hecho o parientes hasta el 2.º grado.\n- **1 día** por traslado del domicilio habitual.\n- Por el tiempo indispensable para el cumplimiento de un **deber inexcusable de carácter público y personal**.\n- Para realizar **funciones sindicales o de representación** del personal.\n- Para el **cuidado del lactante** menor de 9 meses (1 hora de ausencia o reducción de jornada).\n- Por **causa de fuerza mayor**, hasta 4 días al año, cuando sea necesaria por motivos familiares urgentes.\n\n## Reducción por cuidado de familiares\n\nQuien por razones de guarda legal tenga a su cuidado directo a un menor de 12 años, persona con discapacidad o familiar dependiente, tendrá derecho a una **reducción de la jornada de trabajo** entre, al menos, **un octavo y un máximo de la mitad** de la duración de aquélla, con la disminución proporcional del salario.',
 'descanso semanal,día y medio,festivos,14 fiestas,permisos retribuidos,matrimonio,fallecimiento,lactancia,reducción jornada,guarda legal',
 'es-ES', '1.0', '2024-01-01', 10, 1);

-- Art. 38 ET — Vacaciones
INSERT INTO `js5_js_derechos`
(`categoria_id`, `codigo`, `titulo`, `referencia_legal`, `resumen`, `contenido_md`, `palabras_clave`, `idioma`, `version`, `vigente_desde`, `orden`, `publicado`)
VALUES
((SELECT id FROM js5_js_derechos_categorias WHERE codigo='DESCANSOS'),
 'ART_38_ET',
 'Artículo 38 ET — Vacaciones anuales',
 'Real Decreto Legislativo 2/2015, Art. 38',
 'Vacaciones anuales retribuidas no sustituibles por compensación económica, mínimo de 30 días naturales, fijadas por acuerdo y conocidas con al menos 2 meses de antelación.',
 '## Duración\n\nEl período de vacaciones anuales retribuidas, **no sustituible por compensación económica**, será el pactado en convenio colectivo o contrato individual.\n\nEn ningún caso la duración será inferior a **30 días naturales**.\n\n## Fijación del período\n\n- El período o períodos de su disfrute se **fijarán de común acuerdo** entre el empresario y el trabajador, de conformidad con lo establecido en su caso en los convenios colectivos sobre planificación anual.\n- En caso de desacuerdo, la **jurisdicción social** fijará la fecha que para el disfrute corresponda. El procedimiento será sumario y preferente.\n- El calendario de vacaciones se fijará en cada empresa y la persona trabajadora **conocerá las fechas que le correspondan dos meses antes, al menos**, del comienzo del disfrute.\n\n## Coincidencia con incapacidad temporal o permisos\n\n- Si las vacaciones coinciden con una **incapacidad temporal por embarazo, parto o lactancia natural**, o con el período de suspensión por nacimiento, adopción, guarda con fines de adopción o acogimiento, se tendrá derecho a disfrutarlas en **fecha distinta**, aunque haya terminado el año natural.\n- Si la incapacidad temporal es por **otras contingencias** y no permite disfrutarlas, se podrán disfrutar **una vez finalice la incapacidad** y siempre que no hayan transcurrido **más de 18 meses** a partir del final del año en que se hayan originado.\n\n## Retribución\n\nDurante las vacaciones la persona trabajadora percibirá la **retribución normal y habitual**, incluyendo los promedios variables que correspondan según convenio o jurisprudencia aplicable.',
 'vacaciones,30 días naturales,no compensables,calendario,2 meses antelación,IT,embarazo,18 meses',
 'es-ES', '1.0', '2024-01-01', 10, 1);

-- Art. 12 RD-Ley 8/2019 — Registro horario
INSERT INTO `js5_js_derechos`
(`categoria_id`, `codigo`, `titulo`, `referencia_legal`, `resumen`, `contenido_md`, `palabras_clave`, `idioma`, `version`, `vigente_desde`, `orden`, `publicado`)
VALUES
((SELECT id FROM js5_js_derechos_categorias WHERE codigo='REGISTRO'),
 'ART_12_RDL_8_2019',
 'Artículo 12 RD-Ley 8/2019 — Registro horario',
 'Real Decreto-ley 8/2019, de 8 de marzo, Art. 12 (modifica Art. 34.9 ET)',
 'La empresa garantizará el registro diario de jornada, incluyendo el horario concreto de inicio y finalización, conservándolo 4 años a disposición de los trabajadores, sus representantes y la Inspección de Trabajo.',
 '## Obligación empresarial\n\nLa empresa **garantizará el registro diario de jornada**, que deberá incluir el **horario concreto de inicio y finalización** de la jornada de trabajo de cada persona trabajadora, sin perjuicio de la flexibilidad horaria que pueda establecerse.\n\n## Negociación colectiva\n\nMediante negociación colectiva o **acuerdo de empresa** o, en su defecto, decisión del empresario previa consulta con los representantes legales de los trabajadores, se organizará y documentará este registro de jornada.\n\n## Conservación\n\nLa empresa **conservará los registros durante 4 años** y permanecerán a disposición de:\n\n- Las **personas trabajadoras**.\n- Sus **representantes legales**.\n- La **Inspección de Trabajo y Seguridad Social**.\n\n## Régimen sancionador\n\nEl incumplimiento de la obligación de registro de jornada constituye una **infracción grave** según la Ley sobre Infracciones y Sanciones en el Orden Social (LISOS), con sanciones que pueden alcanzar los **7.500 €** por trabajador afectado en su grado máximo.\n\n## Tu derecho\n\n- Puedes **solicitar copia** de tu registro horario en cualquier momento.\n- La empresa **no puede negarse** a que tengas acceso a tu propio registro.\n- Si detectas discrepancias, puedes **denunciarlo ante la Inspección de Trabajo** sin coste y, en su caso, de forma anónima.\n- JornadaSaludable conserva tu registro de forma independiente como **prueba complementaria** frente a discrepancias con la empresa.',
 'registro horario,fichaje,4 años,Inspección de Trabajo,LISOS,sanción,RD-Ley 8/2019,Art. 34.9',
 'es-ES', '1.0', '2019-05-12', 10, 1);

-- =============================================================================
--  Fin del esquema
-- =============================================================================
