<?php
declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Language\Text;
use Joomla\CMS\Layout\LayoutHelper;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Documentos\HtmlView $this */

$listOrder = $this->state->get('list.ordering', 'a.created_at');
$listDirn  = $this->state->get('list.direction', 'DESC');

$humanSize = static function (?int $bytes): string {
    if (!$bytes) return '—';
    $units = ['B', 'KB', 'MB', 'GB'];
    $i = 0;
    $b = (float) $bytes;
    while ($b >= 1024 && $i < 3) { $b /= 1024; $i++; }
    return number_format($b, $i ? 1 : 0) . ' ' . $units[$i];
};
?>

<div class="card mb-3">
    <div class="card-body">
        <h5 class="card-title">
            <span class="icon-download" aria-hidden="true"></span>
            <?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_EXPORT_TITLE'); ?>
        </h5>
        <form method="post"
              action="<?php echo Route::_('index.php?option=com_jornadasaludable'); ?>"
              class="row g-2 align-items-end">
            <div class="col-md-4">
                <label for="exp_user_id" class="form-label">
                    <?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_EXPORT_TRABAJADOR'); ?>
                </label>
                <select id="exp_user_id" name="user_id" class="form-select" required>
                    <option value="">- Selecciona trabajador -</option>
                    <?php foreach ($this->trabajadores as $t) :
                        $label = trim(($t->apellidos ?? '') . ', ' . ($t->nombre ?? '')) . ' (' . ($t->nif ?? '') . ')';
                    ?>
                        <option value="<?php echo (int) $t->id; ?>"><?php echo htmlspecialchars($label, ENT_QUOTES, 'UTF-8'); ?></option>
                    <?php endforeach; ?>
                </select>
            </div>
            <div class="col-md-3">
                <label for="exp_fecha_inicio" class="form-label">
                    <?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_EXPORT_DESDE'); ?>
                </label>
                <input id="exp_fecha_inicio" type="date" name="fecha_inicio" class="form-control" required>
            </div>
            <div class="col-md-3">
                <label for="exp_fecha_fin" class="form-label">
                    <?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_EXPORT_HASTA'); ?>
                </label>
                <input id="exp_fecha_fin" type="date" name="fecha_fin" class="form-control" required>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-success w-100">
                    <span class="icon-download" aria-hidden="true"></span>
                    <?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_EXPORT_BTN'); ?>
                </button>
            </div>
            <input type="hidden" name="task" value="documentos.exportarCsv">
            <?php echo HTMLHelper::_('form.token'); ?>
        </form>
    </div>
</div>

<form action="<?php echo Route::_('index.php?option=com_jornadasaludable&view=documentos'); ?>"
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
                        <th><?php echo HTMLHelper::_('searchtools.sort', 'COM_JORNADASALUDABLE_DOCUMENTO_FIELD_CREATED', 'a.created_at', $listDirn, $listOrder); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_FIELD_TRABAJADOR'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_FIELD_TIPO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_FIELD_PERIODO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_FIELD_FICHERO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_FIELD_TAMANO'); ?></th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php foreach ($this->items as $row) : ?>
                        <tr>
                            <td><?php echo htmlspecialchars((string) $row->created_at, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td>
                                <?php
                                $nombre = trim(($row->user_apellidos ?? '') . ', ' . ($row->user_nombre ?? ''));
                                echo htmlspecialchars($nombre, ENT_QUOTES, 'UTF-8');
                                ?>
                                <?php if (!empty($row->user_nif)) : ?>
                                    <div class="small text-muted"><?php echo htmlspecialchars((string) $row->user_nif, ENT_QUOTES, 'UTF-8'); ?></div>
                                <?php endif; ?>
                            </td>
                            <td><?php echo htmlspecialchars((string) $row->tipo, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td>
                                <?php if (!empty($row->periodo_desde)) : ?>
                                    <?php echo htmlspecialchars((string) $row->periodo_desde, ENT_QUOTES, 'UTF-8'); ?>
                                    <?php if (!empty($row->periodo_hasta)) : ?>
                                        → <?php echo htmlspecialchars((string) $row->periodo_hasta, ENT_QUOTES, 'UTF-8'); ?>
                                    <?php endif; ?>
                                <?php else : ?>—<?php endif; ?>
                            </td>
                            <td><?php echo htmlspecialchars((string) $row->nombre_fichero, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo $humanSize((int) ($row->tamano_bytes ?? 0)); ?></td>
                            <td>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="<?php echo Route::_('index.php?option=com_jornadasaludable&task=documentos.descargar&id=' . (int) $row->id); ?>">
                                    <span class="icon-download" aria-hidden="true"></span>
                                    <?php echo Text::_('COM_JORNADASALUDABLE_DOCUMENTO_FIELD_DESCARGAR'); ?>
                                </a>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
                <?php echo $this->pagination->getListFooter(); ?>
            <?php endif; ?>

            <input type="hidden" name="task" value=""/>
            <?php echo HTMLHelper::_('form.token'); ?>
        </div>
    </div>
</form>
