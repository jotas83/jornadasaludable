<?php
declare(strict_types=1);

namespace JornadaSaludable\Api\Controllers;

use JornadaSaludable\Api\Db;
use JornadaSaludable\Api\Response;
use Ramsey\Uuid\Uuid;
use TCPDF;

final class DocumentoController
{
    private const TIPOS_VALIDOS = [
        'REGISTRO_JORNADA_MENSUAL', 'RESUMEN_HORAS_EXTRA',
        'SOLICITUD_VACACIONES', 'CERTIFICADO_DERECHOS', 'OTROS',
    ];

    public function index(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $q      = $ctx['query'];
        $limit  = max(1, min(500, (int) ($q['limit']  ?? 50)));
        $offset = max(0, (int) ($q['offset'] ?? 0));

        $where = ['user_id = ?', 'deleted_at IS NULL'];
        $vals  = [$userId];
        if (!empty($q['tipo'])) {
            $where[] = 'tipo = ?';
            $vals[]  = strtoupper((string) $q['tipo']);
        }

        $sql = 'SELECT uuid, tipo, periodo_desde, periodo_hasta, nombre_fichero,
                       tamano_bytes, hash_sha256, firmado, descargado, descargado_at, created_at
                FROM ' . Db::table('documentos') . '
                WHERE ' . implode(' AND ', $where) . '
                ORDER BY created_at DESC LIMIT ' . $limit . ' OFFSET ' . $offset;
        $stmt = Db::pdo()->prepare($sql);
        $stmt->execute($vals);

        Response::ok([
            'items'  => $stmt->fetchAll(),
            'limit'  => $limit,
            'offset' => $offset,
        ]);
    }

    public function generar(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $body   = $ctx['body'];

        $tipo = strtoupper((string) ($body['tipo'] ?? 'REGISTRO_JORNADA_MENSUAL'));
        if (!in_array($tipo, self::TIPOS_VALIDOS, true)) {
            Response::error('VALIDATION_ERROR', 'tipo de documento inválido.', 422);
        }
        $periodoDesde = (string) ($body['periodo_desde'] ?? '');
        $periodoHasta = (string) ($body['periodo_hasta'] ?? '');
        if ($tipo === 'REGISTRO_JORNADA_MENSUAL') {
            if (!preg_match('/^\d{4}-\d{2}$/', (string) ($body['mes'] ?? ''))) {
                Response::error('VALIDATION_ERROR', 'mes (YYYY-MM) es obligatorio para REGISTRO_JORNADA_MENSUAL.', 422);
            }
            $mes = (string) $body['mes'];
            $periodoDesde = $mes . '-01';
            $periodoHasta = date('Y-m-t', strtotime($periodoDesde));
        }

        // Carga datos del usuario y empresa actual
        $user    = $this->loadUser($userId);
        $empresa = $this->loadEmpresaActual($userId);
        $jornadas = ($tipo === 'REGISTRO_JORNADA_MENSUAL')
            ? $this->loadJornadasRange($userId, $periodoDesde, $periodoHasta)
            : [];

        // Render PDF
        $pdfBytes = $this->renderPdf($tipo, $user, $empresa, $jornadas, $periodoDesde, $periodoHasta);

        // Persistencia en disco
        $uuid = Uuid::uuid4()->toString();
        $year = (int) date('Y');
        $base = (string) ($GLOBALS['JS_CONFIG']['documentos']['storage_path'] ?? sys_get_temp_dir());
        $rel  = $userId . '/' . $year . '/' . $uuid . '.pdf';
        $dir  = $base . '/' . $userId . '/' . $year;
        if (!is_dir($dir) && !@mkdir($dir, 0755, true) && !is_dir($dir)) {
            Response::error('STORAGE_ERROR', 'No se pudo crear el directorio de almacenamiento.', 500);
        }
        $abs = $base . '/' . $rel;
        if (file_put_contents($abs, $pdfBytes) === false) {
            Response::error('STORAGE_ERROR', 'No se pudo escribir el PDF.', 500);
        }

        $hash = hash('sha256', $pdfBytes);
        $name = sprintf('%s-%s-%s.pdf',
            strtolower(str_replace('_', '-', $tipo)),
            $user['nif'] ?? 'sin-nif',
            $periodoDesde !== '' ? substr($periodoDesde, 0, 7) : date('Y-m')
        );

        Db::pdo()->prepare(
            'INSERT INTO ' . Db::table('documentos') . '
             (uuid, user_id, tipo, periodo_desde, periodo_hasta, nombre_fichero, ruta_storage, tamano_bytes, hash_sha256)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        )->execute([
            $uuid, $userId, $tipo,
            $periodoDesde !== '' ? $periodoDesde : null,
            $periodoHasta !== '' ? $periodoHasta : null,
            $name, $rel, strlen($pdfBytes), $hash,
        ]);

        Response::created([
            'uuid'           => $uuid,
            'tipo'           => $tipo,
            'periodo_desde'  => $periodoDesde !== '' ? $periodoDesde : null,
            'periodo_hasta'  => $periodoHasta !== '' ? $periodoHasta : null,
            'nombre_fichero' => $name,
            'tamano_bytes'   => strlen($pdfBytes),
            'hash_sha256'    => $hash,
        ]);
    }

