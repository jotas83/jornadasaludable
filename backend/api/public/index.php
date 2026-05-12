<?php
// Front controller de la API. Todas las peticiones caen aquí vía .htaccess.
declare(strict_types=1);

// Bootstrap: autoload + config

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

// Defaults globales
date_default_timezone_set('UTC');
mb_internal_encoding('UTF-8');

// Excepciones no capturadas → 500 con JSON. En debug incluye trace.
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

// Path relativo a la API
$baseDir = rtrim(str_replace('\\', '/', \dirname($_SERVER['SCRIPT_NAME'] ?? '/')), '/');
$uriPath = (string) parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path    = ltrim((string) substr($uriPath, \strlen($baseDir)), '/');

// El Router trabaja con paths sin versión; aquí desviaríamos a v2 cuando exista.
if ($path === 'v1' || $path === 'v1/') {
    $path = '';
} elseif (str_starts_with($path, 'v1/')) {
    $path = substr($path, 3);
}

$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

// CORS preflight (útil para Postman/navegador en desarrollo)
if ($method === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Authorization, Content-Type');
    header('Access-Control-Max-Age: 86400');
    http_response_code(204);
    exit;
}

(new \JornadaSaludable\Api\Router())->dispatch($path, $method);
