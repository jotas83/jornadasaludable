<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Field;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Form\Field\ListField;

final class EmpresaField extends ListField
{
    protected $type = 'Empresa';

    protected function getOptions(): array
    {
        $db = Factory::getContainer()->get('DatabaseDriver');
        $q = $db->getQuery(true)
            ->select([$db->quoteName('id', 'value'), $db->quoteName('razon_social', 'text')])
            ->from($db->quoteName('#__js_empresas'))
            ->where($db->quoteName('deleted_at') . ' IS NULL')
            ->order($db->quoteName('razon_social') . ' ASC');
        $db->setQuery($q);
        $options = [];
        foreach ($db->loadObjectList() as $row) {
            $options[] = (object) ['value' => $row->value, 'text' => $row->text];
        }
        return array_merge(parent::getOptions(), $options);
    }
}
