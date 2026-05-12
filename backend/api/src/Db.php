<?php
declare(strict_types=1);

namespace JornadaSaludable\Api;

use PDO;
use PDOException;
use RuntimeException;

// Singleton PDO con conexión lazy.
final class Db
{
    private static ?PDO $instance = null;

    public static function pdo(): PDO
    {
        if (self::$instance !== null) {
            return self::$instance;
        }

        $cfg = $GLOBALS['JS_CONFIG']['db']
            ?? throw new RuntimeException('Config db no cargada (revisa config.php).');

        $dsn = sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=%s',
            $cfg['host'],
            (int) ($cfg['port'] ?? 3306),
            $cfg['name'],
            $cfg['charset'] ?? 'utf8mb4'
        );

        try {
            self::$instance = new PDO($dsn, (string) $cfg['user'], (string) $cfg['pass'], [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
                PDO::ATTR_STRINGIFY_FETCHES  => false,
            ]);
        } catch (PDOException $e) {
            $debug = (bool) ($GLOBALS['JS_CONFIG']['debug'] ?? false);
            throw new RuntimeException(
                $debug
                    ? 'No se pudo conectar a MySQL: ' . $e->getMessage()
                    : 'No se pudo conectar a la base de datos.',
                0,
                $e
            );
        }

        return self::$instance;
    }

    // Nombre de tabla con prefijo y backticks (equivalente a `#__tabla` de Joomla).
    public static function table(string $name): string
    {
        $prefix = (string) ($GLOBALS['JS_CONFIG']['db']['prefix'] ?? '');
        return '`' . $prefix . $name . '`';
    }

    // Solo para tests.
    public static function reset(): void
    {
        self::$instance = null;
    }
}
