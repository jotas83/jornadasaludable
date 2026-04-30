<?php
/**
 * Plantilla de configuración. Copiar a `config.php` y rellenar con valores
 * reales antes de arrancar la API.
 *
 *     cp config.example.php config.php
 *     # editar config.php con tus credenciales
 *
 * `config.php` está en .gitignore y NO debe subirse al repositorio.
 */
declare(strict_types=1);

return [
    // En producción debe ser false: oculta mensajes de error y traces.
    'debug' => false,

    'db' => [
        'host'    => 'localhost',
        'port'    => 3306,
        'name'    => 'jornadasaludable',
        'user'    => 'CHANGE_ME',
        'pass'    => 'CHANGE_ME',
        // Prefijo aplicado a TODAS las tablas. El dump WAMP usa js5_js_;
        // ajustar a 'js_' si se usa el dump original.
        'prefix'  => 'js5_js_',
        'charset' => 'utf8mb4',
    ],

    'jwt' => [
        // Generar con: openssl rand -hex 32
        // Mínimo 32 caracteres (Auth::secret() lanza RuntimeException si no).
        'secret'      => 'CHANGE_ME_AT_LEAST_32_HEX_CHARS_FROM_OPENSSL_RAND_HEX_32',
        'algo'        => 'HS256',
        'access_ttl'  => 900,      // 15 min
        'refresh_ttl' => 2592000,  // 30 días
        'issuer'      => 'jornadasaludable',
        'audience'    => 'jornadasaludable-app',
    ],

    'documentos' => [
        // Ruta absoluta donde se almacenan los PDF generados.
        'storage_path' => dirname(__DIR__) . '/storage/documentos',
    ],
];
