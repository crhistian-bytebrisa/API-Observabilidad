# Monitoreo con Prometheus y Grafana

Este documento explica cómo funcionan las métricas de la API de Ventas y cómo
monitorearlas con **Prometheus** (almacenamiento y consulta) y **Grafana**
(visualización en dashboards).

## Arquitectura

```
┌─────────────────┐   scrape (15s)   ┌─────────────────┐   query    ┌──────────────┐
│  Ventas API     │ ───────────────▶ │   Prometheus    │ ◀───────── │   Grafana    │
│  (Spring Boot)  │  GET /actuator/  │   :9090         │            │   :3000      │
│  Micrometer     │  prometheus      │                 │            │              │
└─────────────────┘                  └─────────────────┘            └──────────────┘
```

1. La API genera métricas en memoria con **Micrometer** (el estándar de
   observabilidad de Spring Boot).
2. Micrometer las expone en formato Prometheus en el endpoint
   `GET /actuator/prometheus`.
3. **Prometheus** "hace scrape": lee ese endpoint cada 15 segundos y guarda las
   series de tiempo en su propia base de datos.
4. **Grafana** consulta a Prometheus y pinta los datos en un dashboard
   precargado, con consultas **PromQL**.

## Cómo levantarlo

```powershell
docker compose up -d --build
```

Quedan estos servicios:

| Servicio    | URL                                  | Usuario/Contraseña |
|-------------|--------------------------------------|--------------------|
| API         | http://localhost:8080                | -                  |
| Métricas    | http://localhost:8080/actuator/prometheus | -              |
| Prometheus  | http://localhost:9090                | -                  |
| Grafana     | http://localhost:3000                | admin / admin      |

En Grafana ya está configurado el datasource **Prometheus** y el dashboard
**Ventas API** (provisionado automáticamente desde `grafana/provisioning/`).
No hay que configurar nada a mano.

## Tipos de métricas

Micrometer modela las métricas con cuatro tipos. En Prometheus se nombran
en minúsculas con guiones bajos y sufijos `_total`, `_seconds`, `_bucket`:

- **Counter** — un contador que solo aumenta. Útil para "cuántas veces".
  En Prometheus: nombre `..._total`. Para ver velocidad por segundo se usa
  `rate(nombre_total[1m])`.
- **Gauge** — un valor que sube y baja. Útil para "cuánto hay ahora"
  (memoria usada, conexiones activas…).
- **Timer** — mide duración y ocurrencias. Expone `_seconds_count`
  (nº de eventos), `_seconds_sum` (tiempo total) y `_seconds_max`.
  Con histogramas habilitados expone además `_seconds_bucket`, que permite
  calcular percentiles (p95, p99) con `histogram_quantile`.
- **Histogram / Distribution Summary** — igual que el timer pero para valores
  sin tiempo (tamaños de petición, etc.).

## Métricas disponibles

La API publica más de 100 métricas. Las importantes:

### 1. Métricas de la JVM (Gauge / Timer)

Explica el estado del proceso Java: memoria, CPU, hilos y garbage collection.

| Métrica                         | Tipo   | Significado                                   |
|---------------------------------|--------|-----------------------------------------------|
| `jvm_memory_used_bytes`         | Gauge  | Memoria usada por área (`heap` / `nonheap`).  |
| `jvm_memory_max_bytes`          | Gauge  | Memoria máxima disponible (techo del heap).   |
| `jvm_threads_live_threads`      | Gauge  | Hilos vivos en la JVM.                        |
| `process_cpu_usage`             | Gauge  | CPU usada por el proceso (0 a 1).             |
| `system_cpu_usage`              | Gauge  | CPU usada por todo el sistema.                |
| `jvm_gc_pause_seconds`          | Timer  | Tiempo y nº de pausas del garbage collector.  |
| `process_uptime_seconds`        | Gauge  | Segundos desde que arrancó la API.            |

Si el heap se acerca al máximo durante mucho rato, la JVM hace GC más seguido
y se degrada el rendimiento; es señal de que hay que subir la memoria o
revisar fugas.

### 2. Métricas HTTP (Timer)

Micrometer mide automáticamente **todas** las peticiones HTTP:

| Métrica                              | Tipo    | Significado                            |
|--------------------------------------|---------|----------------------------------------|
| `http_server_requests_seconds_count` | Timer   | Nº de peticiones, etiquetadas por `method`, `status`, `uri`, `outcome`. |
| `http_server_requests_seconds_sum`   | Timer   | Tiempo total invertido en responder.   |
| `http_server_requests_seconds_bucket`| Histograma | Buckets para calcular percentiles. |
| `http_server_requests_active_seconds`| Timer   | Peticiones en curso ahora mismo.       |

Ejemplos de consultas PromQL:

