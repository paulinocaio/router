package com.example.mainactivity;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class JsonEncryption {

    // Método para criptografar o JSON
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encryptJson(String json, String secretKey) throws Exception {
        // Criar uma instância da classe Cipher com o algoritmo AES
        Cipher cipher = Cipher.getInstance("AES");

        // Gerar uma chave secreta a partir da chave fornecida
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Inicializar o Cipher em modo de criptografia com a chave
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        // Criptografar o JSON e retornar o resultado como uma string Base64
        byte[] encryptedBytes = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Método para descriptografar o JSON
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decryptJson(String encryptedJson, String secretKey) throws Exception {
        // Criar uma instância da classe Cipher com o algoritmo AES
        Cipher cipher = Cipher.getInstance("AES");

        // Gerar uma chave secreta a partir da chave fornecida
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

        // Inicializar o Cipher em modo de descriptografia com a chave
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

        // Decodificar a string Base64 para obter os bytes criptografados
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedJson);

        // Descriptografar os bytes criptografados e retornar o JSON original
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
