<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;
use Ramsey\Uuid\Uuid;

final class AlertaController
{
    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 100)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $where = ['a.user_id = ?'];
        $vals  = [$userId];
        if (isset($q['leida']) && $q['leida'] !== '') {
            $where[] = 'a.leida = ?';
            $vals[]  = (int) (bool) $q['leida'];
        }
        if (!empty($q['nivel'])) {
            $where[] = 't.severidad = ?';
            $vals[]  = strtoupper((string) $q['nivel']);
        }

        $sql = 'SELECT a.*, t.codigo AS tipo_codigo, t.nombre AS tipo_nombre,
                       t.severidad AS tipo_severidad, t.referencia_legal
                FROM ' . Db::table('alertas') . ' a
                JOIN ' . Db::table('alertas_tipos') . ' t ON t.id = a.tipo_id
                WHERE ' . implode(' AND ', $where) . '
                ORDER BY a.fecha_evento DESC
                LIMIT ' . $limit . ' OFFSET ' . $offset;
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);
        $rows = $stmt->fetchAll();

        Response::ok([
            'items'  => array_map(fn ($r) => $this->mapAlerta($r), $rows),
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }

    public function tipos(array $ctx): void
    {
        $stmt = Db::pdo()->query(
            'SELECT codigo, nombre, descripcion, severidad, referencia_legal, umbral_valor, umbral_unidad
             FROM ' . Db::table('alertas_tipos') . '
             WHERE activa = 1 ORDER BY id ASC'
        );
        $rows = $stmt ? $stmt->fetchAll() : [];
        Response::ok(['items' => array_map(static fn ($r) => [
            'tipo'             => $r['codigo'],
            'nombre'           => $r['nombre'],
            'descripcion'      => $r['descripcion'],
            'nivel'            => $r['severidad'],
            'base_legal'       => $r['referencia_legal'],
            'umbral'           => $r['umbral_valor'] !== null ? [
                'valor'  => (int) $r['umbral_valor'],
                'unidad' => $r['umbral_unidad'],
            ] : null,
        ], $rows)]);
    }

    public function generar(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];

        $tipos = $this->loadTiposByCode();
        $generadas = [];

        // 1) JORNADA_EXCEDIDA — minutos_trabajados > 540 en últimos 7 días
        $stmt = Db::pdo()->prepare(
            'SELECT id, uuid, fecha, minutos_trabajados FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
               AND minutos_trabajados > 540'
        );
        $stmt->execute([$userId]);
        foreach ($stmt->fetchAll() as $j) {
            $a = $this->createIfNew($userId, $tipos['JORNADA_EXCEDIDA'], (int) $j['id'],
                $j['fecha'] . ' 23:59:59',
                sprintf('Jornada del %s superó las 9 horas (%d min).', $j['fecha'], $j['minutos_trabajados']),
                (string) $j['minutos_trabajados']);
            if ($a) $generadas[] = $a;
        }

        // 2) DESCANSO_INSUFICIENTE — gap entre fin(N) y inicio(N+1) < 720 min
        $stmt = Db::pdo()->prepare(
            'SELECT id, uuid, fecha, hora_inicio, hora_fin FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha >= DATE_SUB(CURDATE(), INTERVAL 14 DAY)
               AND hora_inicio IS NOT NULL AND hora_fin IS NOT NULL
             ORDER BY fecha ASC'
        );
        $stmt->execute([$userId]);
        $jornadas = $stmt->fetchAll();
        for ($i = 1; $i < count($jornadas); $i++) {
            $prev = $jornadas[$i - 1];
            $curr = $jornadas[$i];
            $gap  = (int) round((strtotime($curr['hora_inicio']) - strtotime($prev['hora_fin'])) / 60);
            if ($gap > 0 && $gap < 720) {
                $a = $this->createIfNew($userId, $tipos['DESCANSO_INSUFICIENTE'], (int) $curr['id'],
                    $curr['hora_inicio'],
                    sprintf('Descanso entre jornadas (%d min) inferior a 12 h.', $gap),
                    (string) $gap);
                if ($a) $generadas[] = $a;
            }
        }

        // 3) HORAS_EXTRA_LIMITE — SUM(minutos) anual > 4800
        $stmt = Db::pdo()->prepare(
            'SELECT COALESCE(SUM(minutos), 0) FROM ' . Db::table('horas_extra') . '
             WHERE user_id = ? AND deleted_at IS NULL AND created_at >= DATE_FORMAT(CURDATE(), "%Y-01-01")'
        );
        $stmt->execute([$userId]);
        $totalAnual = (int) $stmt->fetchColumn();
        if ($totalAnual > 4800) {
            $a = $this->createIfNew($userId, $tipos['HORAS_EXTRA_LIMITE'], null,
                gmdate('Y-m-d H:i:s'),
                sprintf('Total anual de horas extra (%d min) supera el tope legal de 4800 min (80 h).', $totalAnual),
                (string) $totalAnual);
            if ($a) $generadas[] = $a;
        }

        // 4) SIN_DESCANSO_SEMANAL — 7 días distintos trabajados en últimos 7 días
        $stmt = Db::pdo()->prepare(
            'SELECT COUNT(DISTINCT fecha) FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
               AND minutos_trabajados > 0'
        );
        $stmt->execute([$userId]);
        if ((int) $stmt->fetchColumn() >= 7) {
            $a = $this->createIfNew($userId, $tipos['SIN_DESCANSO_SEMANAL'], null,
                gmdate('Y-m-d H:i:s'),
                'Has trabajado los últimos 7 días sin descanso semanal.',
                '7');
            if ($a) $generadas[] = $a;
        }

        // 5) FICHAJE_INCOMPLETO — jornada ABIERTA con fecha < hoy
        $stmt = Db::pdo()->prepare(
            'SELECT id, fecha FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND estado = "ABIERTA" AND fecha < CURDATE()
               AND fecha >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)'
        );
        $stmt->execute([$userId]);
        foreach ($stmt->fetchAll() as $j) {
            $tipo = $tipos['FICHAJE_INCOMPLETO'] ?? $tipos['PAUSA_OMITIDA'] ?? null;
            // Si no existe el tipo en el seed, saltamos (defensivo).
            if ($tipo === null) continue;
            $a = $this->createIfNew($userId, $tipos['FICHAJE_INCOMPLETO'] ?? $tipo, (int) $j['id'],
                $j['fecha'] . ' 23:59:59',
                sprintf('La jornada del %s quedó abierta sin SALIDA.', $j['fecha']),
                null);
            if ($a) $generadas[] = $a;
        }

        // 6) PAUSA_OMITIDA — jornadas con minutos_trabajados > 360 y SUM(pausas computa_jornada=0) < 15
        $stmt = Db::pdo()->prepare(
            'SELECT j.id, j.fecha, COALESCE(SUM(p.minutos), 0) AS pausa_total
             FROM ' . Db::table('jornadas') . ' j
             LEFT JOIN ' . Db::table('pausas') . ' p
               ON p.jornada_id = j.id AND p.computa_jornada = 0 AND p.fin IS NOT NULL
             WHERE j.user_id = ? AND j.deleted_at IS NULL
               AND j.fecha >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
               AND j.minutos_trabajados > 360
             GROUP BY j.id'
        );
        $stmt->execute([$userId]);
        foreach ($stmt->fetchAll() as $j) {
            if ((int) $j['pausa_total'] < 15) {
                $a = $this->createIfNew($userId, $tipos['PAUSA_OMITIDA'], (int) $j['id'],
                    $j['fecha'] . ' 23:59:59',
                    sprintf('Jornada del %s sin pausa mínima (15 min).', $j['fecha']),
                    (string) $j['pausa_total']);
                if ($a) $generadas[] = $a;
            }
        }

        // 7) RIESGO_BURNOUT — calcula score y genera + registra eval si nivel cambia
        $burnout = $this->evaluateBurnout($userId);
        if ($burnout['nivel'] === 'CRITICO' && isset($tipos['RIESGO_BURNOUT'])) {
            $a = $this->createIfNew($userId, $tipos['RIESGO_BURNOUT'], null,
                gmdate('Y-m-d H:i:s'),
                sprintf('Riesgo de burnout %s (puntuación %.1f/100).', $burnout['nivel'], $burnout['puntuacion']),
                (string) $burnout['puntuacion']);
            if ($a) $generadas[] = $a;
        }
        $this->persistBurnoutIfChanged($userId, $burnout);

        Response::ok([
            'generadas' => count($generadas),
            'alertas'   => $generadas,
            'burnout'   => $burnout,
        ]);
    }

    public function marcarLeida(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $uuid   = (string) $ctx['params']['uuid'];

        $stmt = Db::pdo()->prepare(
            'UPDATE ' . Db::table('alertas') . '
             SET leida = 1, leida_at = NOW()
             WHERE uuid = ? AND user_id = ? AND leida = 0'
        );
        $stmt->execute([$uuid, $userId]);
        if ($stmt->rowCount() === 0) {
            // No existe o ya estaba leída
            $check = Db::pdo()->prepare('SELECT id FROM ' . Db::table('alertas') . ' WHERE uuid = ? AND user_id = ?');
            $check->execute([$uuid, $userId]);
            if ($check->fetchColumn() === false) {
                Response::error('NOT_FOUND', 'Alerta no encontrada.', 404);
            }
        }

        $stmt = Db::pdo()->prepare(
            'SELECT a.*, t.codigo AS tipo_codigo, t.nombre AS tipo_nombre,
                    t.severidad AS tipo_severidad, t.referencia_legal
             FROM ' . Db::table('alertas') . ' a
             JOIN ' . Db::table('alertas_tipos') . ' t ON t.id = a.tipo_id
             WHERE a.uuid = ? AND a.user_id = ? LIMIT 1'
        );
        $stmt->execute([$uuid, $userId]);
        $row = $stmt->fetch();
        Response::ok($this->mapAlerta($row));
    }

    // ---------- helpers ----------

    /** @return array<string,int> codigo → id */
    private function loadTiposByCode(): array
    {
        $stmt = Db::pdo()->query('SELECT id, codigo FROM ' . Db::table('alertas_tipos'));
        $out = [];
        foreach (($stmt ? $stmt->fetchAll() : []) as $r) {
            $out[$r['codigo']] = (int) $r['id'];
        }
        return $out;
    }

    /**
     * Inserta una alerta solo si no existe ya una equivalente sin leer
     * (mismo user/tipo/jornada o mismo user/tipo/DATE(fecha_evento)).
     */
    private function createIfNew(int $userId, int $tipoId, ?int $jornadaId, string $fechaEvento, string $mensaje, ?string $valor): ?array
    {
        $sql = 'SELECT id FROM ' . Db::table('alertas') . '
                WHERE user_id = ? AND tipo_id = ? AND leida = 0 AND ';
        $vals = [$userId, $tipoId];
        if ($jornadaId !== null) {
            $sql   .= 'jornada_id = ?';
            $vals[] = $jornadaId;
        } else {
            $sql   .= 'DATE(fecha_evento) = DATE(?)';
            $vals[] = $fechaEvento;
        }
        $sql .= ' LIMIT 1';
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);
        if ($stmt->fetchColumn() !== false) {
            return null; // dedup
        }

        $uuid = Uuid::uuid4()->toString();
        Db::pdo()->prepare(
            'INSERT INTO ' . Db::table('alertas') . '
             (uuid, user_id, tipo_id, jornada_id, fecha_evento, mensaje, valor_detectado)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        )->execute([$uuid, $userId, $tipoId, $jornadaId, $fechaEvento, $mensaje, $valor]);

        $stmt = Db::pdo()->prepare(
            'SELECT a.*, t.codigo AS tipo_codigo, t.nombre AS tipo_nombre,
                    t.severidad AS tipo_severidad, t.referencia_legal
             FROM ' . Db::table('alertas') . ' a
             JOIN ' . Db::table('alertas_tipos') . ' t ON t.id = a.tipo_id
             WHERE a.uuid = ? LIMIT 1'
        );
        $stmt->execute([$uuid]);
        return $this->mapAlerta($stmt->fetch());
    }

    /**
     * @return array{horas_promedio_dia: float, dias_sin_descanso: int, jornadas_excesivas: int, puntuacion: float, nivel: string}
     */
    private function evaluateBurnout(int $userId): array
    {
        // Ventana 30 días
        $stmt = Db::pdo()->prepare(
            'SELECT fecha, minutos_trabajados FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
             ORDER BY fecha ASC'
        );
        $stmt->execute([$userId]);
        $jornadas = $stmt->fetchAll();

        $totalMin    = 0;
        $diasTrab    = 0;
        $excesivas   = 0;
        $rachaMax    = 0;
        $rachaActual = 0;
        $prevDate    = null;

        foreach ($jornadas as $j) {
            $m = (int) $j['minutos_trabajados'];
            if ($m > 0) {
                $totalMin += $m;
                $diasTrab++;
                if ($m > 540) $excesivas++;

                if ($prevDate !== null && (strtotime($j['fecha']) - strtotime($prevDate)) === 86400) {
                    $rachaActual++;
                } else {
                    $rachaActual = 1;
                }
                $rachaMax = max($rachaMax, $rachaActual);
                $prevDate = $j['fecha'];
            }
        }

        $horasProm = $diasTrab > 0 ? ($totalMin / 60.0) / $diasTrab : 0.0;
        $score = ($horasProm / 9 * 40) + ($rachaMax / 30 * 35) + ($excesivas / 20 * 25);
        $score = max(0.0, min(100.0, $score));

        return [
            'horas_promedio_dia' => round($horasProm, 2),
            'dias_sin_descanso'  => $rachaMax,
            'jornadas_excesivas' => $excesivas,
            'puntuacion'         => round($score, 2),
            'nivel'              => match (true) {
                $score >= 75 => 'CRITICO',
                $score >= 50 => 'ALTO',
                $score >= 25 => 'MODERADO',
                default      => 'BAJO',
            },
        ];
    }

    private function persistBurnoutIfChanged(int $userId, array $eval): void
    {
        $stmt = Db::pdo()->prepare(
            'SELECT nivel FROM ' . Db::table('burnout_evaluaciones') . '
             WHERE user_id = ? ORDER BY fecha_evaluacion DESC LIMIT 1'
        );
        $stmt->execute([$userId]);
        $prev = $stmt->fetchColumn();
        if ($prev !== false && $prev === $eval['nivel']) {
            return; // no cambió, no persiste
        }

        Db::pdo()->prepare(
            'INSERT INTO ' . Db::table('burnout_evaluaciones') . '
             (user_id, fecha_evaluacion, horas_promedio_dia, dias_sin_descanso, jornadas_excesivas, puntuacion, nivel)
             VALUES (?, NOW(), ?, ?, ?, ?, ?)'
        )->execute([
            $userId, $eval['horas_promedio_dia'], $eval['dias_sin_descanso'],
            $eval['jornadas_excesivas'], $eval['puntuacion'], $eval['nivel'],
        ]);
    }

    private function mapAlerta(array $r): array
    {
        return [
            'uuid'              => $r['uuid'],
            'tipo'              => $r['tipo_codigo'],
            'tipo_nombre'       => $r['tipo_nombre'],
            'nivel'             => $r['tipo_severidad'],
            'base_legal'        => $r['referencia_legal'],
            'mensaje'           => $r['mensaje'],
            'valor_detectado'   => $r['valor_detectado'],
            'fecha_generacion'  => $r['fecha_evento'],
            'leida'             => (bool) $r['leida'],
            'leida_at'          => $r['leida_at'],
            'jornada_id'        => $r['jornada_id'] !== null ? (int) $r['jornada_id'] : null,
        ];
    }
}
