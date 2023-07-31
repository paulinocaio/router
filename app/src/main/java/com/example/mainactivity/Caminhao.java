package com.example.mainactivity;

public class Caminhao extends Veiculo {
    public Caminhao() {
        super("Caminhão");
    }

    @Override
    public double calcularVelocidadeIdeal() {
        return 80.0;
    }
}