```promql
# Peticiones por segundo (contador -> rate)
sum(rate(http_server_requests_seconds_count[1m]))

# Tasa de errores 5xx por segundo
sum(rate(http_server_requests_seconds_count{status=~"5[0-9][0-9]"}[1m]))

# Latencia p95 de las peticiones (necesita los _bucket)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

### 3. Métricas de conexiones (Gauge)

| Métrica                      | Tipo  | Significado                                   |
|------------------------------|-------|-----------------------------------------------|
| `hikaricp_connections_active`| Gauge | Conexiones a PostgreSQL en uso ahora.         |
| `hikaricp_connections_idle`  | Gauge | Conexiones libres en el pool.                 |
| `hikaricp_connections`       | Gauge | Conexiones totales del pool (active + idle).  |
| `hikaricp_connections_pending`| Gauge| Peticiones esperando una conexión libre.      |

Si `pending` sube o `active` se pega al máximo, la base de datos está
saturándose o hay consultas lentas.

### 4. Métricas de negocio (Counter personalizadas)

Se definieron a medida en `VentasMetrics` (ver
`src/main/java/com/example/ventas/metrics/VentasMetrics.java`) y se incrementan
en los servicios `ProductoService`, `ClienteService` y `StockService`:

| Métrica                     | Tipo     | Se incrementa cuando…               |
|-----------------------------|----------|-------------------------------------|
| `ventas_productos_creados_total`   | Counter | se crea un producto.          |
| `ventas_productos_eliminados_total`| Counter | se elimina un producto.       |
| `ventas_clientes_creados_total`    | Counter | se crea un cliente.           |
| `ventas_clientes_eliminados_total` | Counter | se elimina un cliente.        |
| `ventas_stock_operaciones_total`   | Counter | se crea, actualiza o elimina stock. |

Ejemplos:

```promql
# Productos creados por minuto
rate(ventas_productos_creados_total[1m])

# Total acumulado de operaciones de stock
ventas_stock_operaciones_total
```

Los contadores son por instancia y **se resetean al reiniciar la API**
(representan actividad desde el arranque). El acumulado histórico real estaría
en la propia base de datos.

### 5. Rendimiento de RAM y CPU (Gauge)

Métricas del sistema operativo vistas dentro del contenedor. Se obtienen con el
`OperatingSystemMXBean` de la JVM en `MetricasSistema.java`:

| Métrica                     | Tipo  | Significado                            |
|-----------------------------|-------|----------------------------------------|
| `ventas_ram_usada_bytes`    | Gauge | RAM del sistema usada ahora.           |
| `ventas_ram_libre_bytes`    | Gauge | RAM del sistema libre ahora.           |
| `ventas_ram_total_bytes`    | Gauge | RAM total del sistema.                 |
| `process_cpu_usage`         | Gauge | CPU consumida por la API (0 a 1).       |
| `system_cpu_usage`          | Gauge | CPU consumida por todo el sistema.      |

Ejemplos:

```promql
# % de RAM usada
ventas_ram_usada_bytes / ventas_ram_total_bytes * 100

# CPU del proceso en porcentaje
process_cpu_usage * 100
```

### 6. Peticiones CRUD de la API

Cada operación CRUD (GET/POST/PUT/DELETE) se mide **automáticamente** por el
timer HTTP de Micrometer, etiquetada con el método y el endpoint:

```promql
# Peticiones por segundo, separadas por método CRUD
sum by (method) (rate(http_server_requests_seconds_count[1m]))

# Creaciones (POST) acumuladas
sum(http_server_requests_seconds_count{method="POST"})
```

Mapeo método → operación CRUD:

| Método  | Operación     | Endpoints de ejemplo          |
|---------|---------------|-------------------------------|
| GET     | Read (listar) | `/api/productos`, `/api/stock` |
| POST    | Create        | `/api/productos`, `/api/clientes` |
| PUT     | Update        | `/api/productos/{id}`         |
| DELETE  | Delete        | `/api/productos/{id}`         |

### 7. Respuestas HTTP

El timer HTTP también etiqueta cada petición con el `status` de la respuesta,
por lo que se pueden clasificar los códigos:

```promql
# Respuestas por segundo, por código HTTP
sum by (status) (rate(http_server_requests_seconds_count[1m]))

# Total de respuestas 5xx
sum(http_server_requests_seconds_count{status=~"5[0-9][0-9]"})

# Porcentaje de errores 4xx+5xx sobre el total
sum(http_server_requests_seconds_count{status=~"[45][0-9][0-9]"})
  / sum(http_server_requests_seconds_count) * 100
