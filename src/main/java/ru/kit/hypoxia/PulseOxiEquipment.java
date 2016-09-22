package ru.kit.hypoxia;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

public class PulseOxiEquipment {

    private int age;
    private int systBP;
    private int diastBP;
    private boolean isMan;
    private int weight;
    private int height;

    private static int numberOfIteration = 11600;

    private PulseOxi oxiLib = PulseOxi.INSTANCE;

    private Queue<Integer> dataSPO2 = new ConcurrentLinkedQueue<Integer>();
    private Queue<Integer> dataHR = new ConcurrentLinkedQueue<Integer>();

    private volatile boolean isTesting = false;

    public PulseOxiEquipment(int age, boolean isMan, int weight, int height, int systBP, int diastBP) throws Exception {
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

    private void close() {
        isTesting = false;
        oxiLib.SPO2_CloseDevice();
        oxiLib.FreeOxiLibrary();
    }

    public synchronized void run(){
        final PulseOxi.DATAPACK.ByReference aPack = new PulseOxi.DATAPACK.ByReference();
        final PulseOxi.DATAPACK[] data_pack = (PulseOxi.DATAPACK[]) aPack.toArray(255);
        isTesting = true;


        oxiLib.OXi_InitNewMeasurement(age, isMan ? 1 : 0, weight, height, 2, systBP, diastBP);
        if (!oxiLib.SPO2_OpenDevice()) {
            close();
            return;
        }

        oxiLib.SPO2_GetSpeed();

        int counter = 0, n_pack, n_values = numberOfIteration, n_buffer = 0;
        int wave, spo2, pulse_rate, signal_str, probe_state;

        for (int i = 0; i < 32; i++)
            oxiLib.SPO2_GetBuffer(aPack);

        while (true){
            n_pack = oxiLib.SPO2_GetBuffer(aPack);


            counter += n_pack;
            if (counter > n_values) break;

            for (int i = 0; i < n_pack && i < 255; i++) {
                if (data_pack[i].status1 == (byte) 255 && data_pack[i].status2 == (byte) 255) {
                    break;
                }

                n_buffer++;


                if (n_buffer > 8000) break;

                // WAVE FORM
                wave = data_pack[i].wave;

                // SPO2
                spo2 = data_pack[i].spo2;

                // HR
                pulse_rate = data_pack[i].pr + 128 * ((data_pack[i].status2 >> 6) & 0x1);


                // SIGNAL STRENGTH
                signal_str = data_pack[i].status1 & 0x0f;
                if (signal_str > 10) signal_str = 0;

                // PROBESTATE
                if ((data_pack[i].status2 & 0x10) == 0) probe_state = 1;
                else probe_state = 0;


                //System.err.println("Queue size is " + dataSPO2.size());
                if(counter % 5 == 0) {
                    dataSPO2.add(spo2);
                    dataHR.add(pulse_rate);
                }
                System.err.println(String.format("i = %d wave = %d, spo2 = %d, pulse rate = %d, signal strength = %d, probe state = %d",
                        counter, wave, spo2, pulse_rate, signal_str, probe_state));
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }




        if (1 != oxiLib.OXi_ValidateMeasurement()) {
            System.err.println("OXi_ValidateMeasurement - Error");
            close();
            return;
        }


        if (1 != oxiLib.OXi_Calculate()) {
            System.err.println("OXi_Calculate - Error");
        }

        close();
    }

    public Queue<Integer> getDataSPO2() {
        return dataSPO2;
    }

    public Queue<Integer> getDataHR() {
        return dataHR;
    }

    public boolean isTesting() {
        return isTesting;
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
