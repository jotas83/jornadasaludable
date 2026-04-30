<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;
use Ramsey\Uuid\Uuid;

final class HorasExtraController
{
    private const TIPOS_VALIDOS         = ['ORDINARIA', 'FESTIVA'];
    private const COMPENSACION_VALIDOS  = ['ECONOMICA', 'DESCANSO', 'PENDIENTE'];
    private const MAX_MINUTOS           = 720;

    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 100)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $where = ['he.user_id = ?', 'he.deleted_at IS NULL'];
        $vals  = [$userId];
        if (!empty($q['mes'])) {
            if (!preg_match('/^\d{4}-\d{2}$/', (string) $q['mes'])) {
                Response::error('VALIDATION_ERROR', 'Parámetro "mes" debe tener formato YYYY-MM.', 422);
            }
            $where[] = 'DATE_FORMAT(he.created_at, "%Y-%m") = ?';
            $vals[]  = $q['mes'];
        }

        $sql = 'SELECT he.*, j.uuid AS jornada_uuid, j.fecha AS jornada_fecha
                FROM ' . Db::table('horas_extra') . ' he
                JOIN ' . Db::table('jornadas') . ' j ON j.id = he.jornada_id
                WHERE ' . implode(' AND ', $where) . '
                ORDER BY he.created_at DESC LIMIT ' . $limit . ' OFFSET ' . $offset;
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);

        Response::ok([
            'items'  => array_map(fn ($r) => $this->mapHora($r), $stmt->fetchAll()),
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }

    public function create(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $body   = $ctx['body'];

        $jornadaUuid = (string) ($body['jornada_uuid'] ?? '');
        if ($jornadaUuid === '') {
            Response::error('VALIDATION_ERROR', 'jornada_uuid es obligatorio.', 422);
        }
        $minutos = (int) ($body['minutos'] ?? 0);
        if ($minutos < 1 || $minutos > self::MAX_MINUTOS) {
            Response::error('VALIDATION_ERROR', sprintf('minutos debe estar entre 1 y %d.', self::MAX_MINUTOS), 422);
        }
        $tipo = strtoupper((string) ($body['tipo'] ?? 'ORDINARIA'));
        if (!in_array($tipo, self::TIPOS_VALIDOS, true)) {
            Response::error('VALIDATION_ERROR', 'tipo debe ser ORDINARIA o FESTIVA.', 422);
        }
        $compensacion = strtoupper((string) ($body['compensacion'] ?? 'PENDIENTE'));
        if (!in_array($compensacion, self::COMPENSACION_VALIDOS, true)) {
            Response::error('VALIDATION_ERROR', 'compensacion debe ser ECONOMICA, DESCANSO o PENDIENTE.', 422);
        }

        $stmt = Db::pdo()->prepare(
            'SELECT id, user_id FROM ' . Db::table('jornadas') . ' WHERE uuid = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$jornadaUuid]);
        $jornada = $stmt->fetch();
        if (!$jornada) {
            Response::error('NOT_FOUND', 'Jornada no encontrada.', 404);
        }
        if ((int) $jornada['user_id'] !== $userId) {
            Response::error('FORBIDDEN', 'La jornada no pertenece al usuario autenticado.', 403);
        }

        $uuid = $this->normalizeUuid($body['uuid'] ?? null);

        // Idempotencia
        $stmt = Db::pdo()->prepare('SELECT * FROM ' . Db::table('horas_extra') . ' WHERE uuid = ? LIMIT 1');
        $stmt->execute([$uuid]);
        $existing = $stmt->fetch();
        if ($existing !== false && (int) $existing['user_id'] === $userId) {
            Response::ok(['horas_extra' => $this->mapHora($existing), 'idempotent' => true]);
        }

        Db::pdo()->prepare(
            'INSERT INTO ' . Db::table('horas_extra') . '
             (uuid, jornada_id, user_id, minutos, tipo, compensacion, estado,
              importe_bruto, descanso_minutos, aceptada_trabajador, observaciones)
             VALUES (?, ?, ?, ?, ?, ?, "PENDIENTE", ?, ?, 1, ?)'
        )->execute([
            $uuid, (int) $jornada['id'], $userId, $minutos, $tipo, $compensacion,
            $body['importe_bruto']    ?? null,
            $body['descanso_minutos'] ?? null,
            ($body['observaciones']   ?? '') ?: null,
        ]);

        $this->recalcMinutosExtra((int) $jornada['id']);

        $stmt = Db::pdo()->prepare('SELECT * FROM ' . Db::table('horas_extra') . ' WHERE uuid = ? LIMIT 1');
        $stmt->execute([$uuid]);
        $created = $stmt->fetch();

        // Recargar jornada
        $stmt = Db::pdo()->prepare('SELECT uuid, fecha, minutos_trabajados, minutos_pausa, minutos_extra, estado FROM ' . Db::table('jornadas') . ' WHERE id = ?');
        $stmt->execute([(int) $jornada['id']]);
        $j = $stmt->fetch();

        Response::created([
            'horas_extra' => $this->mapHora($created),
            'jornada'     => $j,
        ]);
    }

    private function recalcMinutosExtra(int $jornadaId): void
    {
        $stmt = Db::pdo()->prepare(
            'SELECT COALESCE(SUM(minutos), 0) FROM ' . Db::table('horas_extra') . '
             WHERE jornada_id = ? AND deleted_at IS NULL'
        );
        $stmt->execute([$jornadaId]);
        $total = (int) $stmt->fetchColumn();
        $total = max(0, min(65535, $total));
        Db::pdo()
            ->prepare('UPDATE ' . Db::table('jornadas') . ' SET minutos_extra = ? WHERE id = ?')
            ->execute([$total, $jornadaId]);
    }

    private function normalizeUuid(mixed $v): string
    {
        $v = is_string($v) ? trim($v) : '';
        if ($v === '' || !preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i', $v)) {
            return Uuid::uuid4()->toString();
        }
        return strtolower($v);
    }

    private function mapHora(array $r): array
    {
        return [
            'uuid'              => $r['uuid'],
            'jornada_id'        => (int) $r['jornada_id'],
            'jornada_uuid'      => $r['jornada_uuid'] ?? null,
            'jornada_fecha'     => $r['jornada_fecha'] ?? null,
            'minutos'           => (int) $r['minutos'],
            'tipo'              => $r['tipo'],
            'compensacion'      => $r['compensacion'],
            'estado'            => $r['estado'],
            'importe_bruto'     => $r['importe_bruto'] !== null ? (float) $r['importe_bruto'] : null,
            'descanso_minutos'  => $r['descanso_minutos'] !== null ? (int) $r['descanso_minutos'] : null,
            'aceptada_trabajador' => (bool) $r['aceptada_trabajador'],
            'observaciones'     => $r['observaciones'],
            'created_at'        => $r['created_at'],
        ];
    }
}
