package com.campus.api.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class Sensor {

    public enum Status {
        ACTIVE, INACTIVE, MAINTENANCE
    }

    private final String id;
    private String name;
    private String type;        
    private String roomId;
    private Status status;
    private double currentValue;
    private String unit;
    private final String createdAt;

    public Sensor() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now().toString();
        this.status = Status.ACTIVE;
    }

    @JsonCreator
    public Sensor(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("roomId") String roomId,
            @JsonProperty("status") Status status,
            @JsonProperty("unit") String unit) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.type = type;
        this.roomId = roomId;
        this.status = (status != null) ? status : Status.ACTIVE;
        this.unit = unit;
        this.currentValue = 0.0;
        this.createdAt = Instant.now().toString();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getCreatedAt() { return createdAt; }
}
