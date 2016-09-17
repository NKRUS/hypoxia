package ru.kit.hypoxia.dto;

public class ReadyStatus extends Data{

    private boolean pulse;

    public ReadyStatus(boolean pulse) {
        this.pulse = pulse;
    }

    public ReadyStatus() {
    }


    public boolean isPulse() {
        return pulse;
    }


    public void setPulse(boolean pulse) {
        this.pulse = pulse;
    }

    @Override
    public String toString() {
        return "Pulse: " + isPulse();
    }
}
