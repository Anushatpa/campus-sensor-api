package com.campus.api.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class SensorReading {

    private final String id;
    private final String sensorId;
    private double value;          
    private String unit;
    private String notes;
    private final long timestamp;  

    public SensorReading() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.sensorId = null;
    }

    @JsonCreator
    public SensorReading(
            @JsonProperty("sensorId") String sensorId,
            @JsonProperty("value") double value,
            @JsonProperty("unit") String unit,
            @JsonProperty("notes") String notes) {
        this.id = UUID.randomUUID().toString();
        this.sensorId = sensorId;
        this.value = value;
        this.unit = unit;
        this.notes = notes;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getSensorId() { return sensorId; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public long getTimestamp() { return timestamp; }
}
