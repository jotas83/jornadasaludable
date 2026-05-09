<?php
declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Language\Text;
use Joomla\CMS\Layout\LayoutHelper;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Trabajadores\HtmlView $this */

HTMLHelper::_('behavior.multiselect');

$listOrder = $this->state->get('list.ordering', 'a.apellidos');
$listDirn  = $this->state->get('list.direction', 'ASC');
?>
<form action="<?php echo Route::_('index.php?option=com_jornadasaludable&view=trabajadores'); ?>"
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
                        <td style="width:1%"><?php echo HTMLHelper::_('grid.checkall'); ?></td>
                        <th scope="col"><?php echo HTMLHelper::_('searchtools.sort', 'COM_JORNADASALUDABLE_TRABAJADOR_FIELD_APELLIDOS', 'a.apellidos', $listDirn, $listOrder); ?></th>
                        <th scope="col"><?php echo Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELD_NIF'); ?></th>
                        <th scope="col"><?php echo Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELD_EMAIL'); ?></th>
                        <th scope="col"><?php echo Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELD_EMPRESA'); ?></th>
                        <th scope="col" style="width:8%"><?php echo Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELD_IDIOMA'); ?></th>
                        <th scope="col" style="width:8%"><?php echo Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELD_ACTIVO'); ?></th>
                        <th scope="col" style="width:5%">ID</th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php foreach ($this->items as $i => $row) : ?>
                        <tr>
                            <td><?php echo HTMLHelper::_('grid.id', $i, (int) $row->id); ?></td>
                            <td>
                                <a href="<?php echo Route::_('index.php?option=com_jornadasaludable&task=trabajador.edit&id=' . (int) $row->id); ?>">
                                    <?php echo htmlspecialchars((string) $row->apellidos . ', ' . $row->nombre, ENT_QUOTES, 'UTF-8'); ?>
                                </a>
                            </td>
                            <td><?php echo htmlspecialchars((string) $row->nif, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) ($row->email ?? ''), ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) ($row->empresa_razon_social ?? '—'), ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) $row->idioma, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td>
                                <?php if ((int) $row->activo === 1) : ?>
                                    <span class="badge bg-success"><?php echo Text::_('COM_JORNADASALUDABLE_ESTADO_ACTIVO'); ?></span>
                                <?php else : ?>
                                    <span class="badge bg-secondary"><?php echo Text::_('COM_JORNADASALUDABLE_ESTADO_INACTIVO'); ?></span>
                                <?php endif; ?>
                            </td>
                            <td><?php echo (int) $row->id; ?></td>
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
