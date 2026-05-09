<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Form\Form;
use Joomla\CMS\MVC\Model\AdminModel;

final class LicenciaModel extends AdminModel
{
    protected $text_prefix = 'COM_JORNADASALUDABLE_LICENCIA';

    public function getTable($name = 'Licencia', $prefix = 'Administrator', $options = [])
    {
        return parent::getTable($name, $prefix, $options);
    }

    public function getForm($data = [], $loadData = true): Form|bool
    {
        $form = $this->loadForm(
            'com_jornadasaludable.licencia',
            'licencia',
            ['control' => 'jform', 'load_data' => $loadData]
        );
        return $form ?: false;
    }

    protected function loadFormData()
    {
        $data = Factory::getApplication()->getUserState('com_jornadasaludable.edit.licencia.data', []);
        if (empty($data)) {
            $data = $this->getItem();
            $empresaId = (int) Factory::getApplication()->getInput()->getInt('empresa_id', 0);
            if ($empresaId > 0 && empty($data->empresa_id)) {
                $data->empresa_id = $empresaId;
            }
        }
        return $data;
    }

    public function save($data): bool
    {
        if (!empty($data['activa']) && (int) $data['activa'] === 1 && !empty($data['empresa_id'])) {
            $db = Factory::getContainer()->get('DatabaseDriver');
            $q  = $db->getQuery(true)
                ->update($db->quoteName('#__js_licencias'))
                ->set($db->quoteName('activa') . ' = 0')
                ->where($db->quoteName('empresa_id') . ' = ' . (int) $data['empresa_id']);
            if (!empty($data['id'])) {
                $q->where($db->quoteName('id') . ' <> ' . (int) $data['id']);
            }
            $db->setQuery($q)->execute();
        }
        return parent::save($data);
    }
}
