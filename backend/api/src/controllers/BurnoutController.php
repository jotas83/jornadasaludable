<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;

final class BurnoutController
{
    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 30)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $stmt = Db::pdo()->prepare(
            'SELECT fecha_evaluacion, horas_promedio_dia, dias_sin_descanso,
                    jornadas_excesivas, puntuacion, nivel
             FROM ' . Db::table('burnout_evaluaciones') . '
             WHERE user_id = ?
             ORDER BY fecha_evaluacion DESC
             LIMIT ' . $limit . ' OFFSET ' . $offset
        );
        $stmt->execute([$userId]);
        $rows = $stmt->fetchAll();

        $items = array_map(static fn ($r) => [
            'fecha_evaluacion'   => $r['fecha_evaluacion'],
            'horas_promedio_dia' => $r['horas_promedio_dia'] !== null ? (float) $r['horas_promedio_dia'] : null,
            'dias_sin_descanso'  => $r['dias_sin_descanso']  !== null ? (int) $r['dias_sin_descanso']  : null,
            'jornadas_excesivas' => $r['jornadas_excesivas'] !== null ? (int) $r['jornadas_excesivas'] : null,
            'puntuacion'         => $r['puntuacion']         !== null ? (float) $r['puntuacion']      : null,
            'nivel'              => $r['nivel'],
        ], $rows);

        Response::ok([
            'items'  => $items,
            'actual' => $items[0] ?? null,  // la más reciente
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }
}
