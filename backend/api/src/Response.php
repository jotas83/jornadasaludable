<?php
/**
 * @package JornadaSaludable\Api
 */
declare(strict_types=1);

namespace JornadaSaludable\Api;

/**
 * Helpers de respuesta JSON. Todos los métodos terminan la ejecución con
 * exit; — están declarados : never para que PHP exija que el flujo no
 * continúe tras llamarlos.
 */
final class Response
{
    public static function ok(mixed $data, int $status = 200): never
    {
        self::send($status, ['data' => $data]);
    }

    public static function created(mixed $data): never
    {
        self::send(201, ['data' => $data]);
    }

    public static function noContent(): never
    {
        self::send(204, null);
    }

    /**
     * Error con formato uniforme.
     *
     *   { "error": { "code": "AUTH_INVALID_TOKEN", "message": "...", "details": {...} } }
     */
    public static function error(string $code, string $message, int $status = 400, array $details = []): never
    {
        self::send($status, ['error' => array_filter([
            'code'    => $code,
            'message' => $message,
            'details' => $details ?: null,
        ], static fn ($v) => $v !== null)]);
    }

    private static function send(int $status, mixed $body): never
    {
        if (!headers_sent()) {
            http_response_code($status);
            header('Content-Type: application/json; charset=utf-8');
            header('Cache-Control: no-store');
        }
        if ($body !== null) {
            echo json_encode($body, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        }
        exit;
    }
}
