<?php
// Modelo admin del calendario de jornadas: carga el trabajador y las jornadas
// del mes seleccionado a partir de #__js_jornadas filtrando por user_id + fecha.

declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\MVC\Model\BaseDatabaseModel;

final class CalendarioModel extends BaseDatabaseModel
{
    public function getTrabajador(int $userId): ?object
    {
        if ($userId <= 0) {
            return null;
        }
        $db = $this->getDatabase();
        $q  = $db->getQuery(true)
            ->select([
                $db->quoteName('id'),
                $db->quoteName('nif'),
                $db->quoteName('nombre'),
                $db->quoteName('apellidos'),
                $db->quoteName('email'),
            ])
            ->from($db->quoteName('#__js_users'))
            ->where($db->quoteName('id') . ' = ' . $userId)
            ->where($db->quoteName('deleted_at') . ' IS NULL');
        $db->setQuery($q);
        $row = $db->loadObject();
        return $row ?: null;
    }

    /**
     * Devuelve las jornadas del mes indexadas por día (1..31).
     *
     * @return array<int, object>
     */
    public function getJornadasMes(int $userId, int $year, int $month): array
    {
        if ($userId <= 0 || $year < 1970 || $month < 1 || $month > 12) {
            return [];
        }

        $first = sprintf('%04d-%02d-01', $year, $month);
        $last  = date('Y-m-t', strtotime($first));

        $db = $this->getDatabase();
        $q  = $db->getQuery(true)
            ->select([
                'j.id', 'j.fecha', 'j.hora_inicio', 'j.hora_fin',
                'j.minutos_trabajados', 'j.minutos_pausa', 'j.minutos_extra',
                'j.estado', 'j.observaciones', 'j.validada_at',
            ])
            ->from($db->quoteName('#__js_jornadas', 'j'))
            ->where($db->quoteName('j.user_id') . ' = ' . $userId)
            ->where($db->quoteName('j.fecha') . ' >= ' . $db->quote($first))
            ->where($db->quoteName('j.fecha') . ' <= ' . $db->quote($last))
            ->where($db->quoteName('j.deleted_at') . ' IS NULL')
            ->order($db->quoteName('j.fecha') . ' ASC');
        $db->setQuery($q);
        $rows = $db->loadObjectList() ?: [];

        $byDay = [];
        foreach ($rows as $row) {
            $day = (int) date('j', strtotime((string) $row->fecha));
            $byDay[$day] = $row;
        }
        return $byDay;
    }
}
