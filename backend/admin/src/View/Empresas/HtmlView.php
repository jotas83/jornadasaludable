<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\View\Empresas;

defined('_JEXEC') or die;

use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\View\HtmlView as BaseHtmlView;
use Joomla\CMS\Toolbar\Toolbar;
use Joomla\CMS\Toolbar\ToolbarHelper;

final class HtmlView extends BaseHtmlView
{
    public $items;
    public $pagination;
    public $state;
    public $filterForm;
    public $activeFilters;

    public function display($tpl = null): void
    {
        $this->items         = $this->get('Items');
        $this->pagination    = $this->get('Pagination');
        $this->state         = $this->get('State');
        $this->filterForm    = $this->get('FilterForm');
        $this->activeFilters = $this->get('ActiveFilters');

        ToolbarHelper::title(Text::_('COM_JORNADASALUDABLE_EMPRESAS'), 'briefcase');

        $bar = Toolbar::getInstance();
        $bar->addNew('empresa.add');
        $bar->edit('empresa.edit')->listCheck(true);
        $bar->standardButton('publish', 'COM_JORNADASALUDABLE_ESTADO_ACTIVO', 'empresas.activar')->icon('icon-publish')->listCheck(true);
        $bar->standardButton('unpublish', 'COM_JORNADASALUDABLE_ESTADO_INACTIVO', 'empresas.desactivar')->icon('icon-unpublish')->listCheck(true);

        parent::display($tpl);
    }
}
