# Smart Campus Sensor & Room Management API

> **5COSC022W – Client-Server Architectures | University of Westminster**  
> A JAX-RS RESTful API for managing campus rooms and IoT sensors.

---

## Table of Contents

1. [API Design Overview](#1-api-design-overview)
2. [Project Structure](#2-project-structure)
3. [Technology Stack](#3-technology-stack)
4. [How to Build & Run](#4-how-to-build--run)
5. [Sample curl Commands](#5-sample-curl-commands)
6. [Endpoint Reference](#6-endpoint-reference)
7. [Report – Question Answers](#7-report--question-answers)

---

## 1. API Design Overview

The **Smart Campus API** is a fully RESTful web service that enables campus facilities managers and automated building systems to interact with campus infrastructure data. The system is built exclusively with **JAX-RS (Jersey 2.41)** deployed as a WAR on an embedded Apache Tomcat server.

### Core Design Principles

| Principle | Implementation |
|---|---|
| Versioned Entry Point | All endpoints are prefixed with `/api/v1` via `@ApplicationPath` |
| Resource Hierarchy | Rooms → Sensors → Readings (nested sub-resources) |
| In-Memory Store | `ConcurrentHashMap` via an enum-based singleton (`DataStore`) |
| Error Safety | Every error returns structured JSON — no raw stack traces ever |
| Observability | A JAX-RS filter logs every request and response automatically |

### Resource Hierarchy

```
/api/v1                          ← Discovery endpoint
├── /rooms                       ← Room management
│   ├── GET    /                 ← List all rooms
│   ├── POST   /                 ← Create a room
│   ├── GET    /{roomId}         ← Get a specific room
│   └── DELETE /{roomId}         ← Delete a room (blocked if sensors exist)
└── /sensors                     ← Sensor management
    ├── GET    /                 ← List all sensors (supports ?type= filter)
    ├── POST   /                 ← Register a sensor (validates roomId)
    ├── GET    /{sensorId}       ← Get a specific sensor
    ├── DELETE /{sensorId}       ← Delete a sensor
    ├── PATCH  /{sensorId}/status ← Update sensor status
    └── /{sensorId}/readings     ← Sub-resource (historical readings)
        ├── GET  /               ← Get reading history
        └── POST /               ← Append a new reading
```

### Data Models

**Room**
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 50,
  "sensorIds": ["TEMP-001", "CO2-002"]
}
```

**Sensor**
```json
{
  "id": "TEMP-001",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 21.5,
  "roomId": "LIB-301"
}
```

**SensorReading**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1713700000000,
  "value": 22.3
}
```

---

## 2. Project Structure

```
SmartCampusAPI/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/smartcampus/
        │       ├── SmartCampusApplication.java   ← JAX-RS Application entry point (@ApplicationPath)
        │       ├── DataStore.java                ← Enum singleton (in-memory ConcurrentHashMap store)
        │       ├── model/
        │       │   ├── Room.java
        │       │   ├── Sensor.java
        │       │   ├── SensorReading.java
        │       │   └── ApiError.java
        │       ├── resource/
        │       │   ├── DiscoveryResource.java     ← GET /api/v1
        │       │   ├── RoomResource.java          ← /api/v1/rooms
        │       │   ├── SensorResource.java        ← /api/v1/sensors
        │       │   └── SensorReadingResource.java ← Sub-resource for readings
        │       ├── exception/
        │       │   ├── RoomNotEmptyException.java
        │       │   ├── RoomNotEmptyExceptionMapper.java        ← 409 Conflict
        │       │   ├── LinkedResourceNotFoundException.java
        │       │   ├── LinkedResourceNotFoundExceptionMapper.java ← 422 Unprocessable
        │       │   ├── SensorUnavailableException.java
        │       │   ├── SensorUnavailableExceptionMapper.java   ← 403 Forbidden
        │       │   └── GlobalExceptionMapper.java              ← 500 catch-all
        │       └── filter/
        │           └── ApiLoggingFilter.java      ← Request/Response logging
        └── webapp/
            └── WEB-INF/
                ├── web.xml
                └── beans.xml
```

---

## 3. Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | 11 |
| REST Framework | JAX-RS via Jersey | 2.41 |
| JSON Serialisation | Jackson (via jersey-media-json-jackson) | bundled |
| Servlet Container | Tomcat (via tomcat7-maven-plugin) | 7.x (plugin) |
| Build Tool | Apache Maven | 3.x |
| Data Storage | `ConcurrentHashMap` / `CopyOnWriteArrayList` (in-memory) | — |

> **No database, no Spring Boot, no SQL** — as required by the coursework specification.

---

## 4. How to Build & Run

### Prerequisites

- **Java JDK 11** or later installed and on `PATH`
- **Apache Maven 3.6+** installed and on `PATH`
- An internet connection (for initial Maven dependency download)

### Step 1 – Clone the Repository

```bash
git clone https://github.com/<your-username>/SmartCampusAPI.git
cd SmartCampusAPI
```

### Step 2 – Build the Project

```bash
mvn clean package
```

A successful build will produce `target/smartcampus-api.war`.

### Step 3 – Run the Server

```bash
mvn tomcat7:run
```

The server starts on **http://localhost:8080**. You will see:

```
INFO: Starting Servlet Engine: Apache Tomcat/7.x.xx
INFO: Starting ProtocolHandler ["http-nio-8080"]
```

### Step 4 – Verify the Server is Running

```bash
curl http://localhost:8080/api/v1
```

Expected response:
```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "1.0.0",
  "description": "RESTful API for managing campus rooms and IoT sensors.",
  "contact": "admin@smartcampus.ac.uk",
  "_links": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

### Step 5 – Stop the Server

Press `Ctrl + C` in the terminal.

### Alternative: Run in NetBeans IDE

1. Open NetBeans → **File → Open Project** → select the `SmartCampusAPI` folder.
2. Right-click the project → **Run** (NetBeans will use the embedded Tomcat plugin automatically).

---

## 5. Sample curl Commands

> **Base URL:** `http://localhost:8080/api/v1`  
> All requests and responses use `Content-Type: application/json`.

---

### 1 – Discover the API (GET /api/v1)

```bash
curl -X GET http://localhost:8080/api/v1 \
     -H "Accept: application/json"
```

**Expected Response (200 OK):**
```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "1.0.0",
  "contact": "admin@smartcampus.ac.uk",
  "_links": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

### 2 – Create a Room (POST /api/v1/rooms)

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

**Expected Response (201 Created):**
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 50,
  "sensorIds": []
}
```

---

### 3 – Register a Sensor with roomId Validation (POST /api/v1/sensors)

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":21.5,"roomId":"LIB-301"}'
```

**Expected Response (201 Created):**
```json
{
  "id": "TEMP-001",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 21.5,
  "roomId": "LIB-301"
}
```

**Test 422 Unprocessable Entity (invalid roomId):**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{"id":"CO2-999","type":"CO2","status":"ACTIVE","currentValue":400,"roomId":"NONEXISTENT"}'
```

---

### 4 – Filter Sensors by Type (GET /api/v1/sensors?type=Temperature)

```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature" \
     -H "Accept: application/json"
```

**Expected Response (200 OK):**
```json
[
  {
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 21.5,
    "roomId": "LIB-301"
  }
]
```

---

### 5 – Post a Sensor Reading (POST /api/v1/sensors/{sensorId}/readings)

```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d '{"value":23.7}'
```

**Expected Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1713700000000,
  "value": 23.7
}
```

---

### 6 – Get Reading History (GET /api/v1/sensors/{sensorId}/readings)

```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Accept: application/json"
```

---

### 7 – Attempt to Delete a Room with Active Sensors (409 Conflict)

```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**Expected Response (409 Conflict):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' cannot be deleted because it still has 1 sensor(s) assigned to it."
}
```

---

### 8 – Post Reading to a MAINTENANCE Sensor (403 Forbidden)

```bash
# First, set sensor to MAINTENANCE
curl -X PATCH http://localhost:8080/api/v1/sensors/TEMP-001/status \
     -H "Content-Type: application/json" \
     -d '{"status":"MAINTENANCE"}'

# Then try to post a reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d '{"value":22.0}'
```

**Expected Response (403 Forbidden):**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Sensor 'TEMP-001' is currently under MAINTENANCE and cannot accept new readings."
}
```

---

## 6. Endpoint Reference

| Method | Path | Description | Success Code |
|--------|------|-------------|-------------|
| GET | `/api/v1` | API discovery & metadata | 200 |
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a new room | 201 |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room | 200 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (fails if sensors exist) | 200 |
| GET | `/api/v1/sensors` | List sensors (optional `?type=` filter) | 200 |
| POST | `/api/v1/sensors` | Register a sensor (validates roomId) | 201 |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor | 200 |
| DELETE | `/api/v1/sensors/{sensorId}` | Delete a sensor | 200 |
| PATCH | `/api/v1/sensors/{sensorId}/status` | Update sensor status | 200 |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history | 200 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a reading (blocked if MAINTENANCE) | 201 |

### HTTP Error Codes

| Code | Scenario |
|------|----------|
| 400 Bad Request | Missing required fields in request body |
| 403 Forbidden | Posting a reading to a MAINTENANCE sensor |
| 404 Not Found | Requested resource ID does not exist |
| 409 Conflict | Deleting a room that still has sensors assigned |
| 415 Unsupported Media Type | Sending non-JSON content (handled by JAX-RS automatically) |
| 422 Unprocessable Entity | Registering a sensor with a non-existent roomId |
| 500 Internal Server Error | Any unexpected server-side error |

---

## 7. Report – Question Answers

> Written answers to all conceptual questions in the coursework specification.

---

### Part 1.1 – JAX-RS Resource Lifecycle & Thread Safety

**Question:** In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.


**Answer:**

JAX-RS follows a **per-request lifecycle** lifecycle for the resource classes by default. The runtime created new objects for incoming HTTP request every time, processes them using that object and discards it after the response is sent. This is the standard behavior of JAX-RS specifications and its implemented by Jersey in the same way. That means instance fields cannot hold shared state, as each request get fresh objects with no memory of the previous.

To solve this, all shared data in this API is stored in a **`DataStore` enum singleton**, which the JVM guarantees to instantiate exactly once. The store uses **`ConcurrentHashMap`** instead of a plain `HashMap` to prevent race conditions when multiple requests arrive on different threads simultaneously, ensuring thread-safe reads and writes without data corruption.

---

### Part 1.2 – HATEOAS and API Discoverability

**Question:** Why is the provision of ”Hypermedia” (links and navigation within responses)
considered a hallmark of advanced RESTful design(HATEOAS)? How does this approach
benefit client developers compared to static documentation?


**Answer:**

HATEOAS (Hypermedia As The Engine Of Application State) means API responses include links to related resources and available actions, making the API self-describing. Key benefits over static documentation:

- **URL decoupling:** Clients follow links from responses rather than hardcoding URLs, so server-side path changes don't break clients.
- **Discoverability:** Developers can explore the entire API from one entry point (GET /api/v1) without external docs.
- **Always accurate:** Links come from the live server and cannot go stale like static documentation can.

In this API, the discovery endpoint returns a `_links` object pointing to `/api/v1/rooms` and `/api/v1/sensors`.

---

### Part 2.1 – Returning IDs vs. Full Objects in List Responses

**Question:** When returning a list of rooms, what are the implications of returning only
IDs versus returning the full room objects? Consider network bandwidth and client side
processing.


**Answer:**

This is a classic API design trade-off between bandwidth efficiency and client complexity.

- **IDs only:** Produces a small payload but forces the client to make N additional GET requests to fetch details.The N+1 problem. Suitable only when identifiers are all that is needed.
- **Full objects:** Larger payload but allows a complete list to be rendered in a single round-trip that more efficient for real-world dashboard use cases.

Decision in this API: Full room objects are returned from GET /api/v1/rooms because campus management dashboards require all fields at the same time. If the list were to grow to thousands of rooms, the recommended evolution would be to introduce cursor-based pagination with lightweight summary objects eliminating large payload transfers while avoiding the N+1 problem

---

### Part 2.2 – DELETE Idempotency

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE
request for a room multiple times.


**Answer:**

Yes. DELETE is **idempotent** per RFC 9110 — multiple calls produce the same server state as one call.

- **First DELETE call** for room LIB-301 (room exists, no sensors): The room is removed from the DataStore. Server state: room absent. Response: `200 OK` with confirmation message.
- **Second DELETE call** for the same LIB-301: The room no longer exists. Server state: room absent Response: `404 Not Found`.

The server state - "room LIB-301 does not exist is identical after both calls. This satisfies idempotency. The fact that the response code differs (200 vs 404) is permitted; RFC 9110 does not require the response to be identical, only the effect on the resource.

---

### Part 3.1 – `@Consumes(APPLICATION_JSON)` and Content Negotiation

**Question:** We explicitly use the @Consumes (MediaType APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in
a different format, such as text/plain or application/xml. How does JAX-RS handle this
mismatch? 


**Answer:**

JAX-RS performs **content negotiation** before calling any resource method. If the request Content-Type header does not match application/json, the framework immediately rejects the request with HTTP `415 Unsupported Media Type`.The resource method code never executes. This protects the application from malformed or unexpected data without requiring any extra defensive code inside the resource method.

---

### Part 3.2 – `@QueryParam` vs. Path Segment for Filtering

**Question:** You implemented this filtering using @QueryParam.Contrast this with an alternative design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?


**Answer:**

- **Path-based** (`/sensors/type/CO2`) implies a different resource requires a separate route for each filter and breaks down when combining multiple filters.
- **Query parameters** (`/sensors?type=CO2`) are semantically correct per RFC 3986.They refine a collection view, not identify a new resource.

Query params are optional by design naturally composable (`?type=CO2&status=ACTIVE`) and are the industry-standard approach used by all major REST APIs such as GitHub.

---

### Part 4.1 – Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?


**Answer:**

A sub-resource locator is a `@Path`-annotated method with no HTTP verb that returns another class instance to handle the sub-path. In this API, `SensorResource` delegates all `/readings` requests to `SensorReadingResource`.

**Benefits:**
- **Single Responsibility:** Each class handles one concern.Sensors or readings, not both.
- **Avoids God Classes:** All nested routes in one class becomes unmanageable as the API grows.
- **Open/Closed Principle:** New sub-resources (e.g., `/alerts`) are added as new classes without changing existing ones.
- **Testability:** Each class can be unit-tested in complete isolation.

---

### Part 5.2 – HTTP 422 vs. HTTP 404 for Missing References

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload? 


**Answer:**

**404** signals that the requested **URL was not found**. In this scenario, the URL `/api/v1/sensors` was found correctly.The problem is that the roomId field inside the payload references a non-existent room. The endpoint was reached correctly.

**422** Unprocessable Entity is designed for exactly this. The request is syntactically valid JSON, but contains a **semantic error**. Using 422 gives an actionable signal ("your data is wrong") to clients  rather than 404 ("your URL is wrong"), which is more correct and easier to handle on the client side.

---

### Part 5.4 – Cybersecurity Risks of Exposing Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?


**Answer:**

Exposing stack traces is classified under **OWASP API8 – Security Misconfiguration**. An attacker can gather:

1. **Framework and version info** — used to find known CVEs and target unpatched vulnerabilities.
2. **Internal class and method names** — reveals application structure and potential injection points.
3. **File system paths** — reveals server directory layout and OS details.
4. **Application logic flow** — the call stack maps how requests are processed, helping craft targeted attacks.
5. **Database Schema Leakage** - ORM related stack traces can expose table names, column names, and query details.

The `GlobalExceptionMapper<Throwable>` in this API prevents all of this by logging full details server-side and returning only a safe generic `500` message to the client.

---

### Part 5.5 – Filters vs. Manual Logging

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?


**Answer:**

Using `ContainerRequestFilter` / `ContainerResponseFilter` is far superior to manual logging:

- **DRY:** One filter automatically covers all endpoints. No risk of missing a new endpoint.
- **Separation of Concerns:** Resource methods stay focused on business logic, not visibility.
- **Centralised control:** Changing the log format means editing one class, not every resource method.
- **Consistent coverage:** The filter symmetrically logs every request and every response without any per-method effort.

Filters apply cross-cutting concerns cleanly across the entire API, following the same principle as Aspect-Oriented Programming (AOP).

---

*End of Report*
