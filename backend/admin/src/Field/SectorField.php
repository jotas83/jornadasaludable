<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Field;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Form\Field\ListField;

final class SectorField extends ListField
{
    protected $type = 'Sector';

    protected function getOptions(): array
    {
        $db = Factory::getContainer()->get('DatabaseDriver');
        $q = $db->getQuery(true)
            ->select([$db->quoteName('id', 'value'), $db->quoteName('nombre', 'text')])
            ->from($db->quoteName('#__js_sectores'))
            ->where($db->quoteName('activo') . ' = 1')
            ->order($db->quoteName('nombre') . ' ASC');
        $db->setQuery($q);
        $options = [];
        foreach ($db->loadObjectList() as $row) {
            $options[] = (object) ['value' => $row->value, 'text' => $row->text];
        }
        return array_merge(parent::getOptions(), $options);
    }
}
