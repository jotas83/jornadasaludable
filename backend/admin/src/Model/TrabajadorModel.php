<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Model;

defined('_JEXEC') or die;

use Joomla\CMS\Factory;
use Joomla\CMS\Form\Form;
use Joomla\CMS\Language\Text;
use Joomla\CMS\MVC\Model\AdminModel;

final class TrabajadorModel extends AdminModel
{
    protected $text_prefix = 'COM_JORNADASALUDABLE_TRABAJADOR';

    public function getTable($name = 'Trabajador', $prefix = 'Administrator', $options = [])
    {
        return parent::getTable($name, $prefix, $options);
    }

    public function getForm($data = [], $loadData = true): Form|bool
    {
        $form = $this->loadForm(
            'com_jornadasaludable.trabajador',
            'trabajador',
            ['control' => 'jform', 'load_data' => $loadData]
        );
        return $form ?: false;
    }

    protected function loadFormData()
    {
        $data = Factory::getApplication()->getUserState('com_jornadasaludable.edit.trabajador.data', []);
        if (empty($data)) {
            $data = $this->getItem();
            if (!empty($data) && is_object($data)) {
                $data->password = '';
            }
        }
        return $data;
    }

    public function save($data): bool
    {
        $isNew = empty($data['id']);
        $plain = isset($data['password']) ? trim((string) $data['password']) : '';

        if ($isNew && $plain === '') {
            $this->setError(Text::_('COM_JORNADASALUDABLE_ERROR_PASSWORD_REQUIRED'));
            return false;
        }

        if ($plain !== '') {
            $data['password_hash'] = password_hash($plain, PASSWORD_BCRYPT);
        }
        unset($data['password']);

        if (!empty($data['email'])) {
            $data['email'] = strtolower(trim((string) $data['email']));
        }

        if (!parent::save($data)) {
            return false;
        }

        if (!empty($data['contrato']) && is_array($data['contrato'])) {
            $this->saveContrato((int) $this->getState($this->getName() . '.id'), $data['contrato']);
        }

        return true;
    }

    private function saveContrato(int $userId, array $contrato): void
    {
        if ($userId <= 0 || empty($contrato['empresa_id']) || empty($contrato['fecha_inicio'])) {
            return;
        }

        $db = Factory::getContainer()->get('DatabaseDriver');
        $vigente = (int) ($contrato['vigente'] ?? 0);

        if ($vigente === 1) {
            $upd = $db->getQuery(true)
                ->update($db->quoteName('#__js_contratos'))
                ->set($db->quoteName('vigente') . ' = 0')
                ->where($db->quoteName('user_id') . ' = ' . $userId);
            $db->setQuery($upd)->execute();
        }

        $row              = new \stdClass();
        $row->uuid        = self::uuid();
        $row->user_id     = $userId;
        $row->empresa_id  = (int) $contrato['empresa_id'];
        $row->tipo        = (string) ($contrato['tipo'] ?? 'INDEFINIDO');
        $row->jornada_tipo= (string) ($contrato['jornada_tipo'] ?? 'COMPLETA');
        $row->horas_semanales = (float) ($contrato['horas_semanales'] ?? 40);
        $row->fecha_inicio= (string) $contrato['fecha_inicio'];
        $row->fecha_fin   = !empty($contrato['fecha_fin']) ? (string) $contrato['fecha_fin'] : null;
        $row->vigente     = $vigente;

        $db->insertObject('#__js_contratos', $row);
    }

    private static function uuid(): string
    {
        $b = random_bytes(16);
        $b[6] = chr((ord($b[6]) & 0x0f) | 0x40);
        $b[8] = chr((ord($b[8]) & 0x3f) | 0x80);
        $h = bin2hex($b);
        return sprintf('%s-%s-%s-%s-%s', substr($h, 0, 8), substr($h, 8, 4), substr($h, 12, 4), substr($h, 16, 4), substr($h, 20, 12));
    }
}
