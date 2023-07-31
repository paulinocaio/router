package com.example.mainactivity;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DirectionsApiClient {
    private static final String TAG = DirectionsApiClient.class.getSimpleName();

    public interface DirectionsApiCallback {
        void onDirectionsReady(List<LatLng> points);

        void onDirectionsFailure(String errorMessage);
    }

    public void getDirections(LatLng origin, LatLng destination, DirectionsApiCallback callback) {
        String apiUrl = buildDirectionsApiUrl(origin, destination);

        new DirectionsAsyncTask(callback).execute(apiUrl);
    }

    private String buildDirectionsApiUrl(LatLng origin, LatLng destination) {
        String apiKey = "AIzaSyDvgG9_ldNWvY0Or4e3Iy4WQUd_AWIrV5c";

        String baseUrl = "https://maps.googleapis.com/maps/api/directions/json?";
        String originParam = "origin=" + origin.latitude + "," + origin.longitude;
        String destinationParam = "destination=" + destination.latitude + "," + destination.longitude;
        String keyParam = "key=" + apiKey;

        return baseUrl + originParam + "&" + destinationParam + "&" + keyParam;
    }

    private static class DirectionsAsyncTask extends AsyncTask<String, Void, String> {
        private final DirectionsApiCallback callback;

        DirectionsAsyncTask(DirectionsApiCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = connection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                return stringBuilder.toString();
            } catch (IOException e) {
                Log.e(TAG, "Error retrieving directions API response: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status");

                    if (status.equals("OK")) {
                        JSONArray routesArray = jsonObject.getJSONArray("routes");
                        JSONObject routeObject = routesArray.getJSONObject(0);
                        JSONObject polylineObject = routeObject.getJSONObject("overview_polyline");
                        String encodedPolyline = polylineObject.getString("points");
                        List<LatLng> points = PolyUtil.decode(encodedPolyline);
                        callback.onDirectionsReady(points);
                    } else {
                        String errorMessage = jsonObject.getString("error_message");
                        callback.onDirectionsFailure(errorMessage);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing directions API response: " + e.getMessage());
                    callback.onDirectionsFailure("Erro ao analisar a resposta da API de direções.");
                }
            } else {
                callback.onDirectionsFailure("Erro ao obter resposta da API de direções.");
            }
        }
    }
}

