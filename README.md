# Campus Sensor Management API

A RESTful API built with **JAX-RS (Jersey 2.41)** and an embedded **Grizzly HTTP server** for managing campus IoT sensor rooms and their historical readings. All data is stored in-memory using `ConcurrentHashMap` and `CopyOnWriteArrayList`.

---

## API Overview

| Base URL | `http://localhost:8080/api/v1` |
|---|---|
| Format | JSON only |
| Architecture | JAX-RS (Jersey) + Grizzly embedded server |
| Data Storage | In-memory (`ConcurrentHashMap`, `CopyOnWriteArrayList`) |

### Resource Map

| Resource | Path |
|---|---|
| Discovery | `GET /api/v1` |
| Rooms | `/api/v1/rooms` |
| Sensors | `/api/v1/sensors` |
| Readings (sub-resource) | `/api/v1/sensors/{sensorId}/readings` |

---

## Build & Run Instructions

### Prerequisites
- Java 11+
- Maven 3.6+

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/campus-sensor-api.git
cd campus-sensor-api
```

### 2. Build the project (creates a fat JAR)
```bash
mvn clean package
```

### 3. Start the server
```bash
java -jar target/campus-sensor-api-1.0.0.jar
```

The server starts at: `http://localhost:8080/api/v1`

---

## Sample curl Commands

### 1. Discovery endpoint
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lab A101",
    "building": "Engineering Block",
    "floor": 1,
    "capacity": 30,
    "description": "Primary IoT testing lab"
  }'
```

### 3. List all Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Get a specific Room by ID
```bash
curl -X GET http://localhost:8080/api/v1/rooms/ROOM_ID
```

### 5. Register a Sensor (replace ROOM_ID with the id from step 2)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CO2 Sensor Alpha",
    "type": "CO2",
    "roomId": "ROOM_ID",
    "unit": "ppm"
  }'
```

### 6. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 7. Post a reading to a sensor (replace SENSOR_ID)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR_ID/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 412.5,
    "unit": "ppm",
    "notes": "Normal atmospheric reading"
  }'
```

### 8. Get all readings for a sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR_ID/readings
```

### 9. Delete a sensor
```bash
curl -X DELETE http://localhost:8080/api/v1/sensors/SENSOR_ID
```

### 10. Attempt to delete a room that still has sensors (expect 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/ROOM_ID
```

### 11. Register sensor with invalid roomId (expect 422 Unprocessable Entity)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bad Sensor",
    "type": "TEMP",
    "roomId": "nonexistent-room-id",
    "unit": "C"
  }'
```

---

## Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each Resource class for every incoming HTTP request** (per-request lifecycle). This means no state is preserved between calls on the resource object itself.

This design has direct implications for shared data management. Since each request gets its own resource instance, any instance-level fields would be lost after the request completes. Therefore, all shared state — rooms, sensors, and readings — must live outside the resource classes, in a centralised singleton (`DataStore`). To prevent race conditions when multiple requests access or modify that shared store concurrently, thread-safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) are used rather than standard `HashMap` or `ArrayList`.

---

### Part 1.2 — HATEOAS and Hypermedia

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include navigational links alongside data, allowing clients to discover available actions dynamically rather than relying on external documentation.

