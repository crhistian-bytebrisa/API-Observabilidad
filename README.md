# Ventas API

API REST de ventas con Spring Boot que gestiona **productos**, **clientes**
y **stock**, todo persistido en **PostgreSQL** y levantado con Docker Compose.

## Qué incluye

- API REST de productos, clientes y stock (Java 21, Spring Boot 4.1.1)
- CRUD completo para cada recurso
- PostgreSQL 16 como base de datos en el mismo Docker Compose
- Swagger UI para probar los endpoints desde el navegador
- Estructura por capas: controller -> service -> repository -> model

## Levantar todo (API + PostgreSQL)

Solo necesitas Docker. Un solo comando levanta todo:

```powershell
docker compose up -d
```

Quedan estas cosas corriendo:

| Servicio          | URL                                        |
|-------------------|--------------------------------------------|
| API Spring Boot   | http://localhost:8080                      |
| Swagger UI        | http://localhost:8080/swagger-ui.html      |
| PostgreSQL        | localhost:5432 (usuario/BD: admin)         |

## Endpoints

### Productos (`/api/productos`)

| Método | URL                   | Qué hace                  |
|--------|-----------------------|---------------------------|
| GET    | `/api/productos`      | Lista todos los productos |
| GET    | `/api/productos/{id}` | Busca un producto por id  |
| POST   | `/api/productos`      | Crea un producto          |
| PUT    | `/api/productos/{id}` | Actualiza un producto     |
| DELETE | `/api/productos/{id}` | Elimina un producto       |

El POST espera un JSON así:

```json
{
  "nombre": "SSD 1TB NVMe",
  "descripcion": "Disco sólido PCIe 4.0",
  "precio": 129.00
}
```

### Clientes (`/api/clientes`)

| Método | URL                  | Qué hace                |
|--------|----------------------|-------------------------|
| GET    | `/api/clientes`      | Lista todos los clientes |
| GET    | `/api/clientes/{id}` | Busca un cliente por id |
| POST   | `/api/clientes`      | Crea un cliente         |
| PUT    | `/api/clientes/{id}` | Actualiza un cliente    |
| DELETE | `/api/clientes/{id}` | Elimina un cliente      |

El POST espera un JSON así:

```json
{
  "nombre": "Ana Torres",
  "email": "ana@example.com",
  "telefono": "+34 600 000 000",
  "direccion": "Calle Luna 3, Sevilla"
}
```

### Stock (`/api/stock`)

| Método | URL             | Qué hace           |
|--------|-----------------|--------------------|
| GET    | `/api/stock`    | Lista el stock     |
| GET    | `/api/stock/{id}` | Busca stock por id |
| POST   | `/api/stock`    | Crea stock         |
| PUT    | `/api/stock/{id}` | Actualiza stock  |
| DELETE | `/api/stock/{id}` | Elimina stock    |

El POST espera un JSON así:

```json
{
  "productoId": 3,
  "cantidad": 60
}
```

## Base de datos

La base `ventas` se crea automáticamente con PostgreSQL en el Docker Compose.
Podés conectarte desde cualquier cliente PG con:

- Host: `localhost`
- Puerto: `5432`
- Base/Usuario/Contraseña: `ventas`

Las tablas y datos iniciales se crean al arrancar la API. Podés desactivar
los datos de ejemplo quitando `src/main/resources/data.sql` o cambiando
`spring.jpa.defer-datasource-initialization` a `false`.

## Parar todo

```powershell
docker compose down
```
