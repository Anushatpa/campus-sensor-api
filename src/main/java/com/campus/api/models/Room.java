package com.campus.api.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {

    private final String id;
    private String name;
    private String building;
    private int floor;
    private int capacity;                         
    private String description;
    private List<String> sensorIds = new ArrayList<>();  
    private final String createdAt;

    public Room() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now().toString();
    }

    @JsonCreator
    public Room(
            @JsonProperty("name") String name,
            @JsonProperty("building") String building,
            @JsonProperty("floor") int floor,
            @JsonProperty("capacity") int capacity,
            @JsonProperty("description") String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.building = building;
        this.floor = floor;
        this.capacity = capacity;
        this.description = description;
        this.createdAt = Instant.now().toString();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getSensorIds() { return sensorIds; }
    public void setSensorIds(List<String> sensorIds) { this.sensorIds = sensorIds; }
    public String getCreatedAt() { return createdAt; }

    public void addSensorId(String sensorId) {
        if (!sensorIds.contains(sensorId)) { sensorIds.add(sensorId); }
    }
    public void removeSensorId(String sensorId) { sensorIds.remove(sensorId); }
}