    public function descargar(array $ctx): void
    {
        $userId = (int) $ctx['auth']['sub'];
        $uuid   = (string) $ctx['params']['uuid'];

        $stmt = Db::pdo()->prepare(
            'SELECT * FROM ' . Db::table('documentos') . '
             WHERE uuid = ? AND user_id = ? AND deleted_at IS NULL LIMIT 1'
        );
        $stmt->execute([$uuid, $userId]);
        $doc = $stmt->fetch();
        if (!$doc) {
            Response::error('NOT_FOUND', 'Documento no encontrado.', 404);
        }

        $base = (string) ($GLOBALS['JS_CONFIG']['documentos']['storage_path'] ?? '');
        $abs  = $base . '/' . $doc['ruta_storage'];
        if (!is_file($abs) || !is_readable($abs)) {
            Response::error('FILE_NOT_FOUND', 'El fichero ya no existe en el almacenamiento.', 410);
        }

        // Marcar como descargado la primera vez
        if ((int) $doc['descargado'] === 0) {
            Db::pdo()
                ->prepare('UPDATE ' . Db::table('documentos') . ' SET descargado = 1, descargado_at = NOW() WHERE id = ?')
                ->execute([(int) $doc['id']]);
        }

        // Stream binario y exit
        $name = preg_replace('/[^A-Za-z0-9._\- ]+/', '_', (string) $doc['nombre_fichero']) ?: 'documento.pdf';
        if (!headers_sent()) {
            http_response_code(200);
            header('Content-Type: application/pdf');
            header('Content-Disposition: attachment; filename="' . mb_substr($name, 0, 200) . '"');
            header('Content-Length: ' . filesize($abs));
            header('Cache-Control: private, no-cache, no-store, must-revalidate');
        }
        readfile($abs);
        exit;
    }

    // ---------- helpers ----------

    private function loadUser(int $userId): array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT id, uuid, nif, nombre, apellidos, email FROM ' . Db::table('users') . ' WHERE id = ?'
        );
        $stmt->execute([$userId]);
        return $stmt->fetch() ?: [];
    }

    private function loadEmpresaActual(int $userId): ?array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT e.razon_social, e.cif FROM ' . Db::table('contratos') . ' c
             JOIN ' . Db::table('empresas') . ' e ON e.id = c.empresa_id
             WHERE c.user_id = ? AND c.vigente = 1
               AND c.fecha_inicio <= CURDATE()
               AND (c.fecha_fin IS NULL OR c.fecha_fin >= CURDATE())
             ORDER BY c.fecha_inicio DESC LIMIT 1'
        );
        $stmt->execute([$userId]);
        return $stmt->fetch() ?: null;
    }

    /** @return list<array> */
    private function loadJornadasRange(int $userId, string $desde, string $hasta): array
    {
        $stmt = Db::pdo()->prepare(
            'SELECT fecha, hora_inicio, hora_fin, minutos_trabajados,
                    minutos_pausa, minutos_extra, estado
             FROM ' . Db::table('jornadas') . '
             WHERE user_id = ? AND deleted_at IS NULL
               AND fecha BETWEEN ? AND ?
             ORDER BY fecha ASC'
        );
        $stmt->execute([$userId, $desde, $hasta]);
        return $stmt->fetchAll();
    }

    private function renderPdf(string $tipo, array $user, ?array $empresa, array $jornadas, string $desde, string $hasta): string
    {
        $pdf = new TCPDF('P', 'mm', 'A4', true, 'UTF-8', false);
        $pdf->SetCreator('JornadaSaludable');
        $pdf->SetAuthor('JornadaSaludable');
        $pdf->SetTitle('Registro horario');
        $pdf->setPrintHeader(false);
        $pdf->setPrintFooter(false);
        $pdf->SetMargins(15, 15, 15);
        $pdf->SetAutoPageBreak(true, 15);
        $pdf->AddPage();

        $nombreCompleto = trim(($user['nombre'] ?? '') . ' ' . ($user['apellidos'] ?? ''));
        $razon = $empresa !== null
            ? htmlspecialchars($empresa['razon_social'] . ' (CIF ' . $empresa['cif'] . ')')
            : '— sin contrato vigente —';

        $totalMin = 0;
        foreach ($jornadas as $j) {
            $totalMin += (int) $j['minutos_trabajados'];
        }

        $rows = '';
        foreach ($jornadas as $j) {
            $rows .= sprintf(
                '<tr>
                    <td>%s</td><td>%s</td><td>%s</td>
                    <td align="right">%d</td>
                    <td align="right">%d</td>
                    <td align="right">%d</td>
                    <td>%s</td>
                </tr>',
                htmlspecialchars((string) $j['fecha']),
                htmlspecialchars((string) ($j['hora_inicio'] ?? '—')),
                htmlspecialchars((string) ($j['hora_fin']    ?? '—')),
                (int) $j['minutos_trabajados'],
                (int) $j['minutos_pausa'],
                (int) $j['minutos_extra'],
                htmlspecialchars((string) $j['estado'])
            );
        }

        $html = '<h1>Registro de jornada</h1>'
              . '<p><b>Trabajador:</b> ' . htmlspecialchars($nombreCompleto)
              . ' &mdash; <b>NIF:</b> ' . htmlspecialchars((string) ($user['nif'] ?? ''))
              . '<br><b>Empresa:</b> ' . $razon
              . '<br><b>Periodo:</b> ' . htmlspecialchars($desde) . ' a ' . htmlspecialchars($hasta)
              . '<br><b>Generado:</b> ' . gmdate('Y-m-d H:i:s') . ' UTC'
              . '<br><b>Tipo:</b> ' . htmlspecialchars($tipo) . '</p>'
              . '<p><i>Documento generado en cumplimiento del Art. 12 RD-Ley 8/2019. '
              . 'Es copia complementaria; la empresa conserva el oficial 4 años.</i></p>'
              . '<table border="1" cellpadding="4" cellspacing="0">'
              . '<tr><th>Fecha</th><th>Inicio</th><th>Fin</th><th>Min trab.</th><th>Min pausa</th><th>Min extra</th><th>Estado</th></tr>'
              . ($rows ?: '<tr><td colspan="7" align="center">Sin jornadas en el periodo.</td></tr>')
              . '<tr><td colspan="3" align="right"><b>Total minutos trabajados:</b></td>'
              . '<td align="right"><b>' . $totalMin . '</b></td><td colspan="3"></td></tr>'
              . '</table>';

        $pdf->writeHTML($html, true, false, true, false, '');

        return (string) $pdf->Output('doc.pdf', 'S');
    }
}
