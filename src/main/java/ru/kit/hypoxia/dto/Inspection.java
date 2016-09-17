package ru.kit.hypoxia.dto;


public class Inspection extends Data{
    private String name;
    private double value;
    private double min;
    private double max;

    Inspection(String name, double value, double min, double max) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public Inspection() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "Inspection{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
