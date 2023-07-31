package com.example.mainactivity;

public class Moto extends Veiculo {
    public Moto() {
        super("Moto");
    }

    @Override
    public double calcularVelocidadeIdeal() {
        return 80.0;
    }
}
