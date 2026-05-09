<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Table;

defined('_JEXEC') or die;

use Joomla\CMS\Table\Table;
use Joomla\Database\DatabaseDriver;

final class LicenciaTable extends Table
{
    public function __construct(DatabaseDriver $db)
    {
        parent::__construct('#__js_licencias', 'id', $db);
    }

    public function check(): bool
    {
        if (empty($this->empresa_id) || empty($this->tipo) || empty($this->fecha_inicio)) {
            $this->setError('Empresa, tipo y fecha de inicio son obligatorios.');
            return false;
        }
        return parent::check();
    }
}
