package com.example.mainactivity;

import static com.example.mainactivity.JsonEncryption.encryptJson;

import android.os.Build;
import android.os.Handler;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationThread extends Thread {
    private List<LatLng> routePoints;
    private GoogleMap googleMap;
    private Marker vehicleMarker;
    private Handler handler = new Handler();
    private double speed;
    private int currentPosition = 0;
    private final Veiculo currentVeiculo;

    public LocationThread(List<LatLng> routePoints, GoogleMap googleMap, Veiculo currentVeiculo) {
        this.routePoints = routePoints;
        this.googleMap = googleMap;
        this.currentVeiculo = currentVeiculo;

        this.speed = currentVeiculo.calcularVelocidadeIdeal();
    }

    @Override
    public void run() {
        simulateMovement();
    }

    private void simulateMovement() {
        if (routePoints.isEmpty()) {
            return;
        }

        // Calcular o atraso entre as atualizações de posição com base na velocidade do veículo
        double distance = calculateTotalDistance();
        currentVeiculo.setTotalDistance(distance);
        double estimatedTime = distance / (speed * 3.6);
        currentVeiculo.setEstimatedTime(estimatedTime);

        // Calcular o atraso proporcional ao tempo total da rota
        long totalTimeInMillis = (long) (estimatedTime * 36000); // Converter horas para milissegundos
        int routeSize = routePoints.size();
        long delayMillis = totalTimeInMillis / routeSize;

        // Restaurar a posição atual para o início da rota
        currentPosition = 0;

        // Utilizar um Handler para atualizar a posição do veículo em intervalos regulares
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Reconciliação de Dados
                ArrayList<Double> reconciliationTimeArray = calculateTimeArray();
                ArrayList<Double> reconciliationDistanceArray = calculateDistanceArray();
                ArrayList<Double> standardDeviation = new ArrayList<>();
                ArrayList<Integer> matrizDeIncidencia = new ArrayList<Integer>();

                double[] y = new double[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    y = reconciliationTimeArray.stream().mapToDouble(Double::doubleValue).toArray();
                }

                for(double value : y){
                    standardDeviation.add(0.00000001);
                    matrizDeIncidencia.add(1);
                }
                matrizDeIncidencia.set(0,-1);

                double[] v = new double[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    v = standardDeviation.stream().mapToDouble(Double::doubleValue).toArray();
                }
                double[] A = new double[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    A = matrizDeIncidencia.stream().mapToDouble(Integer::intValue).toArray();
                }

                Reconciliation rec = new Reconciliation();
                rec.reconcile(y,v,A); //chamada da reconciliação
                currentVeiculo.setIdealSpeed(getIdealSpeed(rec.getReconciledFlow(),reconciliationDistanceArray));

                try {
                    sendJson(); //chamada para enviar JSON
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Verificar se o veículo chegou ao destino
                if (currentPosition >= routePoints.size()) {
                    // O veículo chegou ao destino
                    vehicleMarker.remove();
                    return;
                }

                // Obter o próximo ponto da rota
                LatLng nextPosition = routePoints.get(currentPosition);

                // Atualizar a posição do veículo no mapa
                updateVehiclePosition(nextPosition.latitude, nextPosition.longitude);

                // Atualizar a posição atual na rota
                currentPosition++;

                // Adicionar a posição atual do veículo à lista de rota percorrida
                currentVeiculo.rotaPercorrida.add(nextPosition);

                // Agendar a próxima atualização de posição
                handler.postDelayed(this, delayMillis);
            }
        }, delayMillis);
    }

    private void updateVehiclePosition(double latitude, double longitude) {
        // Remover o marcador anterior do veículo, se existir
        if (vehicleMarker != null) {
            vehicleMarker.setPosition(new LatLng(latitude,longitude));
        }
        else {
            // Cor do marcador do veículo
            float hue = BitmapDescriptorFactory.HUE_BLUE;

            // Posição atual do veículo
            LatLng currentPosition = new LatLng(latitude, longitude);

            // Criar o marcador do veículo com a cor especificada
            vehicleMarker = googleMap.addMarker(new MarkerOptions()
                    .position(currentPosition)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
                    );
        }
    }

    private double calculateDistance(LatLng startLatLng, LatLng destinationLatLng) {
        // Raio da Terra em metros
        double earthRadius = 6371 * 1000;

        // Converter as latitudes e longitudes de graus para radianos
        double lat1Rad = Math.toRadians(startLatLng.latitude);
        double lon1Rad = Math.toRadians(startLatLng.longitude);
        double lat2Rad = Math.toRadians(destinationLatLng.latitude);
        double lon2Rad = Math.toRadians(destinationLatLng.longitude);

        // Diferenças entre as latitudes e longitudes
        double latDiff = lat2Rad - lat1Rad;
        double lonDiff = lon2Rad - lon1Rad;

        // Cálculo do haversine
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        return distance;
    }

    private double calculateTotalDistance() {
        double totalDistance = 0.0;
        for (int i = 0; i < routePoints.size() - 1; i++) {
            totalDistance += calculateDistance(routePoints.get(i), routePoints.get(i + 1));
        }
        return totalDistance/1000;
    }

    private ArrayList<Double> calculateTimeArray() {
        ArrayList<Double> TimeArray = new ArrayList<>();
        double pointsDistance = 0.0;

        for (int i = 0; i < routePoints.size() - 1; i++) {
            pointsDistance += calculateDistance(routePoints.get(currentPosition), routePoints.get(i + 1));
            pointsDistance = pointsDistance/1000; // m para km
            pointsDistance = pointsDistance/80; //km/h para hora
            pointsDistance = pointsDistance * 60;
            TimeArray.add(pointsDistance);
        }
        return TimeArray;
    }

    public double getIdealSpeed(double[] Array,ArrayList<Double> distance) {
        double time = Array[0]/60;
        double distancia = distance.get(0)/1000;

        return distancia/time;
    }

    private ArrayList<Double> calculateDistanceArray() {
        double totalDistance = 0.0;
        ArrayList<Double> distance = new ArrayList<>();
        for (int i = 0; i < routePoints.size() - 1; i++) {
            totalDistance += calculateDistance(routePoints.get(currentPosition), routePoints.get(i + 1));
            distance.add(totalDistance);
        }
        return distance;
    }

    public void sendJson() throws Exception {
        Gson gson = new Gson();

        // Cria um mapa para armazenar os dados
        Map<String, Object> data = new HashMap<>();


        // Adicione os dados ao mapa
        data.put("speed", currentVeiculo.getIdealSpeed());
        data.put("id", currentVeiculo.getVehicleId());

        // Converte o mapa para JSON
        String json = gson.toJson(data);
        String chave = "minhachave123";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            encryptJson(json,chave);
        }

        //ADICIONAR JSON AO SERVIDOR
    }
}
