<?php
// Vista HTML del calendario de jornadas de un trabajador.
// Recoge user_id, year y month del input, normaliza el mes y prepara el grid.

declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\View\Calendario;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\View\HtmlView as BaseHtmlView;
use Joomla\CMS\Toolbar\Toolbar;
use Joomla\CMS\Toolbar\ToolbarHelper;
use Joomla\Component\Jornadasaludable\Administrator\Model\CalendarioModel;

final class HtmlView extends BaseHtmlView
{
    public ?object $trabajador = null;
    public array $jornadasMes = [];
    public int $userId = 0;
    public int $year = 0;
    public int $month = 0;
    public int $diaSeleccionado = 0;
    public ?object $jornadaSeleccionada = null;
    public int $prevYear = 0;
    public int $prevMonth = 0;
    public int $nextYear = 0;
    public int $nextMonth = 0;

    public function display($tpl = null): void
    {
        $app   = Factory::getApplication();
        $input = $app->getInput();

        $this->userId = (int) $input->getInt('user_id', 0);
        $this->year   = (int) $input->getInt('year', 0);
        $this->month  = (int) $input->getInt('month', 0);
        $this->diaSeleccionado = (int) $input->getInt('dia', 0);

        if ($this->year < 1970 || $this->month < 1 || $this->month > 12) {
            $this->year  = (int) date('Y');
            $this->month = (int) date('n');
        }

        $prev = strtotime(sprintf('%04d-%02d-01 -1 month', $this->year, $this->month));
        $next = strtotime(sprintf('%04d-%02d-01 +1 month', $this->year, $this->month));
        $this->prevYear  = (int) date('Y', $prev);
        $this->prevMonth = (int) date('n', $prev);
        $this->nextYear  = (int) date('Y', $next);
        $this->nextMonth = (int) date('n', $next);

        /** @var CalendarioModel $model */
        $model = $this->getModel();
        $this->trabajador  = $model->getTrabajador($this->userId);
        $this->jornadasMes = $model->getJornadasMes($this->userId, $this->year, $this->month);

        if ($this->diaSeleccionado > 0 && isset($this->jornadasMes[$this->diaSeleccionado])) {
            $this->jornadaSeleccionada = $this->jornadasMes[$this->diaSeleccionado];
        }

        ToolbarHelper::title(Text::_('COM_JORNADASALUDABLE_CALENDARIO_TITULO'), 'calendar');

        $bar = Toolbar::getInstance();
        $bar->link(
            'COM_JORNADASALUDABLE_CALENDARIO_VOLVER',
            'index.php?option=com_jornadasaludable&view=trabajadores'
        )->icon('icon-arrow-left');

        parent::display($tpl);
    }
}
