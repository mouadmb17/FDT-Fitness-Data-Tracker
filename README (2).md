# Health Traffic Inspector

Prototipo web local para revisar posibles datos de salud en peticiones HTTP.

## Uso
Abre index.html en un navegador. Incluye datos ficticios e importación de archivos HAR JSON con log.entries. No requiere instalación.

## Funciones
Búsqueda, filtro de transporte, detección heurística de nombres de campos, valores ocultos por defecto y detalle de petición. Analiza parámetros de consulta, formularios y cuerpos JSON.

## Límites y privacidad
No captura tráfico Android, no importa PCAP, no descifra TLS ni confirma fugas. Un HAR puede contener HTTPS descifrado: verlo no implica transmisión en claro. Las coincidencias requieren revisión humana; no detectar campos no demuestra ausencia de datos sensibles. Procesamiento local sin dependencias ni llamadas de red del frontend. Abre únicamente capturas de confianza: importar archivos grandes puede bloquear la pestaña. No subas capturas reales al repositorio. Úsalo con tráfico propio o autorizado.
