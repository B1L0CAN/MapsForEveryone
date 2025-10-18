package com.bilocan.mapsforeveryone.service;

import android.util.Log;
import com.bilocan.mapsforeveryone.model.TransitResponse;
import com.bilocan.mapsforeveryone.model.TransitStep;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TransitServiceImpl implements TransitService {
    private static final String TAG = "TransitServiceImpl";
    private static final String API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY;

    @Override
    public TransitResponse getTransitInfo(String origin, String destination) {
        try {
            String urlString = String.format(
                "https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&mode=transit&language=tr&key=%s",
                origin, destination, API_KEY
            );

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return parseResponse(response.toString());
            } else {
                Log.e(TAG, "HTTP Error: " + responseCode);
                return createErrorResponse("HTTP Error: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting transit info", e);
            return createErrorResponse(e.getMessage());
        }
    }

    private TransitResponse parseResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            String status = json.getString("status");

            if (!status.equals("OK")) {
                return createErrorResponse("API Error: " + status);
            }

            JSONArray routes = json.getJSONArray("routes");
            if (routes.length() == 0) {
                return createErrorResponse("No routes found");
            }

            JSONObject route = routes.getJSONObject(0);
            JSONArray legs = route.getJSONArray("legs");
            if (legs.length() == 0) {
                return createErrorResponse("No legs found in route");
            }

            JSONObject leg = legs.getJSONObject(0);
            JSONArray steps = leg.getJSONArray("steps");
            List<TransitStep> transitSteps = new ArrayList<>();

            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);
                TransitStep transitStep = new TransitStep();

                transitStep.setInstruction(step.getString("html_instructions"));
                transitStep.setDuration(step.getJSONObject("duration").getString("text"));
                transitStep.setDistance(step.getJSONObject("distance").getString("text"));

                if (step.has("transit_details")) {
                    JSONObject transitDetails = step.getJSONObject("transit_details");
                    transitStep.setType("TRANSIT");
                    transitStep.setTransitLine(transitDetails.getJSONObject("line").getString("name"));
                    transitStep.setTransitVehicle(transitDetails.getJSONObject("line").getJSONObject("vehicle").getString("name"));
                    transitStep.setDepartureStop(transitDetails.getJSONObject("departure_stop").getString("name"));
                    transitStep.setArrivalStop(transitDetails.getJSONObject("arrival_stop").getString("name"));
                } else {
                    transitStep.setType("WALKING");
                }

                transitSteps.add(transitStep);
            }

            TransitResponse response = new TransitResponse();
            response.setError(false);
            response.setStatus("OK");
            response.setMessage("Route found successfully");
            response.setDetails("Google Maps API returned a valid route");
            response.setSteps(transitSteps);
            response.setTotalDuration(leg.getJSONObject("duration").getString("text"));
            response.setTotalDistance(leg.getJSONObject("distance").getString("text"));

            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            return createErrorResponse("Error parsing response: " + e.getMessage());
        }
    }

    private TransitResponse createErrorResponse(String message) {
        TransitResponse response = new TransitResponse();
        response.setError(true);
        response.setStatus("ERROR");
        response.setMessage(message);
        response.setDetails("An error occurred while processing the request");
        response.setSteps(new ArrayList<>());
        return response;
    }
} 