<?php
declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Language\Text;
use Joomla\CMS\Layout\LayoutHelper;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Alertas\HtmlView $this */

HTMLHelper::_('behavior.multiselect');

$listOrder = $this->state->get('list.ordering', 'a.fecha_evento');
$listDirn  = $this->state->get('list.direction', 'DESC');

$badgeMap = [
    'INFORMATIVA' => 'bg-info',
    'AVISO'       => 'bg-warning',
    'GRAVE'       => 'bg-danger',
    'CRITICA'     => 'bg-dark',
];
?>
<form action="<?php echo Route::_('index.php?option=com_jornadasaludable&view=alertas'); ?>"
      method="post" name="adminForm" id="adminForm">
    <div class="row">
        <div class="col-md-12">
            <?php echo LayoutHelper::render('joomla.searchtools.default', ['view' => $this]); ?>

            <?php if (empty($this->items)) : ?>
                <div class="alert alert-info"><?php echo Text::_('JGLOBAL_NO_MATCHING_RESULTS'); ?></div>
            <?php else : ?>
                <table class="table table-striped">
                    <thead>
                    <tr>
                        <th><?php echo HTMLHelper::_('searchtools.sort', 'COM_JORNADASALUDABLE_ALERTA_FIELD_FECHA', 'a.fecha_evento', $listDirn, $listOrder); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FIELD_TRABAJADOR'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FIELD_EMPRESA'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FIELD_TIPO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FIELD_SEVERIDAD'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FIELD_MENSAJE'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FIELD_LEIDA'); ?></th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php foreach ($this->items as $row) :
                        $sev = (string) ($row->severidad ?? '');
                        $cls = $badgeMap[$sev] ?? 'bg-secondary';
                    ?>
                        <tr>
                            <td><?php echo htmlspecialchars((string) $row->fecha_evento, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td>
                                <?php
                                $nombre = trim(($row->user_apellidos ?? '') . ', ' . ($row->user_nombre ?? ''));
                                echo htmlspecialchars($nombre, ENT_QUOTES, 'UTF-8');
                                ?>
                                <?php if (!empty($row->user_nif)) : ?>
                                    <div class="small text-muted"><?php echo htmlspecialchars((string) $row->user_nif, ENT_QUOTES, 'UTF-8'); ?></div>
                                <?php endif; ?>
                            </td>
                            <td><?php echo htmlspecialchars((string) ($row->empresa_razon_social ?? '—'), ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) ($row->tipo_nombre ?? ''), ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><span class="badge <?php echo $cls; ?>"><?php echo htmlspecialchars($sev, ENT_QUOTES, 'UTF-8'); ?></span></td>
                            <td><?php echo htmlspecialchars((string) $row->mensaje, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td>
                                <?php if ((int) $row->leida === 1) : ?>
                                    <span class="badge bg-success"><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FILTER_LEIDA_SI'); ?></span>
                                <?php else : ?>
                                    <span class="badge bg-warning text-dark"><?php echo Text::_('COM_JORNADASALUDABLE_ALERTA_FILTER_LEIDA_NO'); ?></span>
                                <?php endif; ?>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
                <?php echo $this->pagination->getListFooter(); ?>
            <?php endif; ?>

            <input type="hidden" name="task" value=""/>
            <input type="hidden" name="boxchecked" value="0"/>
            <?php echo HTMLHelper::_('form.token'); ?>
        </div>
    </div>
</form>
