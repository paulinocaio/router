package com.example.mainactivity;

public class Carro extends Veiculo {
    public Carro() {
        super("Carro");
    }

    @Override
    public double calcularVelocidadeIdeal() {
        return 28.0;
    }
}
