<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Model\ListModel;
use Joomla\Database\QueryInterface;

final class TrabajadoresModel extends ListModel
{
    public function __construct($config = [])
    {
        if (empty($config['filter_fields'])) {
            $config['filter_fields'] = [
                'id', 'a.id',
                'nif', 'a.nif',
                'apellidos', 'a.apellidos',
                'empresa', 'empresa_razon_social',
                'activo', 'a.activo',
            ];
        }
        parent::__construct($config);
    }

    protected function populateState($ordering = 'a.apellidos', $direction = 'ASC')
    {
        $this->setState('filter.search',  $this->getUserStateFromRequest($this->context . '.filter.search', 'filter_search', ''));
        $this->setState('filter.empresa', $this->getUserStateFromRequest($this->context . '.filter.empresa', 'filter_empresa', ''));
        $this->setState('filter.activo',  $this->getUserStateFromRequest($this->context . '.filter.activo', 'filter_activo', ''));
        parent::populateState($ordering, $direction);
    }

    protected function getStoreId($id = ''): string
    {
        $id .= ':' . $this->getState('filter.search');
        $id .= ':' . $this->getState('filter.empresa');
        $id .= ':' . $this->getState('filter.activo');
        return parent::getStoreId($id);
    }

    protected function getListQuery(): QueryInterface
    {
        $db = $this->getDatabase();
        $q  = $db->getQuery(true);

        $q->select([
            'a.id', 'a.nif', 'a.nombre', 'a.apellidos', 'a.email',
            'a.idioma', 'a.activo', 'a.last_login_at', 'a.created_at',
            'c.empresa_id', 'e.razon_social AS empresa_razon_social',
        ])
            ->from($db->quoteName('#__js_users', 'a'))
            ->join('LEFT', '(SELECT cc.user_id, MAX(cc.empresa_id) AS empresa_id FROM ' . $db->quoteName('#__js_contratos', 'cc') .
                ' WHERE cc.vigente = 1 GROUP BY cc.user_id) AS c ON c.user_id = a.id')
            ->join('LEFT', $db->quoteName('#__js_empresas', 'e') . ' ON e.id = c.empresa_id')
            ->where($db->quoteName('a.deleted_at') . ' IS NULL');

        if ($search = trim((string) $this->getState('filter.search'))) {
            $like = '%' . $db->escape($search, true) . '%';
            $q->where('(a.nif LIKE ' . $db->quote($like) .
                ' OR a.nombre LIKE ' . $db->quote($like) .
                ' OR a.apellidos LIKE ' . $db->quote($like) .
                ' OR a.email LIKE ' . $db->quote($like) . ')');
        }
        if ($empresa = $this->getState('filter.empresa')) {
            $q->where('c.empresa_id = ' . (int) $empresa);
        }
        $activo = $this->getState('filter.activo');
        if ($activo !== '' && $activo !== null) {
            $q->where($db->quoteName('a.activo') . ' = ' . (int) $activo);
        }

        $orderCol = $this->state->get('list.ordering', 'a.apellidos');
        $orderDir = strtoupper((string) $this->state->get('list.direction', 'ASC'));
        $q->order($db->escape($orderCol) . ' ' . ($orderDir === 'DESC' ? 'DESC' : 'ASC'));

        return $q;
    }
}
