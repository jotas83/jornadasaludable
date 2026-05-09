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
}
