<?php
// Plantilla del calendario mensual de jornadas (admin).
// Pinta cabecera con trabajador y navegación de mes, grid de 7 columnas con
// colores por estado y panel de detalle con botón Validar para la jornada
// seleccionada (?dia=N).

declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Language\Text;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Calendario\HtmlView $this */

$meses = [
    1 => 'Enero', 2 => 'Febrero', 3 => 'Marzo', 4 => 'Abril',
    5 => 'Mayo', 6 => 'Junio', 7 => 'Julio', 8 => 'Agosto',
    9 => 'Septiembre', 10 => 'Octubre', 11 => 'Noviembre', 12 => 'Diciembre',
];
$diasSemana = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

$firstTs   = strtotime(sprintf('%04d-%02d-01', $this->year, $this->month));
$diasMes   = (int) date('t', $firstTs);
$diaInicio = (int) date('N', $firstTs); // 1=Lun .. 7=Dom

$estadoClase = [
    'VALIDADA'  => 'js-cal-validada',
    'CERRADA'   => 'js-cal-cerrada',
    'CORREGIDA' => 'js-cal-corregida',
    'ABIERTA'   => 'js-cal-abierta',
];

$linkBase = 'index.php?option=com_jornadasaludable&view=calendario'
    . '&user_id=' . $this->userId;

$fmtHora = static function (?string $datetime): string {
    if ($datetime === null || $datetime === '' || str_starts_with($datetime, '0000')) {
        return '—';
    }
    $ts = strtotime($datetime);
    return $ts ? date('H:i', $ts) : '—';
};
$fmtMin = static function (int $min): string {
    return sprintf('%d:%02d', intdiv($min, 60), $min % 60);
};

$nombreTrab = $this->trabajador
    ? trim(($this->trabajador->apellidos ?? '') . ', ' . ($this->trabajador->nombre ?? ''))
    : Text::_('COM_JORNADASALUDABLE_CALENDARIO_TRABAJADOR_NO_ENCONTRADO');
