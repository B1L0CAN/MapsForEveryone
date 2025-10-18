package com.bilocan.mapsforeveryone.model;

public class TransitStep {
    private String type;
    private String instruction;
    private String duration;
    private String distance;
    private String transitLine;
    private String transitVehicle;
    private String departureStop;
    private String arrivalStop;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getTransitLine() {
        return transitLine;
    }

    public void setTransitLine(String transitLine) {
        this.transitLine = transitLine;
    }

    public String getTransitVehicle() {
        return transitVehicle;
    }

    public void setTransitVehicle(String transitVehicle) {
        this.transitVehicle = transitVehicle;
    }

    public String getDepartureStop() {
        return departureStop;
    }

    public void setDepartureStop(String departureStop) {
        this.departureStop = departureStop;
    }

    public String getArrivalStop() {
        return arrivalStop;
    }

    public void setArrivalStop(String arrivalStop) {
        this.arrivalStop = arrivalStop;
    }
} 