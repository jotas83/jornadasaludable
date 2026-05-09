<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\View\Empresa;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\View\GenericDataException;
use Joomla\CMS\MVC\View\HtmlView as BaseHtmlView;
use Joomla\CMS\Toolbar\Toolbar;
use Joomla\CMS\Toolbar\ToolbarHelper;

final class HtmlView extends BaseHtmlView
{
    public $form;
    public $item;
    public $licencias = [];

    public function display($tpl = null): void
    {
        $this->form = $this->get('Form');
        $this->item = $this->get('Item');

        if ($errs = $this->get('Errors')) {
            throw new GenericDataException(implode("\n", $errs), 500);
        }

        if (!empty($this->item->id)) {
            $db = Factory::getContainer()->get('DatabaseDriver');
            $q = $db->getQuery(true)
                ->select('*')
                ->from($db->quoteName('#__js_licencias'))
                ->where($db->quoteName('empresa_id') . ' = ' . (int) $this->item->id)
                ->order($db->quoteName('fecha_inicio') . ' DESC');
            $db->setQuery($q);
            $this->licencias = $db->loadObjectList() ?: [];
        }

        $isNew = empty($this->item->id);
        ToolbarHelper::title(
            Text::_($isNew ? 'COM_JORNADASALUDABLE_EMPRESA_NUEVA' : 'COM_JORNADASALUDABLE_EMPRESA_EDITAR'),
            'briefcase'
        );

        $bar = Toolbar::getInstance();
        $bar->apply('empresa.apply');
        $bar->save('empresa.save');
        $bar->cancel('empresa.cancel', $isNew ? 'JTOOLBAR_CANCEL' : 'JTOOLBAR_CLOSE');

        parent::display($tpl);
    }
}
