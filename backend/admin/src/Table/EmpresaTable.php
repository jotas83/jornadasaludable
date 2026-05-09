<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Table;

defined('_JEXEC') or die;

use Joomla\CMS\Table\Table;
use Joomla\Database\DatabaseDriver;

final class EmpresaTable extends Table
{
    public function __construct(DatabaseDriver $db)
    {
        parent::__construct('#__js_empresas', 'id', $db);
    }

    public function check(): bool
    {
        $this->cif          = trim((string) ($this->cif ?? ''));
        $this->razon_social = trim((string) ($this->razon_social ?? ''));

        if ($this->cif === '' || $this->razon_social === '' || empty($this->sector_id)) {
            $this->setError('CIF, razón social y sector son obligatorios.');
            return false;
        }
        return parent::check();
    }
}
