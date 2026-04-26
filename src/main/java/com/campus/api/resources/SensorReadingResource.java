package com.campus.api.resources;

import com.campus.api.application.DataStore;
import com.campus.api.exceptions.SensorUnavailableException;
import com.campus.api.models.Sensor;
import com.campus.api.models.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getReadingsForSensor(sensorId);
        Sensor sensor = store.getSensors().get(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("sensorName", sensor != null ? sensor.getName() : "unknown");
        response.put("currentValue", sensor != null ? sensor.getCurrentValue() : null);
        response.put("totalReadings", readings.size());
        response.put("readings", readings);
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/sensors/" + sensorId + "/readings", "GET"),
            buildLink("add-reading", "/api/v1/sensors/" + sensorId + "/readings", "POST"),
            buildLink("sensor", "/api/v1/sensors/" + sensorId, "GET")
        });

        return Response.ok(response).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        // Sensor must exist 
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found"))
                    .build();
        }

        // Sensor in MAINTENANCE cannot accept new readings
        if (Sensor.Status.MAINTENANCE.equals(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensor.getName() + "' (ID: " + sensorId + ") is currently in MAINTENANCE mode " +
                "and cannot accept new readings. Please wait until maintenance is complete."
            );
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Reading payload is required"))
                    .build();
        }

        SensorReading newReading = new SensorReading(sensorId, reading.getValue(), reading.getUnit(), reading.getNotes());

        // Append to history
        store.addReading(sensorId, newReading);

        sensor.setCurrentValue(newReading.getValue());
        if (newReading.getUnit() != null && !newReading.getUnit().isBlank()) {
            sensor.setUnit(newReading.getUnit());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reading recorded successfully");
        response.put("reading", newReading);
        response.put("updatedSensorCurrentValue", sensor.getCurrentValue());
        response.put("links", new Map[]{
            buildLink("self", "/api/v1/sensors/" + sensorId + "/readings", "GET"),
            buildLink("sensor", "/api/v1/sensors/" + sensorId, "GET")
        });

        return Response.created(URI.create("/api/v1/sensors/" + sensorId + "/readings"))
                .entity(response)
                .build();
    }

    private Map<String, Object> buildLink(String rel, String href, String method) {
        Map<String, Object> link = new HashMap<>();
        link.put("rel", rel);
        link.put("href", href);
        link.put("method", method);
        return link;
    }
}
