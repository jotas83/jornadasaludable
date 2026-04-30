<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use DateTimeImmutable;
use DateTimeZone;
use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;
use Ramsey\Uuid\Uuid;

final class PausaController
{
    private const TIPOS_VALIDOS = ['BOCADILLO', 'COMIDA', 'DESCANSO_LEGAL', 'OTROS'];

    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 100)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $where = ['j.user_id = ?'];
        $vals  = [$userId];
        if (!empty($q['jornada_uuid'])) {
            $where[] = 'j.uuid = ?';
            $vals[]  = $q['jornada_uuid'];
        }

        $sql = 'SELECT p.* FROM ' . Db::table('pausas') . ' p
                JOIN ' . Db::table('jornadas') . ' j ON j.id = p.jornada_id
                WHERE ' . implode(' AND ', $where) . '
                ORDER BY p.inicio DESC LIMIT ' . $limit . ' OFFSET ' . $offset;
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);

        Response::ok([
            'items'  => array_map(fn ($r) => $this->mapPausa($r), $stmt->fetchAll()),
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }

    public function create(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $body   = $ctx['body'];

        $accion = strtoupper((string) ($body['accion'] ?? ''));
        if (!in_array($accion, ['INICIO', 'FIN'], true)) {
            Response::error('VALIDATION_ERROR', 'accion debe ser INICIO o FIN.', 422);
        }
        $tipo = strtoupper((string) ($body['tipo'] ?? 'DESCANSO_LEGAL'));
        if (!in_array($tipo, self::TIPOS_VALIDOS, true)) {
            Response::error('VALIDATION_ERROR', 'tipo de pausa inválido.', 422);
        }

        if ($accion === 'INICIO') {
            $this->iniciarPausa($userId, $tipo, $body);
        }
        $this->finalizarPausa($userId, $tipo, $body);
    }

    private function iniciarPausa(int $userId, string $tipo, array $b): never
    {
        $tsRaw = (string) ($b['inicio'] ?? $b['timestamp'] ?? '');
        if ($tsRaw === '') {
            Response::error('VALIDATION_ERROR', 'inicio es obligatorio (ISO 8601).', 422);
        }
        try {
            $dt = new DateTimeImmutable($tsRaw);
        } catch (\Exception) {
            Response::error('VALIDATION_ERROR', 'inicio no es ISO 8601 válido.', 422);
        }
        $fecha   = $dt->format('Y-m-d');
        $tsUtc   = $dt->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s.v');

        $jornada = $this->findJornadaAbierta($userId, $fecha);

        $uuid = $this->normalizeUuid($b['uuid'] ?? null);

        // Idempotencia
        $existing = $this->findByUuid($uuid);
        if ($existing !== null && (int) $existing['jornada_id'] === (int) $jornada['id']) {
            Response::ok(['pausa' => $this->mapPausa($existing), 'jornada' => ['uuid' => $jornada['uuid']], 'idempotent' => true]);
        }

        Db::pdo()->prepare(
            'INSERT INTO ' . Db::table('pausas') . '
             (uuid, jornada_id, tipo, inicio, latitud, longitud, computa_jornada, sync_status)
             VALUES (?, ?, ?, ?, ?, ?, 0, ?)'
        )->execute([
            $uuid, (int) $jornada['id'], $tipo, $tsUtc,
            $b['latitud']  ?? null,
            $b['longitud'] ?? null,
            'SYNCED',
        ]);

        $row = $this->findByUuid($uuid);
        Response::created([
            'pausa'   => $this->mapPausa($row),
            'jornada' => ['uuid' => $jornada['uuid']],
        ]);
    }

    private function finalizarPausa(int $userId, string $tipo, array $b): never
    {
        $tsRaw = (string) ($b['fin'] ?? $b['timestamp'] ?? '');
        if ($tsRaw === '') {
            Response::error('VALIDATION_ERROR', 'fin es obligatorio (ISO 8601).', 422);
        }
        try {
            $dt = new DateTimeImmutable($tsRaw);
        } catch (\Exception) {
            Response::error('VALIDATION_ERROR', 'fin no es ISO 8601 válido.', 422);
        }
        $fecha = $dt->format('Y-m-d');
        $tsUtc = $dt->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s.v');

        // 1) Resolver target por uuid (idempotente)
        $target = null;
        if (!empty($b['uuid'])) {
            $byUuid = $this->findByUuid((string) $b['uuid']);
            if ($byUuid !== null && $this->ownsPausa($userId, $byUuid)) {
                $target = $byUuid;
            }
        }

        // 2) Fallback: última pausa abierta del mismo tipo en la jornada del fecha
        if ($target === null) {
            $jornada = $this->findJornadaAbierta($userId, $fecha);
            $stmt = Db::pdo()->prepare(
                'SELECT * FROM ' . Db::table('pausas') . '
                 WHERE jornada_id = ? AND tipo = ? AND fin IS NULL
                 ORDER BY inicio DESC LIMIT 1'
            );
            $stmt->execute([(int) $jornada['id'], $tipo]);
            $target = $stmt->fetch() ?: null;
            if ($target === null) {
                Response::error('NO_PAUSA_ABIERTA', 'No hay pausa abierta del tipo indicado para cerrar.', 422);
            }
        }

        // Si ya está cerrada → idempotente
        if ($target['fin'] !== null) {
            Response::ok(['pausa' => $this->mapPausa($target), 'idempotent' => true]);
        }

        $minutos = (int) round((strtotime($tsUtc) - strtotime($target['inicio'])) / 60);
        $minutos = max(0, min(65535, $minutos));

        Db::pdo()->prepare(
            'UPDATE ' . Db::table('pausas') . '
             SET fin = ?, minutos = ?, latitud = COALESCE(?, latitud), longitud = COALESCE(?, longitud)
             WHERE id = ?'
        )->execute([
            $tsUtc, $minutos,
            $b['latitud']  ?? null,
            $b['longitud'] ?? null,
            (int) $target['id'],
        ]);

        $this->recalcMinutosPausa((int) $target['jornada_id']);

        $updated = $this->findByUuid($target['uuid']);
        Response::ok(['pausa' => $this->mapPausa($updated)]);
    }

    // ---------- helpers ----------

    private function findJornadaAbierta(int $userId, string $fecha): array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT * FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND fecha = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$userId, $fecha]);
        $j = $stmt->fetch();
        if (!$j) {
            Response::error('NO_JORNADA_ABIERTA', 'No existe jornada para esa fecha. Registra primero un fichaje de ENTRADA.', 422);
        }
        if ($j['estado'] !== 'ABIERTA') {
            Response::error('JORNADA_NO_ABIERTA', 'La jornada no está abierta (estado=' . $j['estado'] . ').', 422);
        }
        return $j;
    }

    private function ownsPausa(int $userId, array $pausa): bool
    {
        $stmt = Db::pdo()->prepare('SELECT user_id FROM ' . Db::table('jornadas') . ' WHERE id = ?');
        $stmt->execute([(int) $pausa['jornada_id']]);
        return (int) $stmt->fetchColumn() === $userId;
    }

    private function recalcMinutosPausa(int $jornadaId): void
    {
        $stmt = Db::pdo()->prepare(
            'SELECT COALESCE(SUM(minutos), 0) FROM ' . Db::table('pausas') . '
             WHERE jornada_id = ? AND computa_jornada = 0 AND fin IS NOT NULL'
        );
        $stmt->execute([$jornadaId]);
        $total = (int) $stmt->fetchColumn();
        $total = max(0, min(65535, $total));
        Db::pdo()
            ->prepare('UPDATE ' . Db::table('jornadas') . ' SET minutos_pausa = ? WHERE id = ?')
            ->execute([$total, $jornadaId]);
    }

    private function findByUuid(string $uuid): ?array
    {
        $stmt = Db::pdo()->prepare('SELECT * FROM ' . Db::table('pausas') . ' WHERE uuid = ? LIMIT 1');
        $stmt->execute([$uuid]);
        return $stmt->fetch() ?: null;
    }

    private function normalizeUuid(mixed $v): string
    {
        $v = is_string($v) ? trim($v) : '';
        if ($v === '' || !preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i', $v)) {
            return Uuid::uuid4()->toString();
        }
        return strtolower($v);
    }

    private function mapPausa(array $r): array
    {
        // duracion_min en vivo si la pausa sigue abierta.
        if ($r['fin'] === null) {
            $duracion = (int) round((time() - strtotime($r['inicio'])) / 60);
        } else {
            $duracion = (int) $r['minutos'];
        }
        return [
            'uuid'         => $r['uuid'],
            'jornada_id'   => (int) $r['jornada_id'],
            'tipo'         => $r['tipo'],
            'inicio'       => $r['inicio'],
            'fin'          => $r['fin'],
            'duracion_min' => $duracion,
            'latitud'      => $r['latitud']  !== null ? (float) $r['latitud']  : null,
            'longitud'     => $r['longitud'] !== null ? (float) $r['longitud'] : null,
            'computa_jornada' => (bool) $r['computa_jornada'],
        ];
    }
}
