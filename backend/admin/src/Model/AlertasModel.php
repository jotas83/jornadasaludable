<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\MVC\Model\ListModel;
use Joomla\Database\QueryInterface;

final class AlertasModel extends ListModel
{
    public function __construct($config = [])
    {
        if (empty($config['filter_fields'])) {
            $config['filter_fields'] = [
                'id', 'a.id',
                'fecha_evento', 'a.fecha_evento',
                'tipo_id', 'a.tipo_id',
                'leida', 'a.leida',
                'severidad', 't.severidad',
                'empresa', 'empresa_id',
                'trabajador', 'a.user_id',
            ];
        }
        parent::__construct($config);
    }

    protected function populateState($ordering = 'a.fecha_evento', $direction = 'DESC')
    {
        $this->setState('filter.search',     $this->getUserStateFromRequest($this->context . '.filter.search', 'filter_search', ''));
        $this->setState('filter.empresa',    $this->getUserStateFromRequest($this->context . '.filter.empresa', 'filter_empresa', ''));
        $this->setState('filter.trabajador', $this->getUserStateFromRequest($this->context . '.filter.trabajador', 'filter_trabajador', ''));
        $this->setState('filter.tipo',       $this->getUserStateFromRequest($this->context . '.filter.tipo', 'filter_tipo', ''));
        $this->setState('filter.severidad',  $this->getUserStateFromRequest($this->context . '.filter.severidad', 'filter_severidad', ''));
        $this->setState('filter.leida',      $this->getUserStateFromRequest($this->context . '.filter.leida', 'filter_leida', ''));
        parent::populateState($ordering, $direction);
    }

    protected function getStoreId($id = ''): string
    {
        $id .= ':' . $this->getState('filter.search');
        $id .= ':' . $this->getState('filter.empresa');
        $id .= ':' . $this->getState('filter.trabajador');
        $id .= ':' . $this->getState('filter.tipo');
        $id .= ':' . $this->getState('filter.severidad');
        $id .= ':' . $this->getState('filter.leida');
        return parent::getStoreId($id);
    }

    protected function getListQuery(): QueryInterface
    {
        $db = $this->getDatabase();
        $q  = $db->getQuery(true);

        $q->select([
            'a.id', 'a.fecha_evento', 'a.mensaje', 'a.valor_detectado',
            'a.leida', 'a.leida_at', 'a.created_at',
            'a.user_id', 'a.tipo_id',
            't.codigo AS tipo_codigo', 't.nombre AS tipo_nombre', 't.severidad',
            'u.nombre AS user_nombre', 'u.apellidos AS user_apellidos', 'u.nif AS user_nif',
            'c.empresa_id', 'e.razon_social AS empresa_razon_social',
        ])
            ->from($db->quoteName('#__js_alertas', 'a'))
            ->join('LEFT', $db->quoteName('#__js_alertas_tipos', 't') . ' ON t.id = a.tipo_id')
            ->join('LEFT', $db->quoteName('#__js_users', 'u') . ' ON u.id = a.user_id')
            ->join('LEFT', '(SELECT cc.user_id, cc.empresa_id FROM ' . $db->quoteName('#__js_contratos', 'cc') .
                ' WHERE cc.vigente = 1) AS c ON c.user_id = a.user_id')
            ->join('LEFT', $db->quoteName('#__js_empresas', 'e') . ' ON e.id = c.empresa_id');

        if ($search = trim((string) $this->getState('filter.search'))) {
            $like = '%' . $db->escape($search, true) . '%';
            $q->where('(a.mensaje LIKE ' . $db->quote($like) . ' OR u.nif LIKE ' . $db->quote($like) . ')');
        }
        if ($empresa = $this->getState('filter.empresa')) {
            $q->where('c.empresa_id = ' . (int) $empresa);
        }
        if ($trab = $this->getState('filter.trabajador')) {
            $q->where($db->quoteName('a.user_id') . ' = ' . (int) $trab);
        }
        if ($tipo = $this->getState('filter.tipo')) {
            $q->where($db->quoteName('a.tipo_id') . ' = ' . (int) $tipo);
        }
        if ($sev = $this->getState('filter.severidad')) {
            $q->where($db->quoteName('t.severidad') . ' = ' . $db->quote((string) $sev));
        }
        $leida = $this->getState('filter.leida');
        if ($leida !== '' && $leida !== null) {
            $q->where($db->quoteName('a.leida') . ' = ' . (int) $leida);
        }

        $orderCol = $this->state->get('list.ordering', 'a.fecha_evento');
        $orderDir = strtoupper((string) $this->state->get('list.direction', 'DESC'));
        $q->order($db->escape($orderCol) . ' ' . ($orderDir === 'ASC' ? 'ASC' : 'DESC'));

        return $q;
    }
}
