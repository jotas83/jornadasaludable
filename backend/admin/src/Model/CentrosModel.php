<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Model\ListModel;
use Joomla\Database\QueryInterface;

final class CentrosModel extends ListModel
{
    public function __construct($config = [])
    {
        if (empty($config['filter_fields'])) {
            $config['filter_fields'] = ['id', 'a.id', 'codigo', 'nombre', 'empresa', 'a.empresa_id'];
        }
        parent::__construct($config);
    }

    protected function populateState($ordering = 'a.nombre', $direction = 'ASC')
    {
        $this->setState('filter.search',  $this->getUserStateFromRequest($this->context . '.filter.search', 'filter_search', ''));
        $this->setState('filter.empresa', $this->getUserStateFromRequest($this->context . '.filter.empresa', 'filter_empresa', ''));
        parent::populateState($ordering, $direction);
    }

    protected function getStoreId($id = ''): string
    {
        $id .= ':' . $this->getState('filter.search');
        $id .= ':' . $this->getState('filter.empresa');
        return parent::getStoreId($id);
    }

    protected function getListQuery(): QueryInterface
    {
        $db = $this->getDatabase();
        $q  = $db->getQuery(true);

        $q->select([
            'a.id', 'a.codigo', 'a.nombre', 'a.direccion',
            'a.latitud', 'a.longitud', 'a.radio_geofence_m', 'a.activo',
            'a.empresa_id', 'e.razon_social AS empresa_razon_social',
        ])
            ->from($db->quoteName('#__js_centros_trabajo', 'a'))
            ->join('LEFT', $db->quoteName('#__js_empresas', 'e') . ' ON e.id = a.empresa_id');

        if ($search = trim((string) $this->getState('filter.search'))) {
            $like = '%' . $db->escape($search, true) . '%';
            $q->where('(a.codigo LIKE ' . $db->quote($like) . ' OR a.nombre LIKE ' . $db->quote($like) . ')');
        }
        if ($empresa = $this->getState('filter.empresa')) {
            $q->where($db->quoteName('a.empresa_id') . ' = ' . (int) $empresa);
        }

        $orderCol = $this->state->get('list.ordering', 'a.nombre');
        $orderDir = strtoupper((string) $this->state->get('list.direction', 'ASC'));
        $q->order($db->escape($orderCol) . ' ' . ($orderDir === 'DESC' ? 'DESC' : 'ASC'));

        return $q;
    }
}
