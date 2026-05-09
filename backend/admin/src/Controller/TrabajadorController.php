<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Controller\FormController;

final class TrabajadorController extends FormController
{
    protected $view_item = 'trabajador';
    protected $view_list = 'trabajadores';
    protected $text_prefix = 'COM_JORNADASALUDABLE_TRABAJADOR';
}
