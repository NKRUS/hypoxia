package ru.kit.hypoxia;


import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import ru.kit.hypoxia.commands.*;
import ru.kit.hypoxia.dto.Data;
import ru.kit.hypoxia.dto.Inspections;
import ru.kit.hypoxia.dto.ReadyStatus;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HypoxiaController {

    private Queue<Integer> dataSPO2;
    private Queue<Integer> dataHR;


    private static final int MAX_DATA_POINTS = 7302 / 5;
    private static final int MAX_DATA_VALUES = 150;
    private int xSeriesData = 0;
    private XYChart.Series<Number, Number> seriesHR = new XYChart.Series<>();
    private XYChart.Series<Number, Number> seriesSPO2 = new XYChart.Series<>();


    private PulseOxiEquipment pulseOxiEquipment;
//    private ConcurrentLinkedQueue<Number> dataQHR = new ConcurrentLinkedQueue<>();
//    private ConcurrentLinkedQueue<Number> dataQSPO2 = new ConcurrentLinkedQueue<>();


    private void setDataSPO2(Queue<Integer> dataSPO2) {
        this.dataSPO2 = dataSPO2;
    }

    private void setDataHR(Queue<Integer> dataHR) {
        this.dataHR = dataHR;
    }

    @FXML
    private LineChart<Number, Number> chart;

    @FXML
    private void initialize() {
        NumberAxis xAxis = (NumberAxis) chart.getXAxis();

        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(MAX_DATA_POINTS);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);

        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(MAX_DATA_VALUES);
        yAxis.setAutoRanging(false);
        yAxis.setTickUnit(10);

        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);


        // Set Name for Series
        seriesHR.setName("Heart rate");
        seriesSPO2.setName("SPO2");

        // Add Chart Series
        chart.getData().addAll(seriesHR, seriesSPO2);


    }


    @FXML
    private void startTest(ActionEvent actionEvent) {
//        try {
//            pulseOxiEquipment = new PulseOxiEquipment(20, true, 70, 170, 120, 80);
//            this.setDataHR(pulseOxiEquipment.getDataHR());
//            this.setDataSPO2(pulseOxiEquipment.getDataSPO2());
//            Thread oxiThread = new Thread(pulseOxiEquipment::run);
//            oxiThread.setDaemon(true);
//            oxiThread.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        Thread updateSeries = new Thread(() -> {
//            while (true) {
//                while (!dataSPO2.isEmpty() && !dataHR.isEmpty()) {
//                    seriesHR.getData().add(new XYChart.Data<>(xSeriesData, dataHR.remove()));
//                    seriesSPO2.getData().add(new XYChart.Data<>(xSeriesData++, dataSPO2.remove()));
//                }
//            }


            try (Socket socket = new Socket("localhost", 8085)) {

//            Thread.sleep(1000);

                //OutputStream outputStream = socket.getOutputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Command command = new Launch(20, true, 70, 170, 120, 80);
                output.write(serialize(command));
                output.newLine();
                output.flush();
                System.err.println(command);

                boolean isReady = false;
                do {
                    CheckStatus checkStatus = new CheckStatus();
                    output.write(serialize(checkStatus));
                    output.newLine();
                    output.flush();

                    ReadyStatus readyStatus = (ReadyStatus) deserializeData(br.readLine());
                    isReady = readyStatus.isPulse();
                    System.err.println(readyStatus.isPulse());

                } while (!isReady);

                StartTest startTest = new StartTest();
                output.write(serialize(startTest));
                output.newLine();
                output.flush();

                System.err.println("Test started");
//                command = new GetLastInspections();
//                output.write(serialize(command));
//                output.newLine();
//                output.flush();

                String line = br.readLine();
                Data data = null;
                if (line != null) {
                    data = deserializeData(line);
                }

                System.err.println("Loop start");
                System.err.println(data);
                System.err.println(line);
                System.err.println(data instanceof Inspections);
                System.err.println(data.getClass());
                while (line == null || data instanceof Inspections) {
//                    command = new GetLastInspections();
//                    output.write(serialize(command));
//                    output.newLine();
//                    output.flush();
                    Inspections inspections = (Inspections) data;
                    System.err.println(inspections);
                    System.err.println(data);

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    line = br.readLine();
                    if (line != null) {
                        data = deserializeData(line);
                    }
                }
                System.err.println("Loop end");

            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        updateSeries.setDaemon(true);
        updateSeries.start();
    }

    private Data deserializeData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Data.class);
    }

    private Command deserializeCommand(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Command.class);
    }

    private String serialize(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }
}
