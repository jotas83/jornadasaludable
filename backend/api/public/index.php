<?php
/**
 * Front controller único de la API JornadaSaludable.
 *
 * Toda petición a /jornadasaludable/api/... cae aquí vía .htaccess. El path
 * de la API se calcula como REQUEST_URI menos la carpeta del script, lo que
 * lo hace robusto a despliegues con o sin Alias de Apache.
 *
 *   URL:           http://localhost/jornadasaludable/api/v1/auth/login
 *   SCRIPT_NAME:   /jornadasaludable/api/index.php  (con Alias)
 *                  o /jornadasaludable/api/public/index.php (sin Alias)
 *   path final:    v1/auth/login
 */
declare(strict_types=1);

// -----------------------------------------------------------------------------
//  Bootstrap: Composer autoload + config
// -----------------------------------------------------------------------------

$autoload = __DIR__ . '/../vendor/autoload.php';
if (!is_file($autoload)) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['error' => [
        'code'    => 'BOOTSTRAP_FAILED',
        'message' => 'Falta vendor/autoload.php — ejecuta `composer install` en backend/api/.',
    ]]);
    exit;
}
require_once $autoload;

$configFile = __DIR__ . '/../config.php';
if (!is_file($configFile)) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['error' => [
        'code'    => 'BOOTSTRAP_FAILED',
        'message' => 'Falta backend/api/config.php.',
    ]]);
    exit;
}
$GLOBALS['JS_CONFIG'] = require $configFile;

// -----------------------------------------------------------------------------
//  Defaults globales
// -----------------------------------------------------------------------------

date_default_timezone_set('UTC');
mb_internal_encoding('UTF-8');

// Excepciones no capturadas → 500 con cuerpo JSON. En debug incluye trace.
set_exception_handler(static function (\Throwable $e): void {
    $debug = (bool) ($GLOBALS['JS_CONFIG']['debug'] ?? false);
    if (!headers_sent()) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
    }
    echo json_encode(['error' => array_filter([
        'code'    => 'INTERNAL_ERROR',
        'message' => $debug ? $e->getMessage() : 'Error interno del servidor.',
        'trace'   => $debug ? explode("\n", $e->getTraceAsString()) : null,
    ], static fn ($v) => $v !== null)]);
});

// -----------------------------------------------------------------------------
//  Cálculo del path relativo a la API
// -----------------------------------------------------------------------------

$baseDir = rtrim(str_replace('\\', '/', \dirname($_SERVER['SCRIPT_NAME'] ?? '/')), '/');
$uriPath = (string) parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path    = ltrim((string) substr($uriPath, \strlen($baseDir)), '/');

// Strip del prefijo de versión: el versionado vive en la URL pública
// (/jornadasaludable/api/v1/...) pero el Router trabaja con paths neutros
// (health, auth/login, fichajes/...). Cuando salga v2, este punto es donde
// se ramifica a un Router distinto en función del prefijo.
if ($path === 'v1' || $path === 'v1/') {
    $path = '';
} elseif (str_starts_with($path, 'v1/')) {
    $path = substr($path, 3);
}

$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

// -----------------------------------------------------------------------------
//  CORS preflight (preventivo: el cliente Android no lo necesita, pero útil
//  para Postman/browser desde otro origen durante desarrollo).
// -----------------------------------------------------------------------------

if ($method === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Authorization, Content-Type');
    header('Access-Control-Max-Age: 86400');
    http_response_code(204);
    exit;
}

// -----------------------------------------------------------------------------
//  Dispatch
// -----------------------------------------------------------------------------

(new \JornadaSaludable\Api\Router())->dispatch($path, $method);
