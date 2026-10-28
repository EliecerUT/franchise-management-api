# Franchises API

API REST para gestionar una lista de **franquicias**. Cada franquicia tiene un nombre y un listado de **sucursales**, y cada sucursal tiene un nombre y un listado de **productos** con su cantidad de **stock**.

Además de los campos mínimos que pide el enunciado, el modelo incluye los datos que necesitaría un negocio real: NIT y datos de contacto de la franquicia, dirección y estado de la sucursal, SKU, precio y stock mínimo del producto, y fechas de auditoría en los tres niveles.

## Tecnologías

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 (Spring MVC, Spring Data MongoDB, Bean Validation) |
| Base de datos | MongoDB 8 |
| Documentación | springdoc-openapi 3 (Swagger UI) |
| Tests | JUnit 5, Mockito, MockMvc, RestTestClient, Testcontainers |
| Build | Maven (wrapper incluido: `mvnw`) |

## Requisitos

- **JDK 21**, con la variable `JAVA_HOME` apuntando a él.
- **Docker**, para levantar MongoDB en local y ejecutar los tests de integración.
- No hace falta instalar Maven: el proyecto incluye el wrapper `mvnw` / `mvnw.cmd`.

## Ejecutar en local

### 1. Clonar el repositorio

```bash
git clone https://github.com/EliecerUT/franchise-management-api.git
cd franchise-management-api
```

### 2. Levantar MongoDB

```bash
docker run -d --name franchises-mongo -p 27017:27017 mongo:8.0
```

