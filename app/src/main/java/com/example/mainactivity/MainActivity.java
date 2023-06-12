package com.example.mainactivity;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private EditText editTextDeparture;
    private EditText editTextDestination;
    private Polyline routePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        editTextDeparture = findViewById(R.id.editTextDeparture);
        editTextDestination = findViewById(R.id.editTextDestination);
        Button buttonRoute = findViewById(R.id.buttonRoute);

        buttonRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String departure = editTextDeparture.getText().toString();
                String destination = editTextDestination.getText().toString();

                if (!departure.isEmpty() && !destination.isEmpty()) {
                    addMarkers(departure, destination);
                    drawRoute(departure, destination);
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void addMarkers(String departure, String destination) {
        googleMap.clear();

        LatLng departureLatLng = getLatLngFromAddress(departure);
        LatLng destinationLatLng = getLatLngFromAddress(destination);

        if (departureLatLng != null && destinationLatLng != null) {
            googleMap.addMarker(new MarkerOptions().position(departureLatLng).title("Partida"));
            googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destino"));

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(departureLatLng);
            builder.include(destinationLatLng);
            LatLngBounds bounds = builder.build();

            int padding = 100;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            googleMap.animateCamera(cameraUpdate);
            getRouteInformation(departureLatLng, destinationLatLng);
        } else {
            Toast.makeText(MainActivity.this, "Falha ao obter as coordenadas.", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(String departure, String destination) {
        LatLng departureLatLng = getLatLngFromAddress(departure);
        LatLng destinationLatLng = getLatLngFromAddress(destination);

        if (departureLatLng != null && destinationLatLng != null) {
            DirectionsApiClient directionsApiClient = new DirectionsApiClient();
            directionsApiClient.getDirections(departureLatLng, destinationLatLng, new DirectionsApiClient.DirectionsApiCallback() {
                @Override
                public void onDirectionsReady(List<LatLng> points) {
                    if (routePolyline != null) {
                        routePolyline.remove();
                    }

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(points)
                            .width(16)
                            .color(com.google.android.libraries.places.R.color.quantum_purple);

                    routePolyline = googleMap.addPolyline(polylineOptions);
                }

                @Override
                public void onDirectionsFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, "Falha ao obter a rota: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, "Falha ao obter as coordenadas.", Toast.LENGTH_SHORT).show();
        }
    }

    private LatLng getLatLngFromAddress(String address) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocationName(address, 1);
            if (!addressList.isEmpty()) {
                Address location = addressList.get(0);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                return new LatLng(latitude, longitude);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void getRouteInformation(LatLng origin, LatLng destination) {
        DirectionsApiClient directionsApiClient = new DirectionsApiClient();
        directionsApiClient.getDirections(origin, destination, new DirectionsApiClient.DirectionsApiCallback() {
            @Override
            public void onDirectionsReady(List<LatLng> points) {
                double distance = calculateDistance(points);

                double speed = calculateSpeed(distance);

                double duration = calculateDuration(distance, speed);

                TextView textViewDistance = findViewById(R.id.textViewDistance);
                TextView textViewSpeed = findViewById(R.id.textViewSpeed);
                TextView textViewDuration = findViewById(R.id.textViewDuration);

                textViewDistance.setText("Distância: " + String.format("%.2f km", distance));
                textViewSpeed.setText("Velocidade Ideal: " + String.format("%.2f km/h", speed));
                textViewDuration.setText("Tempo Estimado: " + formatDuration(duration));
            }

            @Override
            public void onDirectionsFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, "Falha ao obter a rota: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
    }

    private double calculateDistance(List<LatLng> points) {
        double distance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            LatLng point1 = points.get(i);
            LatLng point2 = points.get(i + 1);
            distance += SphericalUtil.computeDistanceBetween(point1, point2);
        }
        return distance / 1000;
    }

    private double calculateSpeed(double distance) {
        //Implementar logica para pegar a velocidade das vias
        return 100;
    }

    private double calculateDuration(double distance, double speed) {
        return distance / speed;
    }

    private String formatDuration(double duration) {
        int hours = (int) duration;
        int minutes = (int) ((duration - hours) * 60);

        String formattedDuration = "";
        if (hours > 0) {
            formattedDuration += hours + "h ";
        }
        formattedDuration += minutes + "min";

        return formattedDuration;
    }
}
