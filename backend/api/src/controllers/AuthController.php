<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use Firebase\JWT\ExpiredException;
use Firebase\JWT\SignatureInvalidException;
use JornadaSaludable\Api\Auth;
use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;
use Throwable;

final class AuthController
{
    /**
     * Hash bcrypt no resoluble. Anti-timing: password_verify se ejecuta
     * siempre, evitando enumeración de usuarios.
     * Generado offline con: password_hash(random_bytes(32), PASSWORD_BCRYPT, ['cost' => 12])
     */
    private const DUMMY_HASH = '$2y$12$5QZJk9YNhVRSCkBh/ehTwO9pCB1vRcfX0ZjV3VGtJ8VXDJSsU8tya';

    public function login(array $ctx): void
    {
        $body       = $ctx['body'];
        $identifier = trim((string) ($body['nif'] ?? $body['email'] ?? $body['identifier'] ?? ''));
        $password   = (string) ($body['password'] ?? '');
        $deviceId   = isset($body['device_id'])  ? (string) $body['device_id']  : null;
        $pushToken  = isset($body['push_token']) ? (string) $body['push_token'] : null;

        if ($identifier === '' || $password === '') {
            Response::error('VALIDATION_ERROR', 'Faltan credenciales: nif (o email) y password son obligatorios.', 422);
        }

        $user = $this->findByEmailOrNif($identifier);
        $hash = ($user !== null && !empty($user['password_hash']))
            ? (string) $user['password_hash']
            : self::DUMMY_HASH;

        $passwordOk = password_verify($password, $hash);

        if ($user === null || !$passwordOk) {
            Response::error('AUTH_INVALID_CREDENTIALS', 'Credenciales inválidas.', 401);
        }
        if ((int) $user['activo'] !== 1 || $user['deleted_at'] !== null) {
            Response::error('AUTH_ACCOUNT_DISABLED', 'Cuenta desactivada.', 403);
        }

        $tokens = Auth::issueTokenPair($user);
        $this->persistRefreshAndSession((int) $user['id'], $tokens['refresh_token'], $deviceId, $pushToken);

        Response::ok([
            'access_token'  => $tokens['access_token'],
            'refresh_token' => $tokens['refresh_token'],
            'token_type'    => 'Bearer',
            'expires_in'    => $tokens['expires_in'],
            'user'          => $this->publicUser($user),
        ]);
    }

    public function refresh(array $ctx): void
    {
        $refreshToken = trim((string) ($ctx['body']['refresh_token'] ?? ''));
        if ($refreshToken === '') {
            Response::error('VALIDATION_ERROR', 'Falta refresh_token en el cuerpo.', 422);
        }

        try {
            $claims = Auth::decode($refreshToken);
        } catch (ExpiredException) {
            Response::error('AUTH_TOKEN_EXPIRED', 'El token ha caducado.', 401);
        } catch (SignatureInvalidException) {
            Response::error('AUTH_INVALID_TOKEN', 'Firma de token inválida.', 401);
        } catch (Throwable) {
            Response::error('AUTH_INVALID_TOKEN', 'Token inválido.', 401);
        }

        if (($claims['typ'] ?? null) !== 'refresh') {
            Response::error('AUTH_INVALID_TOKEN', 'Tipo de token incorrecto (se esperaba refresh).', 401);
        }
        $userId = (int) ($claims['sub'] ?? 0);
        if ($userId <= 0) {
            Response::error('AUTH_INVALID_TOKEN', 'Token sin sujeto válido.', 401);
        }

        $user = $this->findById($userId);
        if ($user === null) {
            Response::error('AUTH_INVALID_TOKEN', 'Usuario no encontrado.', 401);
        }
        if ((int) $user['activo'] !== 1 || $user['deleted_at'] !== null) {
            Response::error('AUTH_ACCOUNT_DISABLED', 'Cuenta desactivada.', 403);
        }

        $stored    = (string) ($user['jwt_refresh_token'] ?? '');
        $presented = Auth::hashRefreshToken($refreshToken);
        if ($stored === '' || !hash_equals($stored, $presented)) {
            // Posible reuso o robo: revocar todo.
            $this->revokeRefresh($userId);
            Response::error('AUTH_TOKEN_REUSE', 'Refresh token inválido o reutilizado.', 401);
        }

        $tokens = Auth::issueTokenPair($user);
        $this->persistRefreshAndSession($userId, $tokens['refresh_token'], null, null);

        Response::ok([
            'access_token'  => $tokens['access_token'],
            'refresh_token' => $tokens['refresh_token'],
            'token_type'    => 'Bearer',
            'expires_in'    => $tokens['expires_in'],
            'user'          => $this->publicUser($user),
        ]);
    }

    public function me(array $ctx): void
    {
        Response::ok(['claims' => $ctx['auth']]);
    }

    public function logout(array $ctx): void
    {
        $userId = (int) ($ctx['auth']['sub'] ?? 0);
        if ($userId > 0) {
            $this->revokeRefresh($userId);
        }
        Response::noContent();
    }

    // ---------- DAL ----------

    private function findByEmailOrNif(string $val): ?array
    {
        $row = $this->findOneBy('email', $val);
        return $row ?? $this->findOneBy('nif', $val);
    }

    private function findOneBy(string $col, string $val): ?array
    {
        $sql = 'SELECT id, uuid, nif, nombre, apellidos, email, idioma, password_hash, activo, deleted_at
                FROM ' . Db::table('users') . ' WHERE ' . $col . ' = ? LIMIT 1';
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute([$val]);
        return $stmt->fetch() ?: null;
    }

    private function findById(int $id): ?array
    {
        $sql = 'SELECT id, uuid, nif, nombre, apellidos, email, idioma, jwt_refresh_token, activo, deleted_at
                FROM ' . Db::table('users') . ' WHERE id = ? LIMIT 1';
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute([$id]);
        return $stmt->fetch() ?: null;
    }

    private function persistRefreshAndSession(int $userId, string $refresh, ?string $deviceId, ?string $pushToken): void
    {
        $hash = Auth::hashRefreshToken($refresh);
        $now  = gmdate('Y-m-d H:i:s');
        $sets = ['jwt_refresh_token = ?', 'last_login_at = ?'];
        $vals = [$hash, $now];
        if ($deviceId !== null && $deviceId !== '') {
            $sets[] = 'device_id = ?';
            $vals[] = $deviceId;
        }
        if ($pushToken !== null && $pushToken !== '') {
            $sets[] = 'push_token = ?';
            $vals[] = $pushToken;
        }
        $vals[] = $userId;
        $sql = 'UPDATE ' . Db::table('users') . ' SET ' . implode(', ', $sets) . ' WHERE id = ?';
        Db::pdo()->prepare($sql)->execute($vals);
    }

    private function revokeRefresh(int $userId): void
    {
        Db::pdo()
            ->prepare('UPDATE ' . Db::table('users') . ' SET jwt_refresh_token = NULL WHERE id = ?')
            ->execute([$userId]);
    }

    private function publicUser(array $u): array
    {
        return [
            'id'        => (int) $u['id'],
            'uuid'      => $u['uuid'],
            'nif'       => $u['nif'],
            'nombre'    => $u['nombre'],
            'apellidos' => $u['apellidos'],
            'email'     => $u['email'],
            'idioma'    => $u['idioma'],
        ];
    }
}
