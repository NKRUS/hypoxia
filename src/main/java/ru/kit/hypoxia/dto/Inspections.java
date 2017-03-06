package ru.kit.hypoxia.dto;


public class Inspections extends Data{
    private int wave;
    private int spo2;
    private int pulse;
    private int signal;

    public Inspections(int wave, int spo2, int pulse, int signal) {
        this.wave = wave;
        this.spo2 = spo2;
        this.pulse = pulse;
        this.signal = signal;
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

    public int getSignal() {
        return signal;
    }

    @Override
    public String toString() {
        return "wave: " + wave + ", spo2: " + spo2 + ", pulse: " + pulse + ", signal: " + signal;
    }
}
