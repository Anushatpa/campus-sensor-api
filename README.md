# Campus Sensor API

This is my coursework project for Client-Server Architectures. It's a REST API built with JAX-RS (Jersey) that manages rooms and sensors for a smart campus system. The server runs on an embedded Grizzly HTTP server and stores all data in memory using HashMaps.

---

## What the API does

It let's you create rooms, add sensors to those rooms, and record sensor readings over time. You can also view the history of all readings for any sensor.

---

## Tech used

- Java 11
- JAX-RS with Jersey 2.41
- Grizzly embedded server
- Jackson for JSON
- Maven for building

---

## How to run it


**Step 1** — Cloning the repo
```bash
git clone https://github.com/Anushatpa/campus-sensor-api.git
cd campus-sensor-api
```

**Step 2** — Building it
```bash
mvn clean package
```

**Step 3** — Running it
```bash
java -jar target/campus-sensor-api-1.0.0.jar
```

The server will start at `http://localhost:8080/api/v1`

---

## Curl examples

**1. See the API info**
```bash
curl -X GET http://localhost:8080/api/v1
```

**2. Create a room**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lab A101",
    "building": "Engineering Block",
    "floor": 1,
    "capacity": 30,
    "description": "Main lab"
  }'
```

**3. Get all rooms**
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

**4. Add a sensor to a room**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CO2 Sensor",
    "type": "CO2",
    "roomId": "PASTE_ROOM_ID_HERE",
    "unit": "ppm"
  }'
```

**5. Filter sensors by type**
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

**6. Post a reading**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/SENSOR_ID/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 412.5,
    "unit": "ppm",
    "notes": "Normal reading"
  }'
```

**7. Get reading history**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/SENSOR_ID/readings
```

---

## Report

### Part 1.1 — JAX-RS Resource Lifecycle

In JAX-RS, a new resource object is created every time a request comes in. So if multiple users send requests at the same time, each request gets its own separate object. Because of this, you can’t store shared data inside the resource class itself, since that data would be lost after the request finishes.

To solve this, I created a separate DataStore class using the singleton pattern. This means there is only one shared instance of the data available to the whole application. All resource classes use this shared DataStore to store and retrieve rooms and sensors.

Since multiple requests can access this data at the same time, I used ConcurrentHashMap instead of a normal HashMap. This makes sure the data stays safe and consistent even when many users are using the API at once.

---

### Part 1.2 — HATEOAS

HATEOAS means that an API response doesn’t just return data, it also includes links that show what the client can do next. So instead of only getting information, the client also gets guidance on the next possible actions.

For example, when a room is created, the response includes the room details along with links to view that room, delete it, or go back to the list of all rooms. This makes the API feel more interactive and easier to navigate.

The main benefit is that developers don’t have to remember all the URLs or keep checking documentation. They can simply follow the links provided in the response, just like clicking links on a website. It also makes the API more flexible. If the URL structure changes in the future, clients that rely on these links will continue to work without needing any updates.

---

### Part 2.1 — Returning IDs vs Full Objects

If an API only returns IDs in a list, the client has to make another request for each item to get the full details. For example, if there are 50 rooms, the client would need to send 50 extra requests just to see all the information. This is known as the N+1 problem, and it can make the system slow and inefficient.

On the other hand, returning full objects means the response contains all the details in one go. While this makes the response slightly larger, it saves a lot of time because the client doesn’t need to send multiple requests.

In this project, returning full room objects is a better choice. Since a campus system won’t have a huge number of rooms, the extra data size is not a big issue, and the improved speed and simplicity make it worth it.

---

### Part 2.2 — DELETE Idempotency

When you send a DELETE request for a room that exists and has no sensors, the room is removed and the server returns a 200 OK response. This means the operation was successful. If you send the same DELETE request again for the same room, the room is already gone. In this case, the server returns 404 Not Found because there is nothing left to delete.

Even though the responses are different, the final state of the system is the same, the room does not exist. This is what makes the operation idempotent. Calling DELETE multiple times does not change the result after the first successful deletion.

This is a common and accepted approach in REST APIs. Returning 404 in the second request is helpful because it clearly tells the client that the resource is no longer there, instead of pretending the deletion worked again.

---

### Part 3.1 — @Consumes(APPLICATION_JSON)

When you use @Consumes(MediaType.APPLICATION_JSON) on a method, it tells JAX-RS that the API will only accept requests in JSON format. This means the client must send the header Content-Type: application/json with their request.

If someone tries to send data in a different format, like plain text or XML, the request is automatically rejected by the framework. The API returns a 415 Unsupported Media Type response, and your method is never executed.

This acts like a gatekeeper for your API. It ensures that only the correct data format reaches your business logic, which helps prevent errors and keeps your code clean and safe.

---

### Part 3.2 — @QueryParam vs Path for Filtering

Using a query parameter like /sensors?type=CO2 is better for filtering than using a path like /sensors/type/CO2. The path should be used to identify a specific resource, but filtering is just narrowing down a list. So using the path for filtering can be confusing, because it looks like “CO2” is a resource when it’s not.

Query parameters are also easier to use because they are optional. You can add them only when needed, and the same endpoint still works without them. They also make it simple to apply multiple filters, like /sensors?type=CO2&status=ACTIVE. This would be much harder to manage if you used the path instead.

---

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern means you don’t put all your endpoints in one big class. Instead, you split them into smaller classes based on what they do.

For example, SensorResource handles everything related to sensors. But when a request comes for /sensors/{id}/readings, it passes that request to another class called SensorReadingResource. That class then handles everything related to sensor readings.

This makes the code much easier to manage. Each class focuses on one task, so it stays small and easy to understand. It also makes testing easier because you can test each part separately instead of dealing with one large, complicated class.

---

### Part 5.2 — 422 vs 404

When a user tries to create a sensor with a roomId that doesn’t exist, the API endpoint /api/v1/sensors is still valid. So returning 404 Not Found would be confusing, because it suggests the URL itself is wrong.

Instead, 422 Unprocessable Entity is the better choice. It means the server understood the request and the JSON format is correct, but there is a problem with the data inside it.

In this case, the issue is that the roomId doesn’t match any existing room. So the request is valid in structure, but incorrect in meaning, which is exactly what 422 is meant for.

---

### Part 5.4 — Stack Trace Security Risks

If your API sends full Java stack traces in error responses, it can expose a lot of sensitive information. Attackers can see which frameworks and versions you are using and then look for known security weaknesses. They can also see your package names and class structure, which makes it easier to understand how your system is built.

Sometimes, stack traces even show variable names or query details. This can help attackers find ways to break the system, such as using injection attacks. To prevent this, the project uses a global exception mapper. It catches all errors and returns a simple 500 Internal Server Error message to the client without exposing any details. The full error is still logged on the server, so developers can see it, but it is hidden from users.

---

### Part 5.5 — Filters vs Manual Logging

If you put Logger.info() inside every resource method, you have to remember to add it each time you create a new endpoint. If you ever want to change how logs look, you would also need to update every method. This leads to a lot of repeated code and is easy to forget.

Using a JAX-RS filter is a better approach. You write the logging code once in a single class, and it automatically works for every request and response in the API, even for new endpoints added later.

It also logs requests that fail before reaching your resource methods, such as invalid requests. This makes logging more complete and reliable.
