<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Model\ListModel;
use Joomla\Database\QueryInterface;

final class EmpresasModel extends ListModel
{
    public function __construct($config = [])
    {
        if (empty($config['filter_fields'])) {
            $config['filter_fields'] = [
                'id', 'a.id',
                'cif', 'a.cif',
                'razon_social', 'a.razon_social',
                'sector_id', 'a.sector_id',
                'activo', 'a.activo',
                'created_at', 'a.created_at',
            ];
        }
        parent::__construct($config);
    }

    protected function populateState($ordering = 'a.razon_social', $direction = 'ASC')
    {
        $this->setState('filter.search',  $this->getUserStateFromRequest($this->context . '.filter.search', 'filter_search', ''));
        $this->setState('filter.sector',  $this->getUserStateFromRequest($this->context . '.filter.sector', 'filter_sector', ''));
        $this->setState('filter.activo',  $this->getUserStateFromRequest($this->context . '.filter.activo', 'filter_activo', ''));
        parent::populateState($ordering, $direction);
    }

    protected function getStoreId($id = ''): string
    {
        $id .= ':' . $this->getState('filter.search');
        $id .= ':' . $this->getState('filter.sector');
        $id .= ':' . $this->getState('filter.activo');
        return parent::getStoreId($id);
    }

    protected function getListQuery(): QueryInterface
    {
        $db = $this->getDatabase();
        $q  = $db->getQuery(true);

        $q->select([
            'a.id', 'a.cif', 'a.razon_social', 'a.nombre_comercial',
            'a.sector_id', 'a.activo', 'a.created_at', 'a.deleted_at',
            's.nombre AS sector_nombre',
        ])
            ->from($db->quoteName('#__js_empresas', 'a'))
            ->join('LEFT', $db->quoteName('#__js_sectores', 's') . ' ON s.id = a.sector_id')
            ->where($db->quoteName('a.deleted_at') . ' IS NULL');

        if ($search = trim((string) $this->getState('filter.search'))) {
            $like = '%' . $db->escape($search, true) . '%';
            $q->where('(a.cif LIKE ' . $db->quote($like) . ' OR a.razon_social LIKE ' . $db->quote($like) . ' OR a.nombre_comercial LIKE ' . $db->quote($like) . ')');
        }
        if ($sector = $this->getState('filter.sector')) {
            $q->where($db->quoteName('a.sector_id') . ' = ' . (int) $sector);
        }
        $activo = $this->getState('filter.activo');
        if ($activo !== '' && $activo !== null) {
            $q->where($db->quoteName('a.activo') . ' = ' . (int) $activo);
        }

        $orderCol = $this->state->get('list.ordering', 'a.razon_social');
        $orderDir = strtoupper((string) $this->state->get('list.direction', 'ASC'));
        $q->order($db->escape($orderCol) . ' ' . ($orderDir === 'DESC' ? 'DESC' : 'ASC'));

        return $q;
    }
}
