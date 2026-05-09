<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Model\ListModel;
use Joomla\Database\QueryInterface;

final class DocumentosModel extends ListModel
{
    public function __construct($config = [])
    {
        if (empty($config['filter_fields'])) {
            $config['filter_fields'] = [
                'id', 'a.id',
                'tipo', 'a.tipo',
                'created_at', 'a.created_at',
                'trabajador', 'a.user_id',
            ];
        }
        parent::__construct($config);
    }

    protected function populateState($ordering = 'a.created_at', $direction = 'DESC')
    {
        $this->setState('filter.search',     $this->getUserStateFromRequest($this->context . '.filter.search', 'filter_search', ''));
        $this->setState('filter.trabajador', $this->getUserStateFromRequest($this->context . '.filter.trabajador', 'filter_trabajador', ''));
        $this->setState('filter.tipo',       $this->getUserStateFromRequest($this->context . '.filter.tipo', 'filter_tipo', ''));
        parent::populateState($ordering, $direction);
    }

    protected function getStoreId($id = ''): string
    {
        $id .= ':' . $this->getState('filter.search');
        $id .= ':' . $this->getState('filter.trabajador');
        $id .= ':' . $this->getState('filter.tipo');
        return parent::getStoreId($id);
    }

    protected function getListQuery(): QueryInterface
    {
        $db = $this->getDatabase();
        $q  = $db->getQuery(true);

        $q->select([
            'a.id', 'a.uuid', 'a.tipo', 'a.periodo_desde', 'a.periodo_hasta',
            'a.nombre_fichero', 'a.ruta_storage', 'a.tamano_bytes',
            'a.descargado', 'a.created_at',
            'a.user_id',
            'u.nombre AS user_nombre', 'u.apellidos AS user_apellidos', 'u.nif AS user_nif',
        ])
            ->from($db->quoteName('#__js_documentos', 'a'))
            ->join('LEFT', $db->quoteName('#__js_users', 'u') . ' ON u.id = a.user_id')
            ->where($db->quoteName('a.deleted_at') . ' IS NULL');

        if ($search = trim((string) $this->getState('filter.search'))) {
            $like = '%' . $db->escape($search, true) . '%';
            $q->where('(a.nombre_fichero LIKE ' . $db->quote($like) . ' OR u.nif LIKE ' . $db->quote($like) . ')');
        }
        if ($trab = $this->getState('filter.trabajador')) {
            $q->where($db->quoteName('a.user_id') . ' = ' . (int) $trab);
        }
        if ($tipo = $this->getState('filter.tipo')) {
            $q->where($db->quoteName('a.tipo') . ' = ' . $db->quote((string) $tipo));
        }

        $orderCol = $this->state->get('list.ordering', 'a.created_at');
        $orderDir = strtoupper((string) $this->state->get('list.direction', 'DESC'));
        $q->order($db->escape($orderCol) . ' ' . ($orderDir === 'ASC' ? 'ASC' : 'DESC'));

        return $q;
    }
}
