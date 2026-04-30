<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;

final class DerechoController
{
    private const SEARCH_MAX_RESULTS = 50;
    private const CODIGO_REGEX = '/^[A-Za-z0-9_\-]{1,60}$/';

    public function categorias(array $ctx): void
    {
        $stmt = Db::pdo()->query(
            'SELECT codigo, nombre, descripcion, icono, orden
             FROM ' . Db::table('derechos_categorias') . '
             WHERE activa = 1 ORDER BY orden ASC, nombre ASC'
        );
        Response::ok(['items' => $stmt ? $stmt->fetchAll() : []]);
    }

    public function contenidos(array $ctx): void
    {
        $codigo = (string) ($ctx['params']['codigo'] ?? '');
        if (!preg_match(self::CODIGO_REGEX, $codigo)) {
            Response::error('VALIDATION_ERROR', 'codigo inválido.', 422);
        }

        $stmt = Db::pdo()->prepare(
            'SELECT d.codigo, d.titulo, d.referencia_legal AS articulo_referencia, d.resumen,
                    d.url_boe, d.vigente_desde, d.orden
             FROM ' . Db::table('derechos') . ' d
             JOIN ' . Db::table('derechos_categorias') . ' c ON c.id = d.categoria_id
             WHERE c.codigo = ? AND c.activa = 1
               AND d.publicado = 1 AND d.idioma = ?
               AND d.vigente_desde <= CURDATE()
               AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= CURDATE())
             ORDER BY d.orden ASC, d.titulo ASC'
        );
        $stmt->execute([$codigo, 'es-ES']);
        $rows = $stmt->fetchAll();
        if (!$rows) {
            // ¿La categoría existe?
            $check = Db::pdo()->prepare('SELECT id FROM ' . Db::table('derechos_categorias') . ' WHERE codigo = ?');
            $check->execute([$codigo]);
            if ($check->fetchColumn() === false) {
                Response::error('NOT_FOUND', 'Categoría no encontrada.', 404);
            }
        }
        Response::ok(['items' => $rows]);
    }

    public function buscar(array $ctx): void
    {
        $q = trim((string) ($ctx['query']['q'] ?? ''));
        if (mb_strlen($q) < 3) {
            Response::error('VALIDATION_ERROR', 'La búsqueda requiere al menos 3 caracteres.', 422);
        }

        // Escape de wildcards LIKE: % _ \
        $like = '%' . addcslashes($q, '%_\\') . '%';

        $stmt = Db::pdo()->prepare(
            'SELECT d.codigo, d.titulo, d.referencia_legal AS articulo_referencia, d.resumen, c.codigo AS categoria
             FROM ' . Db::table('derechos') . ' d
             JOIN ' . Db::table('derechos_categorias') . ' c ON c.id = d.categoria_id
             WHERE d.publicado = 1 AND d.idioma = ? AND c.activa = 1
               AND d.vigente_desde <= CURDATE()
               AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= CURDATE())
               AND (d.titulo LIKE ? OR d.resumen LIKE ? OR d.contenido_md LIKE ? OR d.palabras_clave LIKE ?)
             ORDER BY d.orden ASC, d.titulo ASC
             LIMIT ' . self::SEARCH_MAX_RESULTS
        );
        $stmt->execute(['es-ES', $like, $like, $like, $like]);

        Response::ok([
            'query' => $q,
            'items' => $stmt->fetchAll(),
        ]);
    }

    public function show(array $ctx): void
    {
        $codigo = (string) ($ctx['params']['codigo'] ?? '');
        if (!preg_match(self::CODIGO_REGEX, $codigo)) {
            Response::error('VALIDATION_ERROR', 'codigo inválido.', 422);
        }

        $stmt = Db::pdo()->prepare(
            'SELECT d.id, d.codigo, d.titulo,
                    d.referencia_legal AS articulo_referencia,
                    d.resumen,
                    d.contenido_md AS contenido,
                    d.palabras_clave, d.url_boe, d.idioma, d.version,
                    d.vigente_desde, d.vigente_hasta, d.consultas_count,
                    c.codigo AS categoria_codigo, c.nombre AS categoria_nombre
             FROM ' . Db::table('derechos') . ' d
             JOIN ' . Db::table('derechos_categorias') . ' c ON c.id = d.categoria_id
             WHERE d.codigo = ? AND d.idioma = ?
               AND d.publicado = 1 AND c.activa = 1
               AND d.vigente_desde <= CURDATE()
               AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= CURDATE())
             LIMIT 1'
        );
        $stmt->execute([$codigo, 'es-ES']);
        $row = $stmt->fetch();
        if (!$row) {
            Response::error('NOT_FOUND', 'Derecho no encontrado.', 404);
        }

        // Increment atómico de consultas_count (telemetría no-crítica).
        try {
            Db::pdo()
                ->prepare('UPDATE ' . Db::table('derechos') . ' SET consultas_count = consultas_count + 1 WHERE id = ?')
                ->execute([(int) $row['id']]);
            $row['consultas_count'] = (int) $row['consultas_count'] + 1;
        } catch (\Throwable) {
            // Despliegue desactualizado: la columna no existe. Silenciar.
        }

        unset($row['id']);
        Response::ok($row);
    }
}
