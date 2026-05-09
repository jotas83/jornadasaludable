<?php
declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Language\Text;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Empresa\HtmlView $this */

HTMLHelper::_('behavior.formvalidator');
HTMLHelper::_('behavior.keepalive');
Factory::getApplication()->getDocument()->getWebAssetManager()->useScript('keepalive')->useScript('form.validate');

$id = (int) ($this->item->id ?? 0);
?>
<form action="<?php echo Route::_('index.php?option=com_jornadasaludable&view=empresa&layout=edit&id=' . $id); ?>"
      method="post" name="adminForm" id="adminForm" class="form-validate">

    <?php echo HTMLHelper::_('uitab.startTabSet', 'empresaTab', ['active' => 'datos-tab', 'recall' => true]); ?>

        <?php echo HTMLHelper::_('uitab.addTab', 'empresaTab', 'datos-tab', Text::_('COM_JORNADASALUDABLE_EMPRESA_FIELDSET_DATOS')); ?>
            <?php echo $this->form->renderFieldset('datos'); ?>
        <?php echo HTMLHelper::_('uitab.endTab'); ?>

        <?php echo HTMLHelper::_('uitab.addTab', 'empresaTab', 'localizacion-tab', Text::_('COM_JORNADASALUDABLE_EMPRESA_FIELDSET_LOCALIZACION')); ?>
            <?php echo $this->form->renderFieldset('localizacion'); ?>
        <?php echo HTMLHelper::_('uitab.endTab'); ?>

        <?php echo HTMLHelper::_('uitab.addTab', 'empresaTab', 'contacto-tab', Text::_('COM_JORNADASALUDABLE_EMPRESA_FIELDSET_CONTACTO')); ?>
            <?php echo $this->form->renderFieldset('contacto'); ?>
        <?php echo HTMLHelper::_('uitab.endTab'); ?>

        <?php if ($id > 0) : ?>
            <?php echo HTMLHelper::_('uitab.addTab', 'empresaTab', 'licencias-tab', Text::_('COM_JORNADASALUDABLE_LICENCIA')); ?>
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h3 class="mb-0"><?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA'); ?></h3>
                    <a class="btn btn-primary"
                       href="<?php echo Route::_('index.php?option=com_jornadasaludable&task=licencia.add&empresa_id=' . $id); ?>">
                        <span class="icon-plus" aria-hidden="true"></span>
                        <?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA_NUEVA'); ?>
                    </a>
                </div>
                <?php if (empty($this->licencias)) : ?>
                    <div class="alert alert-info"><?php echo Text::_('JGLOBAL_NO_MATCHING_RESULTS'); ?></div>
                <?php else : ?>
                    <table class="table table-sm">
                        <thead>
                        <tr>
                            <th><?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA_FIELD_TIPO'); ?></th>
                            <th><?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA_FIELD_MAX_USUARIOS'); ?></th>
                            <th><?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA_FIELD_FECHA_INICIO'); ?></th>
                            <th><?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA_FIELD_FECHA_FIN'); ?></th>
                            <th><?php echo Text::_('COM_JORNADASALUDABLE_LICENCIA_FIELD_ACTIVA'); ?></th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <?php foreach ($this->licencias as $lic) : ?>
                            <tr>
                                <td><?php echo htmlspecialchars((string) $lic->tipo, ENT_QUOTES, 'UTF-8'); ?></td>
                                <td><?php echo (int) ($lic->max_usuarios ?? 0); ?></td>
                                <td><?php echo htmlspecialchars((string) $lic->fecha_inicio, ENT_QUOTES, 'UTF-8'); ?></td>
                                <td><?php echo htmlspecialchars((string) ($lic->fecha_fin ?? '—'), ENT_QUOTES, 'UTF-8'); ?></td>
                                <td>
                                    <?php if ((int) $lic->activa === 1) : ?>
                                        <span class="badge bg-success">Activa</span>
                                    <?php else : ?>
                                        <span class="badge bg-secondary">Inactiva</span>
                                    <?php endif; ?>
                                </td>
                                <td>
                                    <a class="btn btn-sm btn-outline-primary"
                                       href="<?php echo Route::_('index.php?option=com_jornadasaludable&task=licencia.edit&id=' . (int) $lic->id); ?>">
                                        <?php echo Text::_('JACTION_EDIT'); ?>
                                    </a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                        </tbody>
                    </table>
                <?php endif; ?>
            <?php echo HTMLHelper::_('uitab.endTab'); ?>
        <?php endif; ?>

    <?php echo HTMLHelper::_('uitab.endTabSet'); ?>

    <input type="hidden" name="task" value=""/>
    <?php echo HTMLHelper::_('form.token'); ?>
</form>
