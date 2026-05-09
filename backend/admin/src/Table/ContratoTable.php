<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Table;

defined('_JEXEC') or die;

use Joomla\CMS\Table\Table;
use Joomla\Database\DatabaseDriver;

final class ContratoTable extends Table
{
    public function __construct(DatabaseDriver $db)
    {
        parent::__construct('#__js_contratos', 'id', $db);
    }

    public function check(): bool
    {
        if (empty($this->user_id) || empty($this->empresa_id) || empty($this->fecha_inicio)) {
            $this->setError('Trabajador, empresa y fecha de inicio son obligatorios.');
            return false;
        }
        if (empty($this->uuid)) {
            $b = random_bytes(16);
            $b[6] = chr((ord($b[6]) & 0x0f) | 0x40);
            $b[8] = chr((ord($b[8]) & 0x3f) | 0x80);
            $h = bin2hex($b);
            $this->uuid = sprintf('%s-%s-%s-%s-%s', substr($h, 0, 8), substr($h, 8, 4), substr($h, 12, 4), substr($h, 16, 4), substr($h, 20, 12));
        }
        return parent::check();
    }
}