?>
<style>
    .js-cal-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:1rem; }
    .js-cal-header h2 { margin:0; }
    .js-cal-nav a { margin:0 .25rem; }
    .js-cal-grid { display:grid; grid-template-columns: repeat(7, 1fr); gap:4px; }
    .js-cal-grid .js-cal-dow { font-weight:bold; text-align:center; padding:.25rem; background:#f1f3f5; }
    .js-cal-grid .js-cal-cell {
        min-height:70px; padding:.35rem; border:1px solid #dee2e6;
        background:#f8f9fa; color:#212529; text-align:left; position:relative;
    }
    .js-cal-grid .js-cal-cell.js-cal-empty { background:transparent; border:none; }
    .js-cal-grid .js-cal-cell .js-cal-day-num { font-weight:bold; font-size:.95rem; }
    .js-cal-grid .js-cal-cell .js-cal-min { display:block; font-size:.8rem; margin-top:.25rem; }
    .js-cal-grid a.js-cal-cell { text-decoration:none; }
    .js-cal-grid a.js-cal-cell:hover { outline:2px solid #0d6efd; }
    .js-cal-validada  { background:#d1e7dd !important; color:#0a3622 !important; }
    .js-cal-cerrada   { background:#cfe2ff !important; color:#052c65 !important; }
    .js-cal-corregida { background:#f8d7da !important; color:#58151c !important; }
    .js-cal-abierta   { background:#fff3cd !important; color:#664d03 !important; }
    .js-cal-legend { display:flex; gap:1rem; margin-top:1rem; flex-wrap:wrap; font-size:.85rem; }
    .js-cal-legend span { display:inline-block; padding:.15rem .5rem; border:1px solid #dee2e6; }
    .js-cal-detail { margin-top:1.5rem; padding:1rem; border:1px solid #dee2e6; background:#fff; }
    .js-cal-detail dt { font-weight:bold; }
    .js-cal-detail dl { display:grid; grid-template-columns: 180px 1fr; gap:.3rem .75rem; margin:0; }
</style>

<div class="js-cal-header">
    <h2>
        <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_TITULO'); ?>:
        <?php echo htmlspecialchars($nombreTrab, ENT_QUOTES, 'UTF-8'); ?>
        <?php if ($this->trabajador && !empty($this->trabajador->nif)) : ?>
            <small class="text-muted">(<?php echo htmlspecialchars((string) $this->trabajador->nif, ENT_QUOTES, 'UTF-8'); ?>)</small>
        <?php endif; ?>
    </h2>
    <div class="js-cal-nav">
        <a class="btn btn-sm btn-outline-secondary"
           href="<?php echo Route::_($linkBase . '&year=' . $this->prevYear . '&month=' . $this->prevMonth); ?>">
            &laquo; <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_MES_ANTERIOR'); ?>
        </a>
        <strong style="margin:0 .75rem;">
            <?php echo $meses[$this->month] . ' ' . $this->year; ?>
        </strong>
        <a class="btn btn-sm btn-outline-secondary"
           href="<?php echo Route::_($linkBase . '&year=' . $this->nextYear . '&month=' . $this->nextMonth); ?>">
            <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_MES_SIGUIENTE'); ?> &raquo;
        </a>
    </div>
</div>

<div class="js-cal-grid">
    <?php foreach ($diasSemana as $dow) : ?>
        <div class="js-cal-dow"><?php echo $dow; ?></div>
    <?php endforeach; ?>

    <?php for ($i = 1; $i < $diaInicio; $i++) : ?>
        <div class="js-cal-cell js-cal-empty"></div>
    <?php endfor; ?>

    <?php for ($d = 1; $d <= $diasMes; $d++) :
        $j     = $this->jornadasMes[$d] ?? null;
        $clase = $j ? ($estadoClase[(string) $j->estado] ?? '') : '';
        $href  = Route::_($linkBase . '&year=' . $this->year . '&month=' . $this->month . '&dia=' . $d);
    ?>
        <?php if ($j) : ?>
            <a class="js-cal-cell <?php echo $clase; ?>" href="<?php echo $href; ?>"
               title="<?php echo htmlspecialchars((string) $j->estado, ENT_QUOTES, 'UTF-8'); ?>">
                <span class="js-cal-day-num"><?php echo $d; ?></span>
                <span class="js-cal-min">
                    <?php echo $fmtMin((int) ($j->minutos_trabajados ?? 0)); ?> h
                </span>
            </a>
        <?php else : ?>
            <div class="js-cal-cell">
                <span class="js-cal-day-num"><?php echo $d; ?></span>
            </div>
        <?php endif; ?>
    <?php endfor; ?>
</div>

<div class="js-cal-legend">
    <span class="js-cal-validada"><?php echo Text::_('COM_JORNADASALUDABLE_JORNADA_ESTADO_VALIDADA'); ?></span>
    <span class="js-cal-cerrada"><?php echo Text::_('COM_JORNADASALUDABLE_JORNADA_ESTADO_CERRADA'); ?></span>
    <span class="js-cal-corregida"><?php echo Text::_('COM_JORNADASALUDABLE_JORNADA_ESTADO_CORREGIDA'); ?></span>
    <span class="js-cal-abierta"><?php echo Text::_('COM_JORNADASALUDABLE_JORNADA_ESTADO_ABIERTA'); ?></span>
    <span><?php echo Text::_('COM_JORNADASALUDABLE_JORNADA_ESTADO_SIN_JORNADA'); ?></span>
</div>

<?php if ($this->jornadaSeleccionada) :
    $j = $this->jornadaSeleccionada;
    $puedeValidar     = in_array((string) $j->estado, ['CERRADA', 'CORREGIDA', 'ABIERTA'], true);
    $permiteEditarHoras = in_array((string) $j->estado, ['CORREGIDA', 'ABIERTA'], true);
    $horaInicioActual = '';
    if (!empty($j->hora_inicio) && !str_starts_with((string) $j->hora_inicio, '0000')) {
        $tsIni = strtotime((string) $j->hora_inicio);
        $horaInicioActual = $tsIni ? date('H:i', $tsIni) : '';
    }
    $horaFinActual   = '';
    if (!empty($j->hora_fin) && !str_starts_with((string) $j->hora_fin, '0000')) {
        $tsFin = strtotime((string) $j->hora_fin);
        $horaFinActual = $tsFin ? date('H:i', $tsFin) : '';
    }
?>
    <div class="js-cal-detail">
        <h3>
            <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_DETALLE_JORNADA'); ?>
            — <?php echo htmlspecialchars((string) $j->fecha, ENT_QUOTES, 'UTF-8'); ?>
        </h3>
        <dl>
            <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_HORA_ENTRADA'); ?></dt>
            <dd><?php echo $fmtHora($j->hora_inicio ?? null); ?></dd>

            <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_HORA_SALIDA'); ?></dt>
            <dd><?php echo $fmtHora($j->hora_fin ?? null); ?></dd>

            <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_MIN_TRABAJADOS'); ?></dt>
            <dd>
                <?php echo (int) ($j->minutos_trabajados ?? 0); ?> min
                (<?php echo $fmtMin((int) ($j->minutos_trabajados ?? 0)); ?> h)
            </dd>

            <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_MIN_PAUSA'); ?></dt>
            <dd><?php echo (int) ($j->minutos_pausa ?? 0); ?> min</dd>

            <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_MIN_EXTRA'); ?></dt>
            <dd><?php echo (int) ($j->minutos_extra ?? 0); ?> min</dd>

            <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_ESTADO'); ?></dt>
            <dd>
                <span class="badge <?php echo $estadoClase[(string) $j->estado] ?? ''; ?>" style="padding:.25rem .5rem;">
                    <?php echo htmlspecialchars((string) $j->estado, ENT_QUOTES, 'UTF-8'); ?>
                </span>
            </dd>

            <?php if (!empty($j->observaciones)) : ?>
                <dt><?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_OBSERVACIONES'); ?></dt>
                <dd><?php echo nl2br(htmlspecialchars((string) $j->observaciones, ENT_QUOTES, 'UTF-8')); ?></dd>
            <?php endif; ?>
        </dl>

        <form action="<?php echo Route::_('index.php?option=com_jornadasaludable'); ?>"
              method="post" style="margin-top:1rem;">
            <input type="hidden" name="task" value="calendario.validar"/>
            <input type="hidden" name="jornada_id" value="<?php echo (int) $j->id; ?>"/>
            <input type="hidden" name="user_id" value="<?php echo (int) $this->userId; ?>"/>
            <input type="hidden" name="year" value="<?php echo (int) $this->year; ?>"/>
            <input type="hidden" name="month" value="<?php echo (int) $this->month; ?>"/>
            <?php echo HTMLHelper::_('form.token'); ?>

            <?php if ($permiteEditarHoras) : ?>
                <div class="js-cal-horas" style="display:flex; gap:1.5rem; margin-bottom:.75rem; flex-wrap:wrap;">
                    <div>
                        <label for="js-cal-hora-inicio" class="form-label" style="font-weight:bold; display:block;">
                            <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_HORA_ENTRADA'); ?>
                        </label>
                        <input type="time" id="js-cal-hora-inicio" name="hora_inicio" class="form-control"
                               value="<?php echo htmlspecialchars($horaInicioActual, ENT_QUOTES, 'UTF-8'); ?>"
                               style="max-width:160px;"/>
                    </div>
                    <div>
                        <label for="js-cal-hora-fin" class="form-label" style="font-weight:bold; display:block;">
                            <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_HORA_SALIDA'); ?>
                        </label>
                        <input type="time" id="js-cal-hora-fin" name="hora_fin" class="form-control"
                               value="<?php echo htmlspecialchars($horaFinActual, ENT_QUOTES, 'UTF-8'); ?>"
                               style="max-width:160px;"/>
                    </div>
                </div>
                <small class="text-muted" style="display:block; margin-bottom:.75rem;">
                    <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_HORAS_AYUDA'); ?>
                </small>
            <?php endif; ?>

            <button type="submit" class="btn btn-success"
                <?php echo $puedeValidar ? '' : 'disabled'; ?>>
                <span class="icon-check" aria-hidden="true"></span>
                <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_VALIDAR'); ?>
            </button>
            <a class="btn btn-secondary"
               href="<?php echo Route::_('index.php?option=com_jornadasaludable&view=trabajadores'); ?>">
                <?php echo Text::_('COM_JORNADASALUDABLE_CALENDARIO_VOLVER'); ?>
            </a>
        </form>
    </div>
<?php endif; ?>
