<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\View\Documentos;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\View\HtmlView as BaseHtmlView;
use Joomla\CMS\Toolbar\ToolbarHelper;

final class HtmlView extends BaseHtmlView
{
    public $items;
    public $pagination;
    public $state;
    public $filterForm;
    public $activeFilters;
    public array $trabajadores = [];

    public function display($tpl = null): void
    {
        $this->items         = $this->get('Items');
        $this->pagination    = $this->get('Pagination');
        $this->state         = $this->get('State');
        $this->filterForm    = $this->get('FilterForm');
        $this->activeFilters = $this->get('ActiveFilters');
        $this->trabajadores  = $this->loadTrabajadores();

        ToolbarHelper::title(Text::_('COM_JORNADASALUDABLE_DOCUMENTOS'), 'file');

        parent::display($tpl);
    }

    private function loadTrabajadores(): array
    {
        $db = Factory::getContainer()->get('DatabaseDriver');
        $q  = $db->getQuery(true)
            ->select([
                $db->quoteName('id'),
                $db->quoteName('nif'),
                $db->quoteName('nombre'),
                $db->quoteName('apellidos'),
            ])
            ->from($db->quoteName('#__js_users'))
            ->where($db->quoteName('deleted_at') . ' IS NULL')
            ->where($db->quoteName('activo') . ' = 1')
            ->order($db->quoteName('apellidos') . ' ASC');
        $db->setQuery($q);
        return $db->loadObjectList() ?: [];
    }
}
