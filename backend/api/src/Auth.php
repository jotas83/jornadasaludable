<?php
/**
 * @package JornadaSaludable\Api
 */
declare(strict_types=1);

namespace JornadaSaludable\Api;

use Firebase\JWT\ExpiredException;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Firebase\JWT\SignatureInvalidException;
use RuntimeException;
use Throwable;

/**
 * Middleware JWT. Encapsula firmado/decodificado de tokens y la extracción
 * del Bearer del header Authorization. requireAccessToken() es el guard
 * que llaman los handlers que necesitan usuario autenticado: o devuelve
 * los claims, o termina el request con 401 vía Response::error.
 *
 * Modelo: pareja access (15 min) + refresh (30 días). El refresh se rota
 * en cada uso y se almacena solo como SHA-256 — el JWT en claro nunca toca
 * la BD.
 */
final class Auth
{
    /**
     * Genera la pareja access+refresh para un usuario autenticado.
     *
     * @param  array  $user  Mínimo: id. Opcional: uuid, nif, email.
     * @return array{access_token: string, refresh_token: string, expires_in: int}
     */
    public static function issueTokenPair(array $user): array
    {
        $now = time();
        $cfg = $GLOBALS['JS_CONFIG']['jwt'];

        return [
            'access_token'  => self::encode([
                'sub'   => (int) $user['id'],
                'uuid'  => $user['uuid']  ?? null,
                'nif'   => $user['nif']   ?? null,
                'email' => $user['email'] ?? null,
                'typ'   => 'access',
                'iat'   => $now,
                'exp'   => $now + (int) $cfg['access_ttl'],
            ]),
            'refresh_token' => self::encode([
                'sub' => (int) $user['id'],
                'typ' => 'refresh',
                'iat' => $now,
                'exp' => $now + (int) $cfg['refresh_ttl'],
            ]),
            'expires_in'    => (int) $cfg['access_ttl'],
        ];
    }

    public static function encode(array $payload): string
    {
        $cfg = $GLOBALS['JS_CONFIG']['jwt'];
        $payload += [
            'iss' => $cfg['issuer'],
            'aud' => $cfg['audience'],
        ];
        return JWT::encode($payload, self::secret(), $cfg['algo']);
    }

    /**
     * Decodifica un JWT y devuelve los claims. Lanza ExpiredException o
     * SignatureInvalidException si el token no es válido — el caller decide
     * si las traduce a 401 o las deja burbujear.
     *
     * @return array Claims del JWT
     */
    public static function decode(string $token): array
    {
        $cfg = $GLOBALS['JS_CONFIG']['jwt'];
        $decoded = JWT::decode($token, new Key(self::secret(), $cfg['algo']));
        return (array) $decoded;
    }

    /**
     * Hash determinista del refresh token para almacenarlo en BD sin
     * guardar el JWT en claro. Permite verificación por igualdad
     * (hash_equals) y rotación segura.
     */
    public static function hashRefreshToken(string $token): string
    {
        return hash('sha256', $token);
    }

    /**
     * Extrae el bearer token del header Authorization. Devuelve null si
     * falta o el formato no encaja.
     */
    public static function bearerToken(): ?string
    {
        $header = $_SERVER['HTTP_AUTHORIZATION']
            ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
            ?? '';

        // Apache mod_php: a veces solo está accesible vía apache_request_headers().
        if ($header === '' && \function_exists('apache_request_headers')) {
            $headers = apache_request_headers();
            $header  = $headers['Authorization'] ?? $headers['authorization'] ?? '';
        }

        if (!preg_match('/Bearer\s+(\S+)/i', (string) $header, $m)) {
            return null;
        }
        return $m[1];
    }

    /**
     * Guard para rutas autenticadas. Devuelve los claims si el access token
     * es válido; termina con 401 vía Response::error en cualquier otro caso
     * (sin token, expirado, firma inválida, tipo incorrecto).
     *
     * @return array Claims (sub, uuid, nif, email, typ, iat, exp, iss, aud)
     */
    public static function requireAccessToken(): array
    {
        $token = self::bearerToken();
        if ($token === null) {
            Response::error('AUTH_MISSING_TOKEN', 'Falta la cabecera Authorization: Bearer.', 401);
        }

        try {
            $claims = self::decode($token);
        } catch (ExpiredException) {
            Response::error('AUTH_TOKEN_EXPIRED', 'El token ha caducado.', 401);
        } catch (SignatureInvalidException) {
            Response::error('AUTH_INVALID_TOKEN', 'Firma de token inválida.', 401);
        } catch (Throwable $e) {
            $debug = (bool) ($GLOBALS['JS_CONFIG']['debug'] ?? false);
            Response::error(
                'AUTH_INVALID_TOKEN',
                $debug ? 'Token inválido: ' . $e->getMessage() : 'Token inválido.',
                401
            );
        }

        if (($claims['typ'] ?? null) !== 'access') {
            Response::error('AUTH_INVALID_TOKEN', 'Tipo de token incorrecto (se esperaba access).', 401);
        }

        return $claims;
    }

    private static function secret(): string
    {
        $secret = (string) ($GLOBALS['JS_CONFIG']['jwt']['secret'] ?? '');
        if (\strlen($secret) < 32) {
            throw new RuntimeException(
                'JWT secret no configurado o demasiado corto (mínimo 32 caracteres). Edita config.php.'
            );
        }
        return $secret;
    }
}
