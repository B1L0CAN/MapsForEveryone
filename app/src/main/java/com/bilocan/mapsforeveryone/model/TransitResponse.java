package com.bilocan.mapsforeveryone.model;

import java.util.List;

public class TransitResponse {
    private boolean error;
    private String status;
    private String message;
    private String details;
    private List<TransitStep> steps;
    private String totalDuration;
    private String totalDistance;

    // Getters and Setters
    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<TransitStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TransitStep> steps) {
        this.steps = steps;
    }

    public String getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(String totalDuration) {
        this.totalDuration = totalDuration;
    }

    public String getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(String totalDistance) {
        this.totalDistance = totalDistance;
    }
} 