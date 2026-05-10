<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\Application\AdministratorApplication;
use Joomla\CMS\Component\ComponentHelper;
use Joomla\CMS\Factory;
use Joomla\CMS\Filesystem\Path;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\Controller\BaseController;
use Joomla\CMS\Router\Route;
use Joomla\CMS\Session\Session;

final class DocumentosController extends BaseController
{
    public function descargar(): void
    {
        $id = (int) $this->input->getInt('id', 0);
        if ($id <= 0) {
            $this->setRedirect(Route::_('index.php?option=com_jornadasaludable&view=documentos', false));
            return;
        }

        $db = Factory::getContainer()->get('DatabaseDriver');
        $q = $db->getQuery(true)
            ->select(['ruta_storage', 'nombre_fichero'])
            ->from($db->quoteName('#__js_documentos'))
            ->where($db->quoteName('id') . ' = ' . $id)
            ->where($db->quoteName('deleted_at') . ' IS NULL');
        $db->setQuery($q);
        $row = $db->loadObject();

        if (!$row || empty($row->ruta_storage)) {
            $this->setRedirect(
                Route::_('index.php?option=com_jornadasaludable&view=documentos', false),
                Text::_('COM_JORNADASALUDABLE_ERROR_DOCUMENTO_NOT_FOUND'),
                'error'
            );
            return;
        }

        $params = ComponentHelper::getParams('com_jornadasaludable');
        $base   = rtrim(
            (string) $params->get('storage_path', 'C:/proyectos/jornadasaludable/backend/storage/documentos'),
            "/\\"
        );
        $base   = Path::clean($base);
        $rel    = ltrim((string) $row->ruta_storage, '/\\');

        $relStripped = $rel;
        $baseLeaf    = basename($base);
        if ($baseLeaf !== '' && str_starts_with($relStripped, $baseLeaf . '/')) {
            $relStripped = substr($relStripped, strlen($baseLeaf) + 1);
        } elseif (str_starts_with($relStripped, 'storage/documentos/')) {
            $relStripped = substr($relStripped, strlen('storage/documentos/'));
        } elseif (str_starts_with($relStripped, 'documentos/')) {
            $relStripped = substr($relStripped, strlen('documentos/'));
        }

        $abs = Path::clean($base . DIRECTORY_SEPARATOR . $relStripped);

        if (!is_file($abs) || !is_readable($abs) || strpos($abs, $base) !== 0) {
            $this->setRedirect(
                Route::_('index.php?option=com_jornadasaludable&view=documentos', false),
                Text::_('COM_JORNADASALUDABLE_ERROR_DOCUMENTO_NOT_FOUND'),
                'error'
            );
            return;
        }

        $upd = $db->getQuery(true)
            ->update($db->quoteName('#__js_documentos'))
            ->set($db->quoteName('descargado') . ' = 1')
            ->set($db->quoteName('descargado_at') . ' = ' . $db->quote(gmdate('Y-m-d H:i:s')))
            ->where($db->quoteName('id') . ' = ' . $id);
        $db->setQuery($upd)->execute();

        /** @var AdministratorApplication $app */
        $app = $this->app;
        $app->setHeader('Content-Type', 'application/pdf', true);
        $app->setHeader('Content-Disposition', 'attachment; filename="' . basename((string) $row->nombre_fichero) . '"', true);
        $app->setHeader('Content-Length', (string) filesize($abs), true);
        $app->setHeader('X-Content-Type-Options', 'nosniff', true);
        $app->sendHeaders();

        readfile($abs);
        $app->close();
    }

    public function exportarCsv(): void
    {
        Session::checkToken() or $this->app->enqueueMessage(Text::_('JINVALID_TOKEN'), 'error');

        $userId = (int) $this->input->getInt('user_id', 0);
        $desde  = trim((string) $this->input->getString('fecha_inicio', ''));
        $hasta  = trim((string) $this->input->getString('fecha_fin', ''));

        $reDate = '/^\d{4}-\d{2}-\d{2}$/';
        if ($userId <= 0 || !preg_match($reDate, $desde) || !preg_match($reDate, $hasta) || $desde > $hasta) {
            $this->setRedirect(
                Route::_('index.php?option=com_jornadasaludable&view=documentos', false),
                Text::_('COM_JORNADASALUDABLE_DOCUMENTO_EXPORT_ERROR_PARAMS'),
                'error'
            );
            return;
        }

        $db = Factory::getContainer()->get('DatabaseDriver');

        $userQ = $db->getQuery(true)
            ->select($db->quoteName('nif'))
            ->from($db->quoteName('#__js_users'))
            ->where($db->quoteName('id') . ' = ' . $userId)
            ->where($db->quoteName('deleted_at') . ' IS NULL');
        $db->setQuery($userQ);
        $nif = (string) ($db->loadResult() ?? 'usuario');

        $subEntrada = '(SELECT MIN(f.timestamp_evento) FROM ' . $db->quoteName('#__js_fichajes', 'f')
            . ' WHERE f.jornada_id = j.id AND f.tipo = ' . $db->quote('ENTRADA')
            . ' AND f.deleted_at IS NULL)';
        $subSalida = '(SELECT MAX(f.timestamp_evento) FROM ' . $db->quoteName('#__js_fichajes', 'f')
            . ' WHERE f.jornada_id = j.id AND f.tipo = ' . $db->quote('SALIDA')
            . ' AND f.deleted_at IS NULL)';

        $q = $db->getQuery(true)
            ->select([
                'j.fecha',
                'j.minutos_trabajados',
                'j.minutos_pausa',
                'j.minutos_extra',
                'j.estado',
                $subEntrada . ' AS primera_entrada',
                $subSalida . ' AS ultima_salida',
            ])
            ->from($db->quoteName('#__js_jornadas', 'j'))
            ->where($db->quoteName('j.user_id') . ' = ' . $userId)
            ->where($db->quoteName('j.fecha') . ' >= ' . $db->quote($desde))
            ->where($db->quoteName('j.fecha') . ' <= ' . $db->quote($hasta))
            ->where($db->quoteName('j.deleted_at') . ' IS NULL')
            ->order($db->quoteName('j.fecha') . ' ASC');
        $db->setQuery($q);
        $rows = $db->loadObjectList() ?: [];

        $nifSafe  = preg_replace('/[^A-Za-z0-9]/', '', $nif) ?: 'usuario';
        $filename = sprintf('jornada_%s_%s_%s.csv', $nifSafe, $desde, $hasta);

        /** @var AdministratorApplication $app */
        $app = $this->app;
        $app->setHeader('Content-Type', 'text/csv; charset=UTF-8', true);
        $app->setHeader('Content-Disposition', 'attachment; filename="' . $filename . '"', true);
        $app->setHeader('Cache-Control', 'no-store', true);
        $app->setHeader('X-Content-Type-Options', 'nosniff', true);
        $app->sendHeaders();

        $out = fopen('php://output', 'w');
        fwrite($out, "\xEF\xBB\xBF");
        fputcsv($out, [
            'Fecha', 'Entrada', 'Salida',
            'Horas trabajadas', 'Pausas (min)', 'Horas extra', 'Estado',
        ], ';');

        foreach ($rows as $row) {
            $entrada = !empty($row->primera_entrada) ? substr((string) $row->primera_entrada, 11, 8) : '';
            $salida  = !empty($row->ultima_salida)   ? substr((string) $row->ultima_salida, 11, 8)   : '';
            $minTrab = (int) ($row->minutos_trabajados ?? 0);
            $minExtra = (int) ($row->minutos_extra ?? 0);
            $hTrab   = sprintf('%d:%02d', intdiv($minTrab, 60), $minTrab % 60);
            $hExtra  = sprintf('%d:%02d', intdiv($minExtra, 60), $minExtra % 60);

            fputcsv($out, [
                (string) $row->fecha,
                $entrada,
                $salida,
                $hTrab,
                (string) ($row->minutos_pausa ?? 0),
                $hExtra,
                (string) $row->estado,
            ], ';');
        }
        fclose($out);

        $app->close();
    }
}
