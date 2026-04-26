package com.campus.api.resources;

import com.campus.api.application.DataStore;
import com.campus.api.exceptions.LinkedResourceNotFoundException;
import com.campus.api.models.Room;
import com.campus.api.models.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> allSensors = store.getSensors().values();

        List<Sensor> result;
        if (type != null && !type.isBlank()) {
            result = allSensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        } else {
            result = new ArrayList<>(allSensors);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", result.size());
        response.put("sensors", result);
        if (type != null) response.put("filteredByType", type);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/sensors", "GET"),
            buildLink("create", "/api/v1/sensors", "POST")
        });

        return Response.ok(response).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getName() == null || sensor.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor name is required"))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "roomId is required"))
                    .build();
        }

        // Validate referenced room exists
        if (!store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: Room with ID '" + sensor.getRoomId() + "' does not exist in the system."
            );
        }

        store.getSensors().put(sensor.getId(), sensor);

        // Keep room's sensorIds list in sync
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) room.addSensorId(sensor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor registered successfully");
        response.put("sensor", sensor);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/sensors/" + sensor.getId(), "GET"),
            buildLink("readings", "/api/v1/sensors/" + sensor.getId() + "/readings", "GET"),
            buildLink("add-reading", "/api/v1/sensors/" + sensor.getId() + "/readings", "POST"),
            buildLink("room", "/api/v1/rooms/" + sensor.getRoomId(), "GET")
        });

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(response)
                .build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                        "message", "Sensor with ID '" + sensorId + "' does not exist."))
                    .build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("sensor", sensor);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/sensors/" + sensorId, "GET"),
            buildLink("readings", "/api/v1/sensors/" + sensorId + "/readings", "GET"),
            buildLink("room", "/api/v1/rooms/" + sensor.getRoomId(), "GET")
        });
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                        "message", "Sensor with ID '" + sensorId + "' does not exist."))
                    .build();
        }

        // Remove from parent room's sensorIds list
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) room.removeSensorId(sensorId);

        store.getSensors().remove(sensorId);

        return Response.ok(Map.of(
            "message", "Sensor '" + sensor.getName() + "' deleted successfully.",
            "deletedSensorId", sensorId
        )).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        if (!store.sensorExists(sensorId)) {
            throw new NotFoundException("Sensor with ID '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
    }

    private Map<String, Object> buildLink(String rel, String href, String method) {
        Map<String, Object> link = new HashMap<>();
        link.put("rel", rel);
        link.put("href", href);
        link.put("method", method);
        return link;
    }
}
