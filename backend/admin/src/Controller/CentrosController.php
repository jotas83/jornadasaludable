<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Controller\AdminController;

final class CentrosController extends AdminController
{
    protected $text_prefix = 'COM_JORNADASALUDABLE_CENTRO';

    public function getModel($name = 'Centro', $prefix = 'Administrator', $config = ['ignore_request' => true])
    {
        return parent::getModel($name, $prefix, $config);
    }
}
