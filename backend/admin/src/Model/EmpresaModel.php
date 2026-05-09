<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Form\Form;
use Joomla\CMS\MVC\Model\AdminModel;

final class EmpresaModel extends AdminModel
{
    protected $text_prefix = 'COM_JORNADASALUDABLE_EMPRESA';

    public function getTable($name = 'Empresa', $prefix = 'Administrator', $options = [])
    {
        return parent::getTable($name, $prefix, $options);
    }

    public function getForm($data = [], $loadData = true): Form|bool
    {
        $form = $this->loadForm(
            'com_jornadasaludable.empresa',
            'empresa',
            ['control' => 'jform', 'load_data' => $loadData]
        );
        return $form ?: false;
    }

    protected function loadFormData()
    {
        $data = Factory::getApplication()->getUserState('com_jornadasaludable.edit.empresa.data', []);
        if (empty($data)) {
            $data = $this->getItem();
        }
        return $data;
    }
}
