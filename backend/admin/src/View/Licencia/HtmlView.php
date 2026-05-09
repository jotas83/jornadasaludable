<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\View\Licencia;

defined('_JEXEC') or die;

use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\View\GenericDataException;
use Joomla\CMS\MVC\View\HtmlView as BaseHtmlView;
use Joomla\CMS\Toolbar\Toolbar;
use Joomla\CMS\Toolbar\ToolbarHelper;

final class HtmlView extends BaseHtmlView
{
    public $form;
    public $item;

    public function display($tpl = null): void
    {
        $this->form = $this->get('Form');
        $this->item = $this->get('Item');

        if ($errs = $this->get('Errors')) {
            throw new GenericDataException(implode("\n", $errs), 500);
        }

        $isNew = empty($this->item->id);
        ToolbarHelper::title(
            Text::_($isNew ? 'COM_JORNADASALUDABLE_LICENCIA_NUEVA' : 'COM_JORNADASALUDABLE_LICENCIA'),
            'lock'
        );

        $bar = Toolbar::getInstance();
        $bar->apply('licencia.apply');
        $bar->save('licencia.save');
        $bar->cancel('licencia.cancel');

        parent::display($tpl);
    }
}
