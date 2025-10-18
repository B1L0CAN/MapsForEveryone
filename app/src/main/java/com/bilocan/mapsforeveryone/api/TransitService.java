package com.bilocan.mapsforeveryone.api;

import com.bilocan.mapsforeveryone.api.model.TransitResponse;
 
public interface TransitService {
    TransitResponse getTransitInfo(String origin, String destination);
} 