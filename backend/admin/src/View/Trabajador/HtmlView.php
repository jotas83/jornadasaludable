<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\View\Trabajador;

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
    public $contratos = [];

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
                ->select(['c.*', 'e.razon_social AS empresa_razon_social'])
                ->from($db->quoteName('#__js_contratos', 'c'))
                ->join('LEFT', $db->quoteName('#__js_empresas', 'e') . ' ON e.id = c.empresa_id')
                ->where($db->quoteName('c.user_id') . ' = ' . (int) $this->item->id)
                ->order($db->quoteName('c.fecha_inicio') . ' DESC');
            $db->setQuery($q);
            $this->contratos = $db->loadObjectList() ?: [];
        }

        $isNew = empty($this->item->id);
        ToolbarHelper::title(
            Text::_($isNew ? 'COM_JORNADASALUDABLE_TRABAJADOR_NUEVO' : 'COM_JORNADASALUDABLE_TRABAJADOR_EDITAR'),
            'user'
        );

        $bar = Toolbar::getInstance();
        $bar->apply('trabajador.apply');
        $bar->save('trabajador.save');
        $bar->cancel('trabajador.cancel', $isNew ? 'JTOOLBAR_CANCEL' : 'JTOOLBAR_CLOSE');

        parent::display($tpl);
    }
}
