<?php
declare(strict_types=1);

namespace Joomla\Component\Jornadasaludable\Administrator\Extension;

defined('_JEXEC') or die;

use Joomla\CMS\Component\Router\RouterServiceInterface;
use Joomla\CMS\Component\Router\RouterServiceTrait;
use Joomla\CMS\Extension\BootableExtensionInterface;
use Joomla\CMS\Extension\MVCComponent;
use Joomla\CMS\HTML\HTMLRegistryAwareTrait;
use Psr\Container\ContainerInterface;

final class JornadasaludableComponent extends MVCComponent implements BootableExtensionInterface, RouterServiceInterface
{
    use HTMLRegistryAwareTrait;
    use RouterServiceTrait;

    public function boot(ContainerInterface $container): void
    {
    }
}