> Si ya tienes un MongoDB propio (local o en la nube), sáltate este paso y usa la variable `MONGODB_URI` descrita en [Configuración](#configuración).

### 3. Arrancar la aplicación

```bash
# Linux / macOS / Git Bash
./mvnw spring-boot:run

# Windows (cmd o PowerShell)
mvnw.cmd spring-boot:run
```

O bien generando el JAR:

```bash
./mvnw clean package
java -jar target/franchises-api-0.0.1-SNAPSHOT.jar
```

La API queda disponible en `http://localhost:8080`. Al arrancar se crean los índices únicos de la colección `franchises`.

### Configuración

Se configura mediante variables de entorno (ver `src/main/resources/application.yml`):

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/franchises` | Cadena de conexión a MongoDB (incluye el nombre de la base de datos) |
| `PORT` | `8080` | Puerto HTTP de la aplicación |

```bash
MONGODB_URI="mongodb+srv://usuario:clave@cluster.mongodb.net/franchises" PORT=9090 ./mvnw spring-boot:run
```

### Documentación interactiva (Swagger UI)

Con la aplicación arrancada:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Especificación OpenAPI (JSON): http://localhost:8080/v3/api-docs

Cada endpoint documenta sus campos con ejemplos, qué campos son obligatorios y qué respuestas de error puede devolver.

## Endpoints

Ruta base: `/api/v1/franchises`

| # | Método | Ruta | Descripción | Éxito |
|---|---|---|---|---|
| 2 | `POST` | `/api/v1/franchises` | Agregar una franquicia | `201` |
| 3 | `POST` | `/api/v1/franchises/{franchiseId}/branches` | Agregar una sucursal a una franquicia | `201` |
| 4 | `POST` | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products` | Agregar un producto a una sucursal | `201` |
| 5 | `DELETE` | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Eliminar un producto de una sucursal | `204` |
| 6 | `PATCH` | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` | Modificar el stock de un producto | `200` |
| 7 | `GET` | `/api/v1/franchises/{franchiseId}/top-stock-products` | Producto con más stock de cada sucursal | `200` |
| | `GET` | `/api/v1/franchises` | Listar franquicias | `200` |
| | `GET` | `/api/v1/franchises/{franchiseId}` | Consultar una franquicia con sus sucursales y productos | `200` |
| | `GET` | `/api/v1/franchises/{franchiseId}/branches/{branchId}` | Consultar una sucursal | `200` |
| | `GET` | `/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Consultar un producto | `200` |

La columna `#` indica el criterio de aceptación que cubre cada endpoint. Las creaciones (`201`) devuelven la cabecera `Location` con la URL del recurso creado.

### Campos de entrada

Solo los campos en **negrita** son obligatorios; el resto puede omitirse.

**Franquicia** (`POST /franchises`)

| Campo | Regla |
|---|---|
| **`name`** | Hasta 100 caracteres. Único en toda la API, sin distinguir mayúsculas |
| `legalName` | Razón social, hasta 150 caracteres |
| `taxId` | NIT, RUT, …: 5 a 30 letras, dígitos, puntos o guiones. Único si se informa; se guarda en mayúsculas |
| `description` | Hasta 500 caracteres |
| `email` | Correo válido; se guarda en minúsculas |
| `phone` | 7 a 20 dígitos, espacios, guiones o paréntesis, con `+` opcional |
| `website` | URL `http://` o `https://` |

La franquicia se crea siempre en estado `ACTIVE` y sin sucursales.

**Sucursal** (`POST /franchises/{franchiseId}/branches`)

| Campo | Regla |
|---|---|
| **`name`** | Hasta 100 caracteres. Único dentro de la franquicia, sin distinguir mayúsculas |
| `code` | Código interno (p. ej. `BOG-001`): 2 a 30 letras, dígitos o guiones. Único dentro de la franquicia; se guarda en mayúsculas |
| `address` | Objeto con `street`, **`city`**, `state`, **`country`** y `postalCode` (ciudad y país obligatorios si se envía) |
| `phone`, `email` | Mismas reglas que en la franquicia |
| `managerName` | Responsable, hasta 100 caracteres |
| `openingDate` | Fecha `yyyy-MM-dd`; no puede ser futura |
| `status` | `OPEN` (por defecto), `TEMPORARILY_CLOSED` o `CLOSED` |

**Producto** (`POST /franchises/{franchiseId}/branches/{branchId}/products`)

| Campo | Regla |
|---|---|
| **`name`** | Hasta 100 caracteres. Único dentro de la sucursal, sin distinguir mayúsculas |
| **`price`** | Mayor que cero, con hasta 2 decimales |
| **`stock`** | Entero mayor o igual que cero |
| `sku` | 2 a 30 letras, dígitos o guiones. Único dentro de la sucursal; se guarda en mayúsculas |
| `description` | Hasta 500 caracteres |
| `category` | Hasta 60 caracteres |
| `currency` | Código ISO 4217 de 3 letras; `COP` por defecto |
| `minimumStock` | Umbral de reposición, entero mayor o igual que cero; `0` por defecto |
| `active` | Si el producto se sigue vendiendo; `true` por defecto |

En todos los textos se eliminan los espacios de los extremos, y un texto vacío en un campo opcional se guarda como `null`.

### Reglas de negocio

- Una franquicia `INACTIVE` no admite nuevas sucursales (`422`).
- Una sucursal `CLOSED` no admite nuevos productos (`422`). Una sucursal `TEMPORARILY_CLOSED` sí, porque sigue gestionando su inventario.
- Un producto está en **stock bajo** (`lowStock: true` en la respuesta) cuando su stock es igual o inferior a `minimumStock`.

### Ejemplo completo con curl

Reemplaza `{franchiseId}`, `{branchId}` y `{productId}` por los `id` que devuelve cada respuesta. Las respuestas se muestran abreviadas.

**Crear una franquicia**

```bash
curl -X POST http://localhost:8080/api/v1/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": "Burger Express", "taxId": "900123456-7", "email": "contacto@burgerexpress.com"}'
```

```json
{
  "id": "6ae0375cc698e94971e2fce3",
  "name": "Burger Express",
  "taxId": "900123456-7",
  "email": "contacto@burgerexpress.com",
  "status": "ACTIVE",
  "branches": [],
  "createdAt": "2026-10-27T15:04:05.123Z",
  "updatedAt": "2026-10-27T15:04:05.123Z"
}
```

**Agregar una sucursal**

```bash
curl -X POST http://localhost:8080/api/v1/franchises/{franchiseId}/branches \
  -H "Content-Type: application/json" \
  -d '{"name": "Centro", "code": "bog-001", "address": {"city": "Bogotá", "country": "Colombia"}}'
```

```json
{
  "id": "39b98175-891a-4f25-935d-ed5d67f8256b",
  "code": "BOG-001",
  "name": "Centro",
  "address": { "street": null, "city": "Bogotá", "state": null, "country": "Colombia", "postalCode": null },
  "status": "OPEN",
  "products": []
}
```

**Agregar un producto a la sucursal**

```bash
curl -X POST http://localhost:8080/api/v1/franchises/{franchiseId}/branches/{branchId}/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Hamburguesa clásica", "sku": "HAM-001", "price": 18900, "stock": 40, "minimumStock": 10}'
```

```json
{
  "id": "7f13092a-0327-4c14-8210-f557660e911d",
  "sku": "HAM-001",
  "name": "Hamburguesa clásica",
  "price": 18900,
  "currency": "COP",
  "stock": 40,
  "minimumStock": 10,
  "lowStock": false,
  "active": true
}
```

**Modificar el stock**

```bash
curl -X PATCH http://localhost:8080/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock \
  -H "Content-Type: application/json" \
  -d '{"stock": 8}'
```

```json
{ "id": "7f13092a-0327-4c14-8210-f557660e911d", "name": "Hamburguesa clásica", "stock": 8, "minimumStock": 10, "lowStock": true }
```

**Eliminar un producto** (responde `204 No Content`)

```bash
curl -X DELETE http://localhost:8080/api/v1/franchises/{franchiseId}/branches/{branchId}/products/{productId}
```

**Producto con más stock por sucursal**

```bash
curl http://localhost:8080/api/v1/franchises/{franchiseId}/top-stock-products
```

```json
[
  {
    "branchId": "39b98175-…", "branchCode": "BOG-001", "branchName": "Centro", "branchStatus": "OPEN",
    "productId": "f90f6e76-…", "sku": "PAP-001", "productName": "Papas", "category": "Acompañamientos",
    "stock": 25, "minimumStock": 5, "lowStock": false
  },
  {
    "branchId": "4a5fcf3d-…", "branchCode": "BOG-002", "branchName": "Norte", "branchStatus": "TEMPORARILY_CLOSED",
    "productId": "05d44885-…", "sku": "GAS-001", "productName": "Gaseosa", "category": "Bebidas",
    "stock": 60, "minimumStock": 20, "lowStock": false
  }
]
```

Reglas del reporte:
- Devuelve un elemento por sucursal, con la sucursal a la que pertenece el producto.
- Las sucursales sin productos no aparecen.
- En caso de empate de stock, gana el producto que se agregó primero.

### Errores

Todas las respuestas de error siguen el formato **Problem Details** ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)) con `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/franchises",
  "errors": { "name": "no debe estar vacío", "taxId": "must contain 5 to 30 letters, digits, dots or hyphens" }
}
```

