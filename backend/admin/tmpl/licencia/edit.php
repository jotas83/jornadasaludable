<?php
declare(strict_types=1);

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\HTML\HTMLHelper;
use Joomla\CMS\Router\Route;

/** @var \Joomla\Component\Jornadasaludable\Administrator\View\Licencia\HtmlView $this */

HTMLHelper::_('behavior.formvalidator');
HTMLHelper::_('behavior.keepalive');
Factory::getApplication()->getDocument()->getWebAssetManager()->useScript('keepalive')->useScript('form.validate');

$id = (int) ($this->item->id ?? 0);
?>
<form action="<?php echo Route::_('index.php?option=com_jornadasaludable&view=licencia&layout=edit&id=' . $id); ?>"
      method="post" name="adminForm" id="adminForm" class="form-validate">
    <?php echo $this->form->renderFieldset('datos'); ?>
    <input type="hidden" name="task" value=""/>
    <?php echo HTMLHelper::_('form.token'); ?>
</form>
