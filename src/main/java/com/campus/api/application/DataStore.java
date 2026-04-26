package com.campus.api.application;

import com.campus.api.models.Room;
import com.campus.api.models.Sensor;
import com.campus.api.models.SensorReading;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Thread-safe maps for all data
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return sensorReadings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        getReadingsForSensor(sensorId).add(reading);
    }

    public boolean roomExists(String roomId) {
        return rooms.containsKey(roomId);
    }

    public boolean sensorExists(String sensorId) {
        return sensors.containsKey(sensorId);
    }

    public boolean roomHasSensors(String roomId) {
        return sensors.values().stream()
                .anyMatch(s -> roomId.equals(s.getRoomId()));
    }
}
