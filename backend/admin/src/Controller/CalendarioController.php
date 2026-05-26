<?php
// Controlador admin del calendario de jornadas por trabajador.
// Acciones: index (vista del calendario) y validar (marcar jornada como VALIDADA,
// con posibilidad de corregir hora_inicio y/o hora_fin y recalcular minutos_trabajados).

declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Controller;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\Controller\BaseController;
use Joomla\CMS\Router\Route;
use Joomla\CMS\Session\Session;

final class CalendarioController extends BaseController
{
    protected $default_view = 'calendario';

    public function validar(): void
    {
        Session::checkToken() or $this->app->enqueueMessage(Text::_('JINVALID_TOKEN'), 'error');

        $jornadaId   = (int) $this->input->getInt('jornada_id', 0);
        $userId      = (int) $this->input->getInt('user_id', 0);
        $year        = (int) $this->input->getInt('year', 0);
        $month       = (int) $this->input->getInt('month', 0);
        $horaIniInp  = trim((string) $this->input->getString('hora_inicio', ''));
        $horaFinInp  = trim((string) $this->input->getString('hora_fin', ''));

        $redirect = 'index.php?option=com_jornadasaludable&view=calendario'
            . '&user_id=' . $userId . '&year=' . $year . '&month=' . $month;

        if ($jornadaId <= 0 || $userId <= 0) {
            $this->setRedirect(
                Route::_($redirect, false),
                Text::_('COM_JORNADASALUDABLE_CALENDARIO_ERROR_PARAMS'),
                'error'
            );
            return;
        }

        $db = Factory::getContainer()->get('DatabaseDriver');

        $sel = $db->getQuery(true)
            ->select(['id', 'fecha', 'hora_inicio', 'hora_fin', 'minutos_pausa'])
            ->from($db->quoteName('#__js_jornadas'))
            ->where($db->quoteName('id') . ' = ' . $jornadaId)
            ->where($db->quoteName('user_id') . ' = ' . $userId)
            ->where($db->quoteName('deleted_at') . ' IS NULL');
        $db->setQuery($sel);
        $jornada = $db->loadObject();

        if (!$jornada) {
            $this->setRedirect(
                Route::_($redirect, false),
                Text::_('COM_JORNADASALUDABLE_CALENDARIO_ERROR_PARAMS'),
                'error'
            );
            return;
        }

        $reHora = '/^([01]\d|2[0-3]):([0-5]\d)$/';
        $fecha  = (string) $jornada->fecha;

        // Hora de inicio: si llega válida y distinta a la actual, se reemplaza.
        $horaIniDt = null;
        if ($horaIniInp !== '' && preg_match($reHora, $horaIniInp) === 1) {
            $candidate = $fecha . ' ' . $horaIniInp . ':00';
            $actualHHMM = '';
            if (!empty($jornada->hora_inicio) && !str_starts_with((string) $jornada->hora_inicio, '0000')) {
                $tsAct = strtotime((string) $jornada->hora_inicio);
                $actualHHMM = $tsAct ? date('H:i', $tsAct) : '';
            }
            if ($horaIniInp !== $actualHHMM) {
                $horaIniDt = $candidate;
            }
        }

        // Hora de fin: si llega válida, se acepta.
        $horaFinDt = null;
        if ($horaFinInp !== '' && preg_match($reHora, $horaFinInp) === 1) {
            $horaFinDt = $fecha . ' ' . $horaFinInp . ':00';
        }

        // Resolver hora_inicio / hora_fin efectivas para el cálculo:
        // si solo viene una, se usa la que ya estaba en BD para la otra.
        $iniEfectivoDt = $horaIniDt ?? (!empty($jornada->hora_inicio) ? (string) $jornada->hora_inicio : null);
        $finEfectivoDt = $horaFinDt ?? (!empty($jornada->hora_fin) ? (string) $jornada->hora_fin : null);

        $minTrabNuevo = null;
        if ($iniEfectivoDt && $finEfectivoDt
            && !str_starts_with($iniEfectivoDt, '0000')
            && !str_starts_with($finEfectivoDt, '0000')
        ) {
            $tsIni = strtotime($iniEfectivoDt);
            $tsFin = strtotime($finEfectivoDt);
            if ($tsIni !== false && $tsFin !== false && $tsFin > $tsIni) {
                $diffMin     = (int) floor(($tsFin - $tsIni) / 60);
                $minTrabNuevo = max(0, $diffMin - (int) ($jornada->minutos_pausa ?? 0));
            } else {
                $minTrabNuevo = 0;
            }
        }

        $upd = $db->getQuery(true)
            ->update($db->quoteName('#__js_jornadas'))
            ->set($db->quoteName('estado') . ' = ' . $db->quote('VALIDADA'))
            ->set($db->quoteName('validada_at') . ' = ' . $db->quote(gmdate('Y-m-d H:i:s')));

        if ($horaIniDt !== null) {
            $upd->set($db->quoteName('hora_inicio') . ' = ' . $db->quote($horaIniDt));
        }
        if ($horaFinDt !== null) {
            $upd->set($db->quoteName('hora_fin') . ' = ' . $db->quote($horaFinDt));
        }
        if (($horaIniDt !== null || $horaFinDt !== null) && $minTrabNuevo !== null) {
            $upd->set($db->quoteName('minutos_trabajados') . ' = ' . (int) $minTrabNuevo);
        }

        $upd->where($db->quoteName('id') . ' = ' . $jornadaId)
            ->where($db->quoteName('user_id') . ' = ' . $userId)
            ->where($db->quoteName('deleted_at') . ' IS NULL');

        $db->setQuery($upd)->execute();

        $this->setRedirect(
            Route::_($redirect, false),
            Text::_('COM_JORNADASALUDABLE_CALENDARIO_JORNADA_VALIDADA')
        );
    }
}
