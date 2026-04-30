<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;

final class UsuarioController
{
    public function perfil(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $stmt = Db::pdo()->prepare(
            'SELECT id, uuid, nif, nombre, apellidos, email, telefono, idioma,
                    fecha_nacimiento, nacionalidad, last_login_at, last_sync_at, created_at
             FROM ' . Db::table('users') . '
             WHERE id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$userId]);
        $row = $stmt->fetch();
        if (!$row) {
            Response::error('NOT_FOUND', 'Usuario no encontrado.', 404);
        }
        Response::ok($row);
    }

    public function updatePerfil(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $body   = $ctx['body'];

        // Solo permitimos modificar campos seguros del perfil. NIF/email no se
        // tocan aquí (son identidad legal/login — requieren flujo dedicado).
        $allowed = ['nombre', 'apellidos', 'telefono', 'idioma', 'fecha_nacimiento', 'nacionalidad'];
        $sets = [];
        $vals = [];
        foreach ($allowed as $field) {
            if (array_key_exists($field, $body)) {
                $sets[] = "`$field` = ?";
                $vals[] = $body[$field] === '' ? null : $body[$field];
            }
        }
        if (!$sets) {
            Response::error('VALIDATION_ERROR', 'No se ha enviado ningún campo modificable.', 422);
        }
        $vals[] = $userId;
        Db::pdo()
            ->prepare('UPDATE ' . Db::table('users') . ' SET ' . implode(', ', $sets) . ' WHERE id = ?')
            ->execute($vals);

        $this->perfil($ctx); // termina con Response::ok
    }

    public function empresa(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $stmt = Db::pdo()->prepare(
            'SELECT e.id, e.cif, e.razon_social, e.nombre_comercial, e.direccion,
                    e.cp, e.municipio, e.provincia, e.email, e.telefono,
                    s.codigo AS sector_codigo, s.nombre AS sector_nombre,
                    c.tipo AS contrato_tipo, c.jornada_tipo, c.horas_semanales,
                    c.fecha_inicio AS contrato_desde, c.fecha_fin AS contrato_hasta
             FROM ' . Db::table('contratos') . ' c
             JOIN ' . Db::table('empresas') . ' e  ON e.id = c.empresa_id
             JOIN ' . Db::table('sectores') . ' s  ON s.id = e.sector_id
             WHERE c.user_id = ? AND c.vigente = 1
               AND c.fecha_inicio <= CURDATE()
               AND (c.fecha_fin IS NULL OR c.fecha_fin >= CURDATE())
             ORDER BY c.fecha_inicio DESC LIMIT 1'
        );
        $stmt->execute([$userId]);
        $row = $stmt->fetch();
        if (!$row) {
            Response::error('NO_VIGENTE_CONTRATO', 'No hay contrato vigente que asocie usuario y empresa.', 404);
        }
        Response::ok($row);
    }
}
