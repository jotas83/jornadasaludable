<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use DateTimeImmutable;
use DateTimeZone;
use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;
use PDOException;
use Ramsey\Uuid\Uuid;
use Throwable;

final class FichajeController
{
    public function create(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $body   = $ctx['body'];

        [$tipo, $tsUtc, $fechaJornada] = $this->validateFichajeInput($body);

        $uuid = $this->normalizeUuid($body['uuid'] ?? null);

        // Idempotencia por uuid
        $existing = $this->findByUuid($uuid);
        if ($existing !== null && (int) $existing['user_id'] === $userId) {
            $jornada = $this->findJornadaById((int) $existing['jornada_id']);
            Response::ok([
                'fichaje'    => $this->mapFichaje($existing),
                'jornada'    => $jornada ? $this->mapJornada($jornada) : null,
                'idempotent' => true,
            ]);
        }

        // Empresa derivada del contrato vigente + verificación licencia
        $empresaId  = $this->deriveEmpresaIdOrFail($userId);
        $contratoId = $this->findContratoVigenteId($userId);

        $jornadaId = $this->findOrCreateJornada($userId, $fechaJornada, $contratoId);

        Db::pdo()->prepare(
            'INSERT INTO ' . Db::table('fichajes') . '
             (uuid, jornada_id, user_id, empresa_id, tipo, timestamp_evento, latitud, longitud,
              precision_gps_m, dentro_geofence, metodo, device_id, observaciones, sync_status, synced_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())'
        )->execute([
            $uuid, $jornadaId, $userId, $empresaId, $tipo, $tsUtc,
            $body['latitud']  ?? null,
            $body['longitud'] ?? null,
            $body['precision_gps_m'] ?? null,
            isset($body['dentro_geofence']) ? (int) (bool) $body['dentro_geofence'] : null,
            (string) ($body['metodo']    ?? 'MANUAL'),
            ($body['device_id']     ?? '') ?: null,
            ($body['observaciones'] ?? '') ?: null,
            'SYNCED',
        ]);

        $this->recalcJornada($jornadaId);

        $fichaje = $this->findByUuid($uuid);
        $jornada = $this->findJornadaById($jornadaId);
        Response::created([
            'fichaje' => $this->mapFichaje($fichaje),
            'jornada' => $jornada ? $this->mapJornada($jornada) : null,
        ]);
    }

    public function sync(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $items  = $ctx['body']['fichajes'] ?? null;
        if (!is_array($items)) {
            Response::error('VALIDATION_ERROR', 'Cuerpo debe contener un array "fichajes".', 422);
        }

        // Verificación empresa/licencia una sola vez (aborta el batch entero si falla).
        $empresaId  = $this->deriveEmpresaIdOrFail($userId);
        $contratoId = $this->findContratoVigenteId($userId);

        $results            = [];
        $jornadasAfectadas  = [];
        foreach ($items as $i => $item) {
            try {
                if (!is_array($item)) {
                    throw new \RuntimeException('item no es objeto');
                }
                [$tipo, $tsUtc, $fecha] = $this->validateFichajeInput($item);

                $uuid     = $this->normalizeUuid($item['uuid'] ?? null);
                $existing = $this->findByUuid($uuid);
                if ($existing !== null && (int) $existing['user_id'] === $userId) {
                    $results[] = ['index' => $i, 'uuid' => $uuid, 'status' => 'duplicate'];
                    $jornadasAfectadas[(int) $existing['jornada_id']] = true;
                    continue;
                }

                $jornadaId = $this->findOrCreateJornada($userId, $fecha, $contratoId);

                Db::pdo()->prepare(
                    'INSERT INTO ' . Db::table('fichajes') . '
                     (uuid, jornada_id, user_id, empresa_id, tipo, timestamp_evento, latitud, longitud,
                      precision_gps_m, dentro_geofence, metodo, device_id, observaciones, sync_status, synced_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())'
                )->execute([
                    $uuid, $jornadaId, $userId, $empresaId, $tipo, $tsUtc,
                    $item['latitud']  ?? null,
                    $item['longitud'] ?? null,
                    $item['precision_gps_m'] ?? null,
                    isset($item['dentro_geofence']) ? (int) (bool) $item['dentro_geofence'] : null,
                    (string) ($item['metodo']    ?? 'MANUAL'),
                    ($item['device_id']     ?? '') ?: null,
                    ($item['observaciones'] ?? '') ?: null,
                    'SYNCED',
                ]);
                $jornadasAfectadas[$jornadaId] = true;
                $results[] = ['index' => $i, 'uuid' => $uuid, 'status' => 'created'];
            } catch (Throwable $e) {
                $results[] = [
                    'index'   => $i,
                    'uuid'    => $item['uuid'] ?? null,
                    'status'  => 'error',
                    'message' => $e->getMessage(),
                ];
            }
        }

        // Recalc batched al final, una vez por jornada.
        foreach (array_keys($jornadasAfectadas) as $jId) {
            $this->recalcJornada((int) $jId);
        }

        Response::ok([
            'results'           => $results,
            'jornadas_afectadas' => count($jornadasAfectadas),
        ]);
    }

    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 100)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $where = ['user_id = ?', 'deleted_at IS NULL'];
        $vals  = [$userId];
        if (!empty($q['fecha_inicio'])) {
            $where[] = 'timestamp_evento >= ?';
            $vals[]  = $q['fecha_inicio'] . ' 00:00:00';
        }
        if (!empty($q['fecha_fin'])) {
            $where[] = 'timestamp_evento <= ?';
            $vals[]  = $q['fecha_fin'] . ' 23:59:59.999';
        }

        $sql = 'SELECT * FROM ' . Db::table('fichajes') . '
                WHERE ' . implode(' AND ', $where) . '
                ORDER BY timestamp_evento DESC LIMIT ' . $limit . ' OFFSET ' . $offset;
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);
        $rows = $stmt->fetchAll();

        Response::ok([
            'items'  => array_map(fn ($r) => $this->mapFichaje($r), $rows),
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }

    // ---------- helpers ----------

    /**
     * @return array{0: string, 1: string, 2: string} [tipo, tsUtc, fechaJornadaLocal]
     */
    private function validateFichajeInput(array $b): array
    {
        $tipo = strtoupper((string) ($b['tipo'] ?? ''));
        if (!in_array($tipo, ['ENTRADA', 'SALIDA'], true)) {
            Response::error('VALIDATION_ERROR', 'tipo debe ser ENTRADA o SALIDA.', 422);
        }
        $ts = (string) ($b['timestamp_evento'] ?? $b['timestamp'] ?? '');
        if ($ts === '') {
            Response::error('VALIDATION_ERROR', 'timestamp_evento es obligatorio (ISO 8601).', 422);
        }
        try {
            $dt = new DateTimeImmutable($ts);
        } catch (\Exception) {
            Response::error('VALIDATION_ERROR', 'timestamp_evento no es ISO 8601 válido.', 422);
        }
        // Fecha de jornada: en la zona horaria del cliente (preserva offset).
        $fechaJornada = $dt->format('Y-m-d');
        $tsUtc        = $dt->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d H:i:s.v');
        return [$tipo, $tsUtc, $fechaJornada];
    }

    private function deriveEmpresaIdOrFail(int $userId): int
    {
        $sql = 'SELECT empresa_id FROM ' . Db::table('contratos') . '
                WHERE user_id = ? AND vigente = 1
                  AND fecha_inicio <= CURDATE()
                  AND (fecha_fin IS NULL OR fecha_fin >= CURDATE())
                ORDER BY fecha_inicio DESC LIMIT 1';
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute([$userId]);
        $eid = $stmt->fetchColumn();
        if ($eid === false) {
            Response::error('NO_VIGENTE_CONTRATO', 'No hay contrato vigente para el usuario.', 403);
        }

        $stmt = Db::pdo()->prepare('SELECT activo FROM ' . Db::table('empresas') . ' WHERE id = ?');
        $stmt->execute([$eid]);
        if ((int) $stmt->fetchColumn() !== 1) {
            Response::error('EMPRESA_INACTIVE', 'Empresa inactiva.', 403);
        }

        $stmt = Db::pdo()->prepare(
            'SELECT 1 FROM ' . Db::table('licencias') . '
             WHERE empresa_id = ? AND activa = 1
               AND fecha_inicio <= CURDATE()
               AND (fecha_fin IS NULL OR fecha_fin >= CURDATE())
             LIMIT 1'
        );
        $stmt->execute([$eid]);
        if ($stmt->fetchColumn() === false) {
            Response::error('LICENCIA_EXPIRED', 'No hay licencia vigente para la empresa.', 403);
        }
        return (int) $eid;
    }

    private function findContratoVigenteId(int $userId): ?int
    {
        $sql = 'SELECT id FROM ' . Db::table('contratos') . '
                WHERE user_id = ? AND vigente = 1
                  AND fecha_inicio <= CURDATE()
                  AND (fecha_fin IS NULL OR fecha_fin >= CURDATE())
                ORDER BY fecha_inicio DESC LIMIT 1';
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute([$userId]);
        $r = $stmt->fetchColumn();
        return $r === false ? null : (int) $r;
    }

    private function findOrCreateJornada(int $userId, string $fecha, ?int $contratoId): int
    {
        $sql = 'SELECT id FROM ' . Db::table('jornadas') . ' WHERE user_id = ? AND fecha = ? LIMIT 1';
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute([$userId, $fecha]);
        $r = $stmt->fetchColumn();
        if ($r !== false) {
            return (int) $r;
        }

        try {
            Db::pdo()->prepare(
                'INSERT INTO ' . Db::table('jornadas') . '
                 (uuid, user_id, contrato_id, fecha, sync_status, synced_at)
                 VALUES (?, ?, ?, ?, ?, NOW())'
            )->execute([Uuid::uuid4()->toString(), $userId, $contratoId, $fecha, 'SYNCED']);
            return (int) Db::pdo()->lastInsertId();
        } catch (PDOException) {
            // Race: re-fetch
            $stmt = Db::pdo()->prepare($sql);
            $stmt->execute([$userId, $fecha]);
            return (int) $stmt->fetchColumn();
        }
    }

    /**
     * Recorre fichajes de la jornada en orden cronológico, empareja
     * ENTRADA→SALIDA y actualiza minutos_trabajados / hora_inicio /
     * hora_fin / estado.
     */
    private function recalcJornada(int $jornadaId): void
    {
        $stmt = Db::pdo()->prepare(
            'SELECT tipo, timestamp_evento FROM ' . Db::table('fichajes') . '
             WHERE jornada_id = ? AND deleted_at IS NULL
             ORDER BY timestamp_evento ASC'
        );
        $stmt->execute([$jornadaId]);
        $rows = $stmt->fetchAll();

        $minutos      = 0;
        $entrada      = null;
        $primera      = null;
        $ultimaSalida = null;
        foreach ($rows as $r) {
            if ($r['tipo'] === 'ENTRADA') {
                if ($entrada === null) {
                    $entrada = $r['timestamp_evento'];
                    if ($primera === null) {
                        $primera = $entrada;
                    }
                }
            } elseif ($r['tipo'] === 'SALIDA' && $entrada !== null) {
                $minutos      += (int) round((strtotime($r['timestamp_evento']) - strtotime($entrada)) / 60);
                $ultimaSalida  = $r['timestamp_evento'];
                $entrada       = null;
            }
        }
        $estado = $entrada !== null ? 'ABIERTA' : 'CERRADA';

        Db::pdo()->prepare(
            'UPDATE ' . Db::table('jornadas') . '
             SET hora_inicio = ?, hora_fin = ?, minutos_trabajados = ?, estado = ?
             WHERE id = ?'
        )->execute([
            $primera, $ultimaSalida, max(0, min(65535, $minutos)),
            $estado, $jornadaId,
        ]);
    }

    private function findByUuid(string $uuid): ?array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT * FROM ' . Db::table('fichajes') . ' WHERE uuid = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$uuid]);
        return $stmt->fetch() ?: null;
    }

    private function findJornadaById(int $id): ?array
    {
        $stmt = Db::pdo()->prepare('SELECT * FROM ' . Db::table('jornadas') . ' WHERE id = ?');
        $stmt->execute([$id]);
        return $stmt->fetch() ?: null;
    }

    private function normalizeUuid(mixed $val): string
    {
        $v = is_string($val) ? trim($val) : '';
        if ($v === '' || !preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i', $v)) {
            return Uuid::uuid4()->toString();
        }
        return strtolower($v);
    }

    private function mapFichaje(array $r): array
    {
        return [
            'uuid'             => $r['uuid'],
            'jornada_id'       => (int) $r['jornada_id'],
            'tipo'             => $r['tipo'],
            'timestamp_evento' => $r['timestamp_evento'],
            'latitud'          => $r['latitud']  !== null ? (float) $r['latitud']  : null,
            'longitud'         => $r['longitud'] !== null ? (float) $r['longitud'] : null,
            'metodo'           => $r['metodo'],
            'sync_status'      => $r['sync_status'],
        ];
    }

    private function mapJornada(array $r): array
    {
        return [
            'uuid'               => $r['uuid'],
            'fecha'              => $r['fecha'],
            'hora_inicio'        => $r['hora_inicio'],
            'hora_fin'           => $r['hora_fin'],
            'minutos_trabajados' => (int) $r['minutos_trabajados'],
            'minutos_pausa'      => (int) $r['minutos_pausa'],
            'minutos_extra'      => (int) $r['minutos_extra'],
            'estado'             => $r['estado'],
        ];
    }
}
