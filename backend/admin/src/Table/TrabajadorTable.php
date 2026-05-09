<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Table;

defined('_JEXEC') or die;

use Joomla\CMS\Table\Table;
use Joomla\Database\DatabaseDriver;

final class TrabajadorTable extends Table
{
    public function __construct(DatabaseDriver $db)
    {
        parent::__construct('#__js_users', 'id', $db);
    }

    public function check(): bool
    {
        $this->nif       = trim((string) ($this->nif ?? ''));
        $this->nombre    = trim((string) ($this->nombre ?? ''));
        $this->apellidos = trim((string) ($this->apellidos ?? ''));

        if ($this->nif === '' || $this->nombre === '' || $this->apellidos === '') {
            $this->setError('NIF, nombre y apellidos son obligatorios.');
            return false;
        }

        if (empty($this->uuid)) {
            $this->uuid = self::generateUuid();
        }

        if (empty($this->idioma)) {
            $this->idioma = 'es-ES';
        }

        return parent::check();
    }

    private static function generateUuid(): string
    {
        $b = random_bytes(16);
        $b[6] = chr((ord($b[6]) & 0x0f) | 0x40);
        $b[8] = chr((ord($b[8]) & 0x3f) | 0x80);
        $h = bin2hex($b);
        return sprintf('%s-%s-%s-%s-%s', substr($h, 0, 8), substr($h, 8, 4), substr($h, 12, 4), substr($h, 16, 4), substr($h, 20, 12));
    }
}
