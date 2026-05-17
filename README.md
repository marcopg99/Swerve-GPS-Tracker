# GPS Tracker

Aplicación Android para registrar rutas mediante GPS, visualizarlas sobre un mapa de OpenStreetMap y exportarlas como archivos GPX.

## Funcionalidades

- Grabación de rutas en segundo plano mediante un *foreground service* con `FusedLocationProvider`.
- Almacenamiento local en Room (`gpstracker.db`) con relación `Track` (1) ↔ `TrackPoint` (N).
- Listado de rutas con resumen (número de puntos, fechas) y detalle con mapa.
- Renderizado del mapa con [osmdroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap).
- Exportación de cada ruta como GPX 1.1 y compartido vía `FileProvider`.