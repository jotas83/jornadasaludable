<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\Controller\AdminController;
use Joomla\CMS\Router\Route;

final class TrabajadoresController extends AdminController
{
    protected $text_prefix = 'COM_JORNADASALUDABLE_TRABAJADOR';

    public function getModel($name = 'Trabajador', $prefix = 'Administrator', $config = ['ignore_request' => true])
    {
        return parent::getModel($name, $prefix, $config);
    }

    public function activar(): void
    {
        $this->setActivo(1);
    }

    public function desactivar(): void
    {
        $this->setActivo(0);
    }

    private function setActivo(int $value): void
    {
        $this->checkToken();
        $ids = (array) $this->input->get('cid', [], 'array');
        $ids = array_values(array_unique(array_filter(array_map('intval', $ids))));
        $msg = '';
        if ($ids) {
            $db = Factory::getContainer()->get('DatabaseDriver');
            $q = $db->getQuery(true)
                ->update($db->quoteName('#__js_users'))
                ->set($db->quoteName('activo') . ' = ' . (int) $value)
                ->whereIn($db->quoteName('id'), $ids);
            $db->setQuery($q)->execute();
            $msg = sprintf(Text::_('COM_JORNADASALUDABLE_N_ITEMS_GUARDADOS'), count($ids));
        }
        $this->setRedirect(Route::_('index.php?option=com_jornadasaludable&view=trabajadores', false), $msg);
    }
}
