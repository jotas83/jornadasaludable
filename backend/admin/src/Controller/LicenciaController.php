<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Controller\FormController;
use Joomla\CMS\Router\Route;

final class LicenciaController extends FormController
{
    protected $view_item = 'licencia';
    protected $view_list = 'empresas';
    protected $text_prefix = 'COM_JORNADASALUDABLE_LICENCIA';

    protected function getRedirectToItemAppend($recordId = null, $urlVar = 'id')
    {
        $append = parent::getRedirectToItemAppend($recordId, $urlVar);
        $eId = (int) $this->input->getInt('empresa_id', 0);
        if ($eId > 0) {
            $append .= '&empresa_id=' . $eId;
        }
        return $append;
    }

    public function cancel($key = null)
    {
        $result = parent::cancel($key);
        $eId = (int) $this->input->getInt('empresa_id', 0);
        if ($eId > 0) {
            $this->setRedirect(Route::_('index.php?option=com_jornadasaludable&task=empresa.edit&id=' . $eId, false));
        }
        return $result;
    }
}
