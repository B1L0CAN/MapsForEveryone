package com.bilocan.mapsforeveryone.service;

import com.bilocan.mapsforeveryone.model.TransitResponse;

public interface TransitService {
    TransitResponse getTransitInfo(String origin, String destination);
} 