<?php
/**
 * @package JornadaSaludable\Api
 */
declare(strict_types=1);

namespace JornadaSaludable\Api;

use LogicException;

/**
 * Router REST monolítico.
 *
 * Tabla de rutas estática: cada entrada es [METHOD, pattern, controller,
 * action, isPublic]. dispatch() hace match contra el path normalizado, y
 * para rutas no-públicas exige access token vía Auth::requireAccessToken
 * antes de instanciar el controller correspondiente.
 *
 * El controller recibe un único array de contexto con auth, params (de
 * placeholders {nombre}), body (JSON parseado), query y method. Cualquier
 * controller debe terminar la respuesta llamando a Response::ok/error/etc;
 * si no lo hace, lanzamos LogicException que cae al exception handler
 * global de public/index.php.
 */
final class Router
{
    /** @var list<array{0:string,1:string,2:?string,3:string,4:bool}> */
    private const ROUTES = [
        // [METHOD, pattern, controller (null = inline), action, isPublic]

        // --- Health ------------------------------------------------------
        ['GET',   'health',                                       null,                       'health',          true],

        // --- Auth --------------------------------------------------------
        ['POST',  'auth/login',                                'AuthController',           'login',           true],
        ['POST',  'auth/refresh',                              'AuthController',           'refresh',         true],
        ['GET',   'auth/me',                                   'AuthController',           'me',              false],
        ['POST',  'auth/logout',                               'AuthController',           'logout',          false],

        // --- Jornadas (estáticas ANTES de patrones con {uuid}) -----------
        ['GET',   'jornadas',                                  'JornadaController',        'index',           false],
        ['GET',   'jornadas/resumen',                          'JornadaController',        'resumen',         false],
        ['GET',   'jornadas/{uuid}',                           'JornadaController',        'show',            false],

        // --- Fichajes ----------------------------------------------------
        ['GET',   'fichajes',                                  'FichajeController',        'index',           false],
        ['POST',  'fichajes/sync',                             'FichajeController',        'sync',            false],
        ['POST',  'fichajes',                                  'FichajeController',        'create',          false],

        // --- Pausas ------------------------------------------------------
        ['GET',   'pausas',                                    'PausaController',          'index',           false],
        ['POST',  'pausas',                                    'PausaController',          'create',          false],

        // --- Horas extra -------------------------------------------------
        ['GET',   'horas-extra',                               'HorasExtraController',     'index',           false],
        ['POST',  'horas-extra',                               'HorasExtraController',     'create',          false],

        // --- Alertas (estáticas ANTES de patrones con {uuid}) ------------
        ['GET',   'alertas',                                   'AlertaController',         'index',           false],
        ['GET',   'alertas/tipos',                             'AlertaController',         'tipos',           false],
        ['POST',  'alertas/generar',                           'AlertaController',         'generar',         false],
        ['PATCH', 'alertas/{uuid}/leida',                      'AlertaController',         'marcarLeida',     false],

        // --- Derechos (estáticas y patrón 4-segs ANTES del catch-all) ----
        ['GET',   'derechos/categorias',                       'DerechoController',        'categorias',      false],
        ['GET',   'derechos/categorias/{codigo}/contenidos',   'DerechoController',        'contenidos',      false],
        ['GET',   'derechos/buscar',                           'DerechoController',        'buscar',          false],
        ['GET',   'derechos/{codigo}',                         'DerechoController',        'show',            false],

        // --- Documentos --------------------------------------------------
        ['GET',   'documentos',                                'DocumentoController',      'index',           false],
        ['POST',  'documentos/generar',                        'DocumentoController',      'generar',         false],
        ['GET',   'documentos/{uuid}/descargar',               'DocumentoController',      'descargar',       false],

        // --- Burnout -----------------------------------------------------
        ['GET',   'burnout',                                   'BurnoutController',        'index',           false],

        // --- Usuario (perfil propio + empresa actual) --------------------
        ['GET',   'usuarios/perfil',                           'UsuarioController',        'perfil',          false],
        ['PUT',   'usuarios/perfil',                           'UsuarioController',        'updatePerfil',    false],
        ['GET',   'usuarios/empresa',                          'UsuarioController',        'empresa',         false],
    ];

    public function dispatch(string $rawPath, string $method): never
    {
        $method = strtoupper($method);
        $path   = trim($rawPath, '/');
        $body   = $this->parseRequestBody();
        $query  = $_GET ?? [];

        foreach (self::ROUTES as [$routeMethod, $pattern, $controller, $action, $isPublic]) {
            if ($routeMethod !== $method) {
                continue;
            }
            $params = $this->matchPattern($pattern, $path);
            if ($params === null) {
                continue;
            }

            $auth = $isPublic ? [] : Auth::requireAccessToken();

            $context = [
                'auth'   => $auth,
                'params' => $params,
                'body'   => $body,
                'query'  => $query,
                'method' => $method,
            ];

            // Handler inline (sin clase de controller — p.ej. health).
            if ($controller === null) {
                $this->dispatchInline($action, $context);
            }

            $fqcn = __NAMESPACE__ . '\\Controllers\\' . $controller;
            if (!\class_exists($fqcn)) {
                Response::error(
                    'NOT_IMPLEMENTED',
                    sprintf('Controller %s pendiente de portar.', $controller),
                    501
                );
            }
            $instance = new $fqcn();
            if (!\method_exists($instance, $action)) {
                Response::error(
                    'NOT_IMPLEMENTED',
                    sprintf('%s::%s() pendiente de portar.', $controller, $action),
                    501
                );
            }

            $instance->{$action}($context);

            // Cualquier controller debe terminar con Response::*. Si llegamos
            // aquí es bug — lanzamos para que el handler global lo registre.
            throw new LogicException(sprintf(
                'Handler %s::%s no terminó la respuesta.',
                $controller,
                $action
            ));
        }

        Response::error(
            'ROUTE_NOT_FOUND',
            sprintf('No existe la ruta %s /%s', $method, $path),
            404
        );
    }

    /**
     * Compara el patrón ('jornadas/{uuid}') contra el path real
     * ('jornadas/abc-123'). Devuelve los placeholders capturados o null
     * si no encaja.
     */
    private function matchPattern(string $pattern, string $path): ?array
    {
        $patternSegs = explode('/', $pattern);
        $pathSegs    = explode('/', $path);

        if (\count($patternSegs) !== \count($pathSegs)) {
            return null;
        }

        $params = [];
        foreach ($patternSegs as $i => $seg) {
            if (preg_match('/^\{(\w+)\}$/', $seg, $m)) {
                $params[$m[1]] = $pathSegs[$i];
                continue;
            }
            if ($seg !== $pathSegs[$i]) {
                return null;
            }
        }
        return $params;
    }

    private function parseRequestBody(): array
    {
        $raw = file_get_contents('php://input') ?: '';
        if ($raw === '') {
            return [];
        }
        $decoded = json_decode($raw, true);
        if (!\is_array($decoded)) {
            Response::error('BAD_REQUEST', 'Cuerpo de la petición no es JSON válido.', 400);
        }
        return $decoded;
    }

    /**
     * Handlers que no merecen una clase Controller propia. Por ahora solo
     * `health` — un ping estático.
     */
    private function dispatchInline(string $action, array $context): never
    {
        if ($action === 'health') {
            Response::ok([
                'status'    => 'ok',
                'service'   => 'jornadasaludable-api',
                'version'   => '0.2.0',
                'timestamp' => gmdate('c'),
            ]);
        }
        Response::error('INTERNAL_ERROR', sprintf('Inline action "%s" desconocida.', $action), 500);
    }
}
