package com.example.mainactivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Veiculo {
    private UUID vehicleId;
    private final String tipo;
    public List<LatLng> rotaPercorrida; //Localizaçao atual na rota
    private double totalDistance;
    private double estimatedTime;
    private double idealSpeed;


    public Veiculo(String tipo) {
        this.vehicleId = UUID.randomUUID();
        this.tipo = tipo;
        this.rotaPercorrida = new ArrayList<>();
    }

    public String getTipo() {
        return tipo;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public double getEstimatedTime() {
        return estimatedTime;
    }

    public double getIdealSpeed() {
        return idealSpeed;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public void setEstimatedTime(double estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public void setIdealSpeed(double idealSpeed) {
        this.idealSpeed = idealSpeed;
    }

    public abstract double calcularVelocidadeIdeal();

    public String getInfo() {
        // Retorne as informações do veículo em uma String formatada
        return "Tipo do Veículo: " + getTipo() + "\n"
                + "Distância Total: " + getTotalDistance() + " km\n"
                + "Tempo Estimado: " + getEstimatedTime() + " HH:MM\n"
                + "Velocidade Ideal: " + getIdealSpeed() + "km/h\n";
    }
}
