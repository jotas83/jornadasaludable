<?php
declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Language\Text;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Trabajador\HtmlView $this */

HTMLHelper::_('behavior.formvalidator');
HTMLHelper::_('behavior.keepalive');
Factory::getApplication()->getDocument()->getWebAssetManager()->useScript('keepalive')->useScript('form.validate');

$id = (int) ($this->item->id ?? 0);
?>
<form action="<?php echo Route::_('index.php?option=com_jornadasaludable&view=trabajador&layout=edit&id=' . $id); ?>"
      method="post" name="adminForm" id="adminForm" class="form-validate">

    <?php echo HTMLHelper::_('uitab.startTabSet', 'trabajadorTab', ['active' => 'datos-tab', 'recall' => true]); ?>

        <?php echo HTMLHelper::_('uitab.addTab', 'trabajadorTab', 'datos-tab', Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELDSET_DATOS')); ?>
            <?php echo $this->form->renderFieldset('datos'); ?>
        <?php echo HTMLHelper::_('uitab.endTab'); ?>

        <?php echo HTMLHelper::_('uitab.addTab', 'trabajadorTab', 'acceso-tab', Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELDSET_ACCESO')); ?>
            <?php echo $this->form->renderFieldset('acceso'); ?>
        <?php echo HTMLHelper::_('uitab.endTab'); ?>

        <?php echo HTMLHelper::_('uitab.addTab', 'trabajadorTab', 'contrato-tab', Text::_('COM_JORNADASALUDABLE_TRABAJADOR_FIELDSET_CONTRATOS')); ?>
            <?php if ($id > 0 && !empty($this->contratos)) : ?>
                <h4><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO'); ?></h4>
                <table class="table table-sm">
                    <thead>
                    <tr>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_EMPRESA'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_TIPO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_JORNADA_TIPO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_HORAS_SEMANALES'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_FECHA_INICIO'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_FECHA_FIN'); ?></th>
                        <th><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_FIELD_VIGENTE'); ?></th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php foreach ($this->contratos as $c) : ?>
                        <tr>
                            <td><?php echo htmlspecialchars((string) ($c->empresa_razon_social ?? '—'), ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) $c->tipo, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) $c->jornada_tipo, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo number_format((float) $c->horas_semanales, 2); ?></td>
                            <td><?php echo htmlspecialchars((string) $c->fecha_inicio, ENT_QUOTES, 'UTF-8'); ?></td>
                            <td><?php echo htmlspecialchars((string) ($c->fecha_fin ?? '—'), ENT_QUOTES, 'UTF-8'); ?></td>
                            <td>
                                <?php if ((int) $c->vigente === 1) : ?>
                                    <span class="badge bg-success">Vigente</span>
                                <?php else : ?>
                                    <span class="badge bg-secondary">—</span>
                                <?php endif; ?>
                            </td>
                            <td>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="<?php echo Route::_('index.php?option=com_jornadasaludable&task=contrato.edit&id=' . (int) $c->id); ?>">
                                    <?php echo Text::_('JACTION_EDIT'); ?>
                                </a>
                            </td>
                        </tr>
                    <?php endforeach; ?>
                    </tbody>
                </table>
            <?php endif; ?>

            <h4 class="mt-4"><?php echo Text::_('COM_JORNADASALUDABLE_CONTRATO_NUEVO'); ?></h4>
            <?php echo $this->form->renderFieldset('contrato'); ?>
        <?php echo HTMLHelper::_('uitab.endTab'); ?>

    <?php echo HTMLHelper::_('uitab.endTabSet'); ?>

    <input type="hidden" name="task" value=""/>
    <?php echo HTMLHelper::_('form.token'); ?>
</form>
