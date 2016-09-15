package ru.kit.hypoxia;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class PulseOxiEquipment {

    private int age;
    private int systBP;
    private int diastBP;
    private boolean isMan;
    private double weight;
    private double height;

    private PulseOxi oxiLib = PulseOxi.INSTANCE;

    private Queue<Integer> dataSPO2 = new PriorityQueue<Integer>();
    private Queue<Integer> dataHR = new PriorityQueue<Integer>();

    public PulseOxiEquipment(int age, boolean isMan, double weight, double height, int systBP, int diastBP) throws Exception {
        this.age = age;
        this.isMan = isMan;
        this.weight = weight;
        this.height = height;
        this.systBP = systBP;
        this.diastBP = diastBP;


        if (!oxiLib.InitOxiLibrary()) {
            throw new Exception("Cannot init Oxi lib");
        }
    }

    public void close() {
        oxiLib.SPO2_CloseDevice();
        oxiLib.FreeOxiLibrary();
    }

    public synchronized void run(){
        
    }

    public Queue<Integer> getDataSPO2() {
        return dataSPO2;
    }

    public Queue<Integer> getDataHR() {
        return dataHR;
    }


    private interface PulseOxi extends Library {
        PulseOxi INSTANCE = (PulseOxi) Native.loadLibrary("mm_oxi_lib", PulseOxi.class);

        static class DATAPACK extends Structure {
            @Override
            protected List getFieldOrder() {
                return Arrays.asList(new String[]{"status1", "wave", "status2", "pr", "spo2"});
            }

            public static class ByReference extends DATAPACK implements Structure.ByReference {
            }

            public byte status1;
            public byte wave;
            public byte status2;
            public byte pr;
            public byte spo2;

        }

        int SPO2_GetSpeed();

        int SPO2_GetBuffer(DATAPACK datapack);

        void SPO2_CloseDevice();

        boolean SPO2_OpenDevice();

        int OXi_Calculate();

        int OXi_ValidateMeasurement();

        void OXi_InitNewMeasurement(int age, int sex, int weight, int height, int dal, int syst_bp, int diast_bp);

        boolean InitOxiLibrary();

        boolean FreeOxiLibrary();

        DATAPACK SPO2_GetValue();
    }
}