| Código | Cuándo |
|---|---|
| `400` | Cuerpo inválido según las reglas de [Campos de entrada](#campos-de-entrada), o JSON mal formado. `errors` indica el campo y el motivo; los mensajes estándar de Bean Validation salen en el idioma del sistema |
| `404` | La franquicia, la sucursal o el producto no existe |
| `409` | Nombre, NIT, código de sucursal o SKU repetido en su ámbito, o modificación concurrente de la misma franquicia |
| `422` | La petición es válida pero el estado no lo permite: franquicia `INACTIVE` o sucursal `CLOSED` |

## Diseño

### Modelo de datos

Cada franquicia se guarda como **un único documento** en la colección `franchises` de MongoDB, con sus sucursales y productos embebidos:

```json
{
  "_id": "6ae0375cc698e94971e2fce3",
  "name": "Burger Express",
  "taxId": "900123456-7",
  "status": "ACTIVE",
  "branches": [
    {
      "id": "39b98175-891a-4f25-935d-ed5d67f8256b",
      "code": "BOG-001",
      "name": "Centro",
      "address": { "city": "Bogotá", "country": "Colombia" },
      "status": "OPEN",
      "products": [
        { "id": "7f13092a-…", "sku": "HAM-001", "name": "Hamburguesa clásica",
          "price": { "$numberDecimal": "18900" }, "currency": "COP", "stock": 8, "minimumStock": 10, "active": true }
      ]
    }
  ],
  "createdAt": { "$date": "2026-10-27T15:04:05.123Z" },
  "updatedAt": { "$date": "2026-10-27T15:09:41.517Z" },
  "version": 4
}
```

- Una sucursal o un producto no existen fuera de su franquicia, así que se leen y se escriben juntos: cada operación es una lectura y una escritura atómica del documento, y el reporte de stock se resuelve con una sola consulta.
- Los ids de sucursales y productos son UUID generados por la aplicación, porque MongoDB solo genera `_id` para el documento raíz.
- **Índices únicos**: el nombre de la franquicia (con *collation* `es`, fuerza 2, para no distinguir mayúsculas) y el NIT (índice *sparse*, porque es opcional). La unicidad de nombres, códigos y SKU dentro de una franquicia o sucursal la comprueba el servicio, ya que viven en el mismo documento.
- Los precios son `BigDecimal` y se guardan como `Decimal128`, para no perder precisión con decimales.
- `createdAt` y `updatedAt` de la franquicia los asigna la auditoría de Spring Data; los de sucursales y productos, el servicio. Todos salen del mismo `Clock`, que los tests pueden fijar.
- El campo `version` (`@Version`) aplica **bloqueo optimista**: si dos peticiones modifican la misma franquicia a la vez, la segunda recibe `409` en lugar de sobrescribir los cambios de la primera.

### Estructura del código

```
src/main/java/com/example/franchisesapi
├── config/       Auditoría de MongoDB + Clock, y metadatos/respuestas comunes de OpenAPI
├── controller/   Endpoints REST: traducen HTTP <-> servicio, sin lógica de negocio
├── dto/          Records de entrada (validados con Bean Validation) y de salida
├── service/      Reglas de negocio: unicidad, estados, normalización de textos, reporte
├── model/        Documento de MongoDB: Franchise -> Branch -> Product (+ Address y estados)
├── repository/   FranchiseRepository (Spring Data MongoDB)
└── exception/    Excepciones propias y GlobalExceptionHandler (Problem Details)
```

## Tests

```bash
./mvnw test
```

| Tipo | Clases | Necesita Docker |
|---|---|---|
| Unitarios del modelo | `FranchiseTest` | No |
| Unitarios de servicios (Mockito) | `FranchiseServiceTest`, `BranchServiceTest`, `ProductServiceTest` | No |
| Capa web (`@WebMvcTest`) | `*ControllerTest`, `GlobalExceptionHandlerTest` | No |
| Repositorio (`@DataMongoTest`) | `FranchiseRepositoryTest` | Sí |
| Documentación OpenAPI | `OpenApiDocumentationTest` | Sí |
| Extremo a extremo (`RANDOM_PORT` + HTTP real) | `FranchisesApiIntegrationTest` | Sí |

Los tests que necesitan MongoDB lo levantan automáticamente con **Testcontainers** (`mongo:8.0`). Si Docker no está en ejecución, **se saltan** en lugar de fallar.

## Flujo de trabajo con Git

El proyecto sigue **Git Flow**:

- `main`: versiones estables, etiquetadas por fase (`v0.1.0`, `v0.2.0`, …).
- `develop`: rama de integración y rama por defecto del repositorio.
- Ramas de trabajo creadas desde `develop` con prefijo según su tipo (`feature/`, `chore/`, `test/`, `docs/`), integradas mediante **Pull Request**.

Los Pull Requests del repositorio muestran cada cambio con su descripción.