```

### 8. Elementos en la base de datos (Gauge)

Un scheduler en `MetricasSistema.java` (`@Scheduled`, cada 30 segundos) ejecuta
`count()` sobre cada tabla y publica el número de filas. Persiste el dato en
Prometheus, así ves la **evolución** de la BD en el tiempo:

| Métrica                      | Tipo  | Significado                         |
|------------------------------|-------|-------------------------------------|
| `ventas_db_conteo_productos` | Gauge | Filas en la tabla `productos`.      |
| `ventas_db_conteo_clientes`  | Gauge | Filas en la tabla `clientes`.       |
| `ventas_db_conteo_stock`     | Gauge | Filas en la tabla `stock`.          |
| `ventas_db_conteo_global`    | Gauge | Suma de las tres tablas.            |

## El dashboard "Ventas API"

Dashboard pensado para ser **simple** (estilo principiante): solo números
y totales, **sin** secciones avanzadas de JVM ni métricas de negocio. Las
peticiones se muestran como **recuentos enteros** (sin decimales ni `rate()/s`).

Paneles incluidos:

| Sección                          | Panel                 | Query principal                     |
|----------------------------------|-----------------------|-------------------------------------|
| Estado general                   | Total de peticiones   | `sum(http_server_requests_seconds_count)` |
|                                  | Respuestas 4xx        | `sum(...{status=~"4[0-9][0-9]"})`  |
|                                  | Respuestas 5xx        | `sum(...{status=~"5[0-9][0-9]"})`  |
| Recursos del sistema (RAM/CPU)   | RAM usada / libre / total | `ventas_ram_*_bytes`            |
|                                  | CPU del proceso       | `process_cpu_usage`                |
|                                  | CPU del sistema       | `system_cpu_usage`                 |
| Peticiones CRUD de la API        | GET / POST / PUT / DELETE | `sum(...{method="..."})`      |
| Respuestas HTTP                  | 2xx / 3xx / 4xx / 5xx | `sum(...{status=~"[2345][0-9][0-9]"})` |
| Elementos en la base de datos    | Productos / Clientes / Stock / Total | `ventas_db_conteo_*` |
|                                  | Evolución en el tiempo| `ventas_db_conteo_*` (timeseries)  |

Todos los paneles de peticiones y respuestas usan contadores acumulados
(`..._seconds_count`) con `decimals: 0`, así que nunca se ven fracciones.

## Consultar y configurar Prometheus

Interfaz de Prometheus (`http://localhost:9090/graph`): probar PromQL y ver
los targets (objetivos) que scrapea en `Status → Targets`. El job de la API se
llama `ventas-api` y está definido en `prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s          # cada cuánto scrapea

scrape_configs:
  - job_name: 'ventas-api'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['api:8080']
        labels:
          application: 'ventas-api'
```

Nota: `api` es el nombre del contenedor dentro de la red Docker de
`docker-compose`, por eso no es `localhost`.

## Configuración relevante en la API

En `src/main/resources/application.properties`:

```properties
# Expone los endpoints de Actuator (entre ellos /actuator/prometheus)
management.endpoints.web.exposure.include=health,info,prometheus

# Etiqueta común que añade a todas las series (útil para filtrar)
management.metrics.tags.application=ventas-api

# Habilita los buckets del histograma HTTP (necesario para p95/p99)
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```

Dependencia añadida en `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Métricas personalizadas en código

- `VentasMetrics` — contadores de negocio que se incrementan en los servicios
  (`ProductoService`, `ClienteService`, `StockService`).
- `MetricasSistema` — gauges de RAM del sistema y conteos de filas de la BD.
  Un `@Scheduled(fixedDelay = 30000)` recalcula los conteos cada 30 segundos.
  Para que los `@Scheduled` funcionen, `VentasApplication` está anotada con
  `@EnableScheduling`.

El dashboard de Grafana se controla desde archivos versionables (provisioning):
`grafana/provisioning/datasources`, `grafana/provisioning/dashboards` y
`grafana/dashboards/ventas-dashboard.json`, así que cualquier cambio se aplica
solo editando esos archivos (Grafana sincroniza cada 30 s).

## Cómo generar tráfico de prueba

Con la API levantada, unas llamadas para ver cómo suben los paneles:

```powershell
# Crea 10 productos
for ($i = 0; $i -lt 10; $i++) {
  Invoke-RestMethod -Uri http://localhost:8080/api/productos -Method Post `
    -ContentType "application/json" `
    -Body '{"nombre":"Producto de prueba","descripcion":"x","precio":10}' -OutVariable null
}

# Crea un cliente
Invoke-RestMethod -Uri http://localhost:8080/api/clientes -Method Post `
  -ContentType "application/json" `
  -Body '{"nombre":"Ana Torres","email":"ana@example.com"}'

# Crea stock
Invoke-RestMethod -Uri http://localhost:8080/api/stock -Method Post `
  -ContentType "application/json" -Body '{"productoId":1,"cantidad":25}'
```

En Grafana, refresca y verás los contadores de negocio subir y el QPS de
`/api/productos` aumentar.

## Parar todo

```powershell
docker compose down        # no borra los datos (volúmenes)
docker compose down -v     # borra también las bases de datos y métricas
```