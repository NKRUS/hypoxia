package ru.kit.hypoxia.dto;


public class Inspections extends Data{
    private int wave;
    private int spo2;
    private int pulse;

    public Inspections(int wave, int spo2, int pulse) {
        this.wave = wave;
        this.spo2 = spo2;
        this.pulse = pulse;
    }

    public Inspections(){}

    public int getPulse() {
        return pulse;
    }

    public int getSpo2() {
        return spo2;
    }

    public int getWave() {
        return wave;
    }

    @Override
    public String toString() {
        return "wave: " + wave + ", spo2: " + spo2 + ", pulse: " + pulse;
    }
}