The key benefit is **loose coupling**: a client does not need to hard-code URIs or memorise the API structure. It simply follows links returned in each response (e.g., after creating a room, the response contains a link to that room's detail endpoint and to the sensors collection). This makes client code more resilient to API changes and dramatically reduces the onboarding time for new developers compared to maintaining separate static documentation that can fall out of date.

---

### Part 2.1 — Returning IDs vs Full Room Objects

Returning only IDs in a list response is bandwidth-efficient, but it forces the client to make N additional requests to fetch details for each room — the classic "N+1 problem". This increases latency and network load proportionally with the number of rooms.

Returning full room objects costs more bandwidth per response but allows the client to render a complete room list in a single round-trip. For typical room counts in a campus system, the bandwidth cost is negligible and the reduced latency is worth it. A good compromise (used in this API) is to return full objects in list responses but also include a `count` field so clients can paginate if the dataset grows.

---

### Part 2.2 — DELETE Idempotency

The DELETE operation in this implementation is **not fully idempotent in terms of response**, though it is idempotent in terms of final state.

- **First DELETE**: If the room exists and has no sensors, it is removed. Returns `200 OK` with a confirmation message.
- **Subsequent DELETE (same ID)**: The room no longer exists, so the endpoint returns `404 Not Found`.

Strict REST theory defines idempotency as the *server state* being identical after repeated calls (i.e., the resource remains absent), which is satisfied here. However, the *response code* differs between the first and subsequent calls. This is an accepted and common design trade-off — returning `404` on a second delete is more informative to the client than pretending the resource still exists.

---

### Part 3.1 — @Consumes(APPLICATION_JSON) Consequences

When `@Consumes(MediaType.APPLICATION_JSON)` is declared on a POST method, JAX-RS uses it for **content negotiation**. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime cannot find a matching resource method and returns **`415 Unsupported Media Type`** automatically — before the method body is even executed. This prevents malformed or unexpected data formats from reaching business logic, acting as a first line of input validation at the framework level.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

Using `@QueryParam` (e.g., `GET /api/v1/sensors?type=CO2`) is the correct approach for filtering and searching because:

1. **Semantic clarity**: Path segments (e.g., `/sensors/type/CO2`) imply a distinct resource identity. Query parameters communicate that the same resource collection is being filtered.
2. **Optional by nature**: Query params are naturally optional. A path segment approach requires a separate route definition for the unfiltered case.
3. **Multiple filters**: Query params compose easily (`?type=CO2&status=ACTIVE`). Nested path segments become unmanageable.
4. **REST convention**: RESTful standards (and RFC 3986) treat the path as the resource identifier and the query string as refinement criteria — filtering is refinement, not a new resource.

---

### Part 4.1 — Sub-Resource Locator Pattern Benefits

The Sub-Resource Locator pattern (returning a new resource instance from a `@Path`-annotated method without an HTTP verb annotation) delegates handling of nested paths to dedicated classes. The key architectural benefits are:

1. **Separation of concerns**: `SensorResource` manages sensor collection operations; `SensorReadingResource` exclusively handles reading history. Each class has a single, well-defined responsibility.
2. **Manageable complexity**: Without this pattern, a single controller would need to handle every possible nested path (`/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}` etc.), leading to a bloated "God class".
3. **Testability**: Each sub-resource can be unit-tested in isolation by instantiating it directly with a known `sensorId`.
4. **Reusability**: Sub-resource classes can theoretically be reused across multiple parent paths if the same reading logic applies to multiple sensor types.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Referenced Resource

When a client POSTs a new sensor with a `roomId` that does not exist, the request URI (`/api/v1/sensors`) is entirely valid and the JSON body is syntactically correct. The problem is **semantic**: a field inside the payload references a non-existent dependency.

- **404 Not Found** implies the *requested URL* was not found — which is misleading since `/api/v1/sensors` exists perfectly.
- **422 Unprocessable Entity** means "the server understands the request format but cannot process it due to semantic errors in the content." This is precisely the situation: the payload is valid JSON, but its business logic is broken because the referenced room does not exist.

Therefore, `422` is more semantically precise and gives API consumers a clearer signal to look at the *contents* of their request rather than the URL.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers creates several security risks:

1. **Technology fingerprinting**: Stack traces reveal the exact framework, library versions, and package structure (e.g., `org.glassfish.jersey 2.41`, `com.campus.api.resources`). Attackers use this to look up known CVEs for those specific versions.
2. **Internal path disclosure**: Full class paths and file names reveal the application's internal architecture, making it easier to craft targeted attacks.
3. **Logic disclosure**: Stack traces reveal the call chain — which methods were invoked, in what order, and from which classes. This provides a roadmap for understanding business logic vulnerabilities.
4. **Information for injection attacks**: Error messages embedded in traces (e.g., SQL errors, null pointer sources) can reveal variable names, data types, and query structures useful for SQL injection or parameter tampering.

The global `ExceptionMapper<Throwable>` prevents all of this by logging the full trace **server-side only** and returning a safe, generic `500` message to the client.

---

### Part 5.5 — JAX-RS Filters vs Manual Logging

Using a JAX-RS filter for cross-cutting concerns like logging is superior to manually inserting `Logger.info()` in every resource method for several reasons:

1. **DRY principle**: Logging logic lives in exactly one class. Any change (format, log level, destination) requires editing one file.
2. **Completeness**: A filter automatically covers every endpoint — including new ones added in the future — with zero additional effort. Manual insertion is error-prone and easily forgotten.
3. **Clean resource methods**: Resources stay focused on business logic. Mixing observability code into them violates the Single Responsibility Principle.
4. **Consistent format**: All log entries follow a uniform pattern, making log aggregation and analysis (e.g., in tools like Splunk or ELK) far easier.
5. **Lifecycle guarantee**: Filters execute for *every* request and response cycle, including error paths handled by exception mappers, which a manually placed `Logger.info()` at the top of a method would miss.
