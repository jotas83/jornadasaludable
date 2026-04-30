<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;

final class JornadaController
{
    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 60)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $where = ['user_id = ?', 'deleted_at IS NULL'];
        $vals  = [$userId];
        if (!empty($q['mes'])) {
            // Formato esperado: YYYY-MM
            if (!preg_match('/^\d{4}-\d{2}$/', (string) $q['mes'])) {
                Response::error('VALIDATION_ERROR', 'Parámetro "mes" debe tener formato YYYY-MM.', 422);
            }
            $where[] = 'fecha LIKE ?';
            $vals[]  = $q['mes'] . '-%';
        }

        $sql = 'SELECT * FROM ' . Db::table('jornadas') . '
                WHERE ' . implode(' AND ', $where) . '
                ORDER BY fecha DESC LIMIT ' . $limit . ' OFFSET ' . $offset;
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);
        $rows = $stmt->fetchAll();

        Response::ok([
            'items'  => array_map(fn ($r) => $this->mapJornada($r), $rows),
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }

    public function resumen(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $hoy    = new \DateTimeImmutable('today');

        $weekStart  = $hoy->modify('monday this week')->format('Y-m-d');
        $monthStart = $hoy->format('Y-m-01');
        $yearStart  = $hoy->format('Y-01-01');

        $semana = $this->aggregate($userId, $weekStart, $hoy->format('Y-m-d'));
        $mes    = $this->aggregate($userId, $monthStart, $hoy->format('Y-m-d'));
        $anio   = $this->aggregate($userId, $yearStart, $hoy->format('Y-m-d'));

        // Jornadas incompletas SOLO en mes (Art. 12 RD-Ley 8/2019: actionable corto plazo).
        $stmt = Db::pdo()->prepare(
            'SELECT COUNT(*) FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha BETWEEN ? AND ?
               AND estado = "ABIERTA" AND fecha < CURDATE()'
        );
        $stmt->execute([$userId, $monthStart, $hoy->format('Y-m-d')]);
        $incompletas = (int) $stmt->fetchColumn();

        // Horas contrato (aproximación lineal)
        $contrato = $this->loadContratoVigente($userId);
        $horasContrato = $this->calcHorasContrato($contrato, $hoy);

        Response::ok([
            'semana_actual' => array_merge($semana, ['horas_contrato' => $horasContrato['semana']]),
            'mes_actual'    => array_merge($mes,    [
                'horas_contrato'        => $horasContrato['mes'],
                'jornadas_incompletas'  => $incompletas,
            ]),
            'anio_actual'   => array_merge($anio,   ['horas_contrato' => $horasContrato['anio']]),
        ]);
    }

    public function show(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $uuid   = (string) $ctx['params']['uuid'];

        $stmt = Db::pdo()->prepare(
            'SELECT * FROM ' . Db::table('jornadas') . '
             WHERE uuid = ? AND user_id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$uuid, $userId]);
        $row = $stmt->fetch();
        if (!$row) {
            Response::error('NOT_FOUND', 'Jornada no encontrada.', 404);
        }

        // Fichajes asociados
        $stmt = Db::pdo()->prepare(
            'SELECT uuid, tipo, timestamp_evento, latitud, longitud, metodo
             FROM ' . Db::table('fichajes') . '
             WHERE jornada_id = ? AND deleted_at IS NULL
             ORDER BY timestamp_evento ASC'
        );
        $stmt->execute([(int) $row['id']]);
        $fichajes = $stmt->fetchAll();

        Response::ok(array_merge(
            $this->mapJornada($row),
            [
                'estado_raw' => $row['estado'],   // sin mapear, por si la app necesita CORREGIDA vs VALIDADA
                'fichajes'   => $fichajes,
            ]
        ));
    }

    // ---------- helpers ----------

    /**
     * Mapea estado del schema al vocabulario de la API:
     *   - VALIDADA o CORREGIDA o validada_at NOT NULL → VALIDADA
     *   - CERRADA → CERRADA
     *   - ABIERTA + fecha < hoy → INCOMPLETA
     *   - ABIERTA + fecha >= hoy → ABIERTA
     */
    private function mapEstado(array $r): string
    {
        if ($r['validada_at'] !== null || in_array($r['estado'], ['VALIDADA', 'CORREGIDA'], true)) {
            return 'VALIDADA';
        }
        if ($r['estado'] === 'CERRADA') {
            return 'CERRADA';
        }
        // ABIERTA
        return $r['fecha'] < date('Y-m-d') ? 'INCOMPLETA' : 'ABIERTA';
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
            'estado'             => $this->mapEstado($r),
        ];
    }

    /**
     * @return array{minutos_trabajados:int, minutos_extra:int, dias_trabajados:int, jornadas_total:int}
     */
    private function aggregate(int $userId, string $desde, string $hasta): array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT
                COALESCE(SUM(minutos_trabajados), 0) AS mt,
                COALESCE(SUM(minutos_extra), 0)      AS me,
                COUNT(DISTINCT CASE WHEN minutos_trabajados > 0 THEN fecha END) AS dt,
                COUNT(*) AS jt
             FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha BETWEEN ? AND ?'
        );
        $stmt->execute([$userId, $desde, $hasta]);
        $r = $stmt->fetch() ?: ['mt' => 0, 'me' => 0, 'dt' => 0, 'jt' => 0];
        return [
            'minutos_trabajados' => (int) $r['mt'],
            'minutos_extra'      => (int) $r['me'],
            'dias_trabajados'    => (int) $r['dt'],
            'jornadas_total'     => (int) $r['jt'],
        ];
    }

    private function loadContratoVigente(int $userId): ?array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT horas_semanales FROM ' . Db::table('contratos') . '
             WHERE user_id = ? AND vigente = 1
               AND fecha_inicio <= CURDATE()
               AND (fecha_fin IS NULL OR fecha_fin >= CURDATE())
             ORDER BY fecha_inicio DESC LIMIT 1'
        );
        $stmt->execute([$userId]);
        return $stmt->fetch() ?: null;
    }

    /**
     * Aproximación lineal sin descontar vacaciones ni festivos.
     * @return array{semana: ?float, mes: ?float, anio: ?float}
     */
    private function calcHorasContrato(?array $contrato, \DateTimeImmutable $hoy): array
    {
        if ($contrato === null) {
            return ['semana' => null, 'mes' => null, 'anio' => null];
        }
        $hSem = (float) $contrato['horas_semanales'];
        $diasMes = (int) $hoy->format('t');
        return [
            'semana' => round($hSem, 2),
            'mes'    => round($hSem * ($diasMes / 7), 2),
            'anio'   => round($hSem * 52, 2),
        ];
    }
}
