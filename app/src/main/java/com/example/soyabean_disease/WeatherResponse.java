package com.example.soyabean_disease;

public class WeatherResponse {
    public Main main;
    public String name;

    public static class Main {
        public float temp;
    }
}
