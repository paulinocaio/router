package com.example.mainactivity;

import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private List<LatLng> routePoints;
    private MapView mapView;
    private GoogleMap googleMap;
    private EditText editTextDeparture;
    private EditText editTextDestination;
    private Polyline routePolyline;
    private Veiculo currentVeiculo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        editTextDeparture = findViewById(R.id.editTextDeparture);
        editTextDestination = findViewById(R.id.editTextDestination);

        currentVeiculo = new Carro();

        editTextDeparture.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    String departure = editTextDeparture.getText().toString();
                    addMarkers(departure, "");
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        editTextDestination.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    String destination = editTextDestination.getText().toString();
                    addMarkers("", destination);
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        editTextDeparture.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editTextDeparture.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String departure = editTextDeparture.getText().toString().trim();
                    addMarkers(departure, "");
                    editTextDeparture.clearFocus();
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        editTextDestination.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editTextDestination.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String destination = editTextDestination.getText().toString().trim();
                    addMarkers("", destination);
                    editTextDestination.clearFocus();
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        Button buttonRoute = findViewById(R.id.buttonRoute);
        buttonRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String departure = editTextDeparture.getText().toString();
                String destination = editTextDestination.getText().toString();

                if (!departure.isEmpty() && !destination.isEmpty()) {
                    startSimulation(departure, destination);
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button buttonChangeVeiculo = findViewById(R.id.buttonMudarVeiculo);
        buttonChangeVeiculo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeVeiculo();
            }
        });

        Button buttonGetInfo = findViewById(R.id.buttonGetInfo);
        buttonGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVehicleInfo(currentVeiculo);
            }
        });
    }

    private void startSimulation(String departure, String destination) {
        addMarkers(departure, destination);
        drawRoute(departure, destination);
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

            int padding = 60;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            googleMap.animateCamera(cameraUpdate);
        } else {
            Toast.makeText(MainActivity.this, "Falha ao obter as coordenadas.", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRoute(String departure, String destination) {
        routePoints = new ArrayList<>();
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

                    // Atualizar a lista routePoints com os pontos da rota
                    routePoints.addAll(points);

                    LocationThread locationThread = new LocationThread(routePoints, googleMap, currentVeiculo);
                    locationThread.start();
                }

                @Override
                public void onDirectionsFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, "Falha ao obter a rota: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }

        else {
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


    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editTextDeparture.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(editTextDestination.getWindowToken(), 0);
        }
    }

    private void changeVeiculo() {
        // Exibir um diálogo de seleção do veículo para o usuário
        // O usuário pode selecionar entre as opções "Moto", "Carro" e "Caminhão"

        // Exemplo de implementação básica do diálogo de seleção de veículo:
        String[] veiculos = {"Moto", "Carro", "Caminhão"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecione o veículo");
        builder.setItems(veiculos, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        currentVeiculo = new Moto();
                        break;
                    case 1:
                        currentVeiculo = new Carro();
                        break;
                    case 2:
                        currentVeiculo = new Caminhao();
                        break;
                }
            }
        });
        builder.show();
    }

    private void showVehicleInfo(Veiculo veiculo) {
        // Exemplo de implementação básica do diálogo de seleção de veículo:
        String[] infoVehicle = {veiculo.getInfo()};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Informacoes do Veiculo");
        builder.setItems(infoVehicle, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }
}