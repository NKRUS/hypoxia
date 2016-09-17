package ru.kit.hypoxia.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LastResearch extends Data{
    Map<String, Inspection> inspections = new HashMap<>(20);
    private List<Integer> spo2;

    public void addInspection(String name, double value, double min, double max) {
        inspections.put(name, new Inspection(name, value, min, max));
    }

    public Map<String, Inspection> getInspections() {
        return inspections;
    }

    public void setInspections(Map<String, Inspection> inspections) {
        this.inspections = inspections;
    }

    public List<Integer> getSpo2() {
        return spo2;
    }

    public void setSpo2(List<Integer> spo2) {
        this.spo2 = spo2;
    }

    @Override
    public String toString() {
        return "LastResearch{" +
                "inspections=" + inspections +
                '}';
    }
}
