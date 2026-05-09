<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Controller\FormController;

final class CentroController extends FormController
{
    protected $view_item = 'centro';
    protected $view_list = 'centros';
    protected $text_prefix = 'COM_JORNADASALUDABLE_CENTRO';
}
