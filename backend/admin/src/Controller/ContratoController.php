<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Controller\FormController;
use Joomla\CMS\Router\Route;

final class ContratoController extends FormController
{
    protected $view_item = 'contrato';
    protected $view_list = 'trabajadores';
    protected $text_prefix = 'COM_JORNADASALUDABLE_CONTRATO';

    protected function getRedirectToItemAppend($recordId = null, $urlVar = 'id')
    {
        $append = parent::getRedirectToItemAppend($recordId, $urlVar);
        $userId = (int) $this->input->getInt('user_id', 0);
        if ($userId > 0) {
            $append .= '&user_id=' . $userId;
        }
        return $append;
    }

    protected function getRedirectToListAppend()
    {
        return '';
    }

    public function cancel($key = null)
    {
        $result = parent::cancel($key);
        $userId = (int) $this->input->getInt('user_id', 0);
        if ($userId > 0) {
            $this->setRedirect(Route::_('index.php?option=com_jornadasaludable&task=trabajador.edit&id=' . $userId, false));
        }
        return $result;
    }
}
