# JornadaSaludable

Aplicación móvil para el control de jornada y la protección del bienestar laboral en sectores vulnerables

[![Licencia](https://img.shields.io/badge/licencia-Apache%202.0-blue.svg)](LICENSE)
[![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow.svg)]()

---

## Trabajo de Fin de Grado

- **Universidad:** UNIR - Universidad Internacional de La Rioja
- **Titulación:** Grado en Ingeniería Informática
- **Autor:** Jonathan Soriano
- **Curso académico:** 2025-2026
- **Línea de investigación:** Ingeniería del Software

---

## Descripción del proyecto

JornadaSaludable es un sistema diseñado para empoderar a los trabajadores en el control de su jornada laboral y la prevención de riesgos laborales, cumpliendo con el Real Decreto-ley 8/2019 sobre registro obligatorio de jornada.

### Objetivos principales

1. **Control horario transparente:** Fichaje con geolocalización y acceso directo del trabajador a sus datos (cumplimiento RDL 8/2019)
2. **Prevención de riesgos laborales:** Sistema de alertas automáticas que detecta situaciones de riesgo (exceso de horas según ET, ausencia de descansos legales, indicadores de fatiga laboral)
3. **Educación sobre derechos laborales:** Módulo informativo con artículos del Estatuto de los Trabajadores explicados en lenguaje accesible

### Alineación ODS

- **ODS 3:** Salud y bienestar
- **ODS 8:** Trabajo decente y crecimiento económico
- **ODS 10:** Reducción de las desigualdades

---

## Stack tecnológico

**Frontend:**

- Android nativo (Kotlin)
- Arquitectura MVVM
- Material Design 3

**Backend:**

- API REST en PHP 8.3 (servicio autónomo)
- MySQL con 16 tablas
- Joomla 5.x (panel de administración de RRHH)

**Características:**

- 100% Open Source
- Licencia Apache 2.0
- Funcionamiento offline con sincronización diferida (Room + WorkManager)

---

## Pruebas de rendimiento

Los registros de las pruebas de rendimiento que respaldan la sección de resultados de la memoria se encuentran en el directorio [`docs/pruebas-rendimiento/`](docs/pruebas-rendimiento/).

| Fichero | Descripción |
|---|---|
| `latencias.txt` | Tiempos de respuesta del endpoint `POST /fichajes` (50 peticiones, curl) |
| `gps_exterior.txt` | Lecturas de precisión GPS en exterior (Samsung Galaxy A25) |
| `gps_interior.txt` | Lecturas de precisión GPS en interior (Samsung Galaxy A25) |

**Resultados principales:**

- Latencia mediana de la API: 47,3 ms (p95: 94,3 ms)
- Precisión GPS en exterior: 11,5 m de mediana
- Consumo de memoria estable: 83-85 MB (emulador Pixel 6, API 34)
- Payload por fichaje: 221 bytes

Todas las mediciones se realizaron en un entorno de desarrollo local y no son extrapolables directamente a producción.

---

## Licencia

Apache License 2.0

Este proyecto es software libre. Puedes usar, modificar y distribuir este código bajo los términos de la licencia Apache 2.0.

Ver [LICENSE](LICENSE) para más detalles.

---

## Estado actual

- **Versión:** 0.1.0-dev
- **Última actualización:** Julio 2026

Este repositorio forma parte del Trabajo de Fin de Grado de Ingeniería Informática en UNIR.

---

*Proyecto en desarrollo - TFG UNIR 2026*
