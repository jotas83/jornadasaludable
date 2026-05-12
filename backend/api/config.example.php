<?php
// Plantilla de configuración. Copiar a `config.php` y rellenar valores reales.
// config.php está en .gitignore.
declare(strict_types=1);

return [
    'debug' => false,

    'db' => [
        'host'    => 'localhost',
        'port'    => 3306,
        'name'    => 'jornadasaludable',
        'user'    => 'CHANGE_ME',
        'pass'    => 'CHANGE_ME',
        // El dump WAMP usa js5_js_; el dump original usa js_.
        'prefix'  => 'js5_js_',
        'charset' => 'utf8mb4',
    ],

    'jwt' => [
        // Generar con: openssl rand -hex 32 (mínimo 32 caracteres).
        'secret'      => 'CHANGE_ME_AT_LEAST_32_HEX_CHARS_FROM_OPENSSL_RAND_HEX_32',
        'algo'        => 'HS256',
        'access_ttl'  => 900,
        'refresh_ttl' => 2592000,
        'issuer'      => 'jornadasaludable',
        'audience'    => 'jornadasaludable-app',
    ],

    'documentos' => [
        'storage_path' => dirname(__DIR__) . '/storage/documentos',
    ],
];
