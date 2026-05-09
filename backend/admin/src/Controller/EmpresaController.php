<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Controller\FormController;

final class EmpresaController extends FormController
{
    protected $view_item = 'empresa';
    protected $view_list = 'empresas';
    protected $text_prefix = 'COM_JORNADASALUDABLE_EMPRESA';
}
