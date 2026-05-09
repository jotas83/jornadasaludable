<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Form\Form;
use Joomla\CMS\MVC\Model\AdminModel;

final class ContratoModel extends AdminModel
{
    protected $text_prefix = 'COM_JORNADASALUDABLE_CONTRATO';

    public function getTable($name = 'Contrato', $prefix = 'Administrator', $options = [])
    {
        return parent::getTable($name, $prefix, $options);
    }

    public function getForm($data = [], $loadData = true): Form|bool
    {
        $form = $this->loadForm(
            'com_jornadasaludable.contrato',
            'contrato',
            ['control' => 'jform', 'load_data' => $loadData]
        );
        return $form ?: false;
    }

    protected function loadFormData()
    {
        $data = Factory::getApplication()->getUserState('com_jornadasaludable.edit.contrato.data', []);
        if (empty($data)) {
            $data = $this->getItem();
            $userId = (int) Factory::getApplication()->getInput()->getInt('user_id', 0);
            if ($userId > 0 && empty($data->user_id)) {
                $data->user_id = $userId;
            }
        }
        return $data;
    }

    public function save($data): bool
    {
        if (!empty($data['vigente']) && (int) $data['vigente'] === 1 && !empty($data['user_id'])) {
            $db = Factory::getContainer()->get('DatabaseDriver');
            $q  = $db->getQuery(true)
                ->update($db->quoteName('#__js_contratos'))
                ->set($db->quoteName('vigente') . ' = 0')
                ->where($db->quoteName('user_id') . ' = ' . (int) $data['user_id']);
            if (!empty($data['id'])) {
                $q->where($db->quoteName('id') . ' <> ' . (int) $data['id']);
            }
            $db->setQuery($q)->execute();
        }
        return parent::save($data);
    }
}
