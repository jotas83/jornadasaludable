<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Table;

defined('_JEXEC') or die;

use Joomla\CMS\Table\Table;
use Joomla\Database\DatabaseDriver;

final class CentroTable extends Table
{
    public function __construct(DatabaseDriver $db)
    {
        parent::__construct('#__js_centros_trabajo', 'id', $db);
    }

    public function check(): bool
    {
        if (empty($this->empresa_id) || empty($this->codigo) || empty($this->nombre)) {
            $this->setError('Empresa, código y nombre son obligatorios.');
            return false;
        }
        return parent::check();
    }
}
