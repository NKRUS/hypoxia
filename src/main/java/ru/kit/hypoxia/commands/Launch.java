package ru.kit.hypoxia.commands;

public class Launch extends Command{
    private int age;
    private boolean isMan;
    private int weight;
    private int height;
    private int systBP;
    private int diastBP;
    private int seconds;


    public Launch() {
    }


    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }


    public Launch(int age, boolean isMan, int weight, int height, int systBP, int diastBP, int seconds) {
        this.age = age;
        this.isMan = isMan;
        this.weight = weight;
        this.height = height;
        this.systBP = systBP;
        this.diastBP = diastBP;
        this.seconds = seconds;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isMan() {
        return isMan;
    }

    public void setMan(boolean man) {
        this.isMan = isMan;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDiastBP() {
        return diastBP;
    }

    public void setDiastBP(int diastBP) {
        this.diastBP = diastBP;
    }

    public int getSystBP() {
        return systBP;
    }

    public void setSystBP(int systBP) {
        this.systBP = systBP;
    }
}
