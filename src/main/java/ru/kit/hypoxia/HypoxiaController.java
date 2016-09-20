package ru.kit.hypoxia;


import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ru.kit.hypoxia.commands.*;
import ru.kit.hypoxia.dto.Data;
import ru.kit.hypoxia.dto.Inspections;
import ru.kit.hypoxia.dto.LastResearch;
import ru.kit.hypoxia.dto.ReadyStatus;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HypoxiaController {

//    private Queue<Integer> dataSPO2;
//    private Queue<Integer> dataHR;

    private final static int port = 8085;

    private static final int NUMBER_OF_SKIP = 5;
    private static final int MAX_DATA_POINTS = 7302 / NUMBER_OF_SKIP;
    private static final int MAX_DATA_VALUES = 150;
    //    private int xSeriesData = 0;
    private XYChart.Series<Number, Number> seriesHR;
    private XYChart.Series<Number, Number> seriesSPO2;


    private PulseOxiEquipment pulseOxiEquipment;

    private boolean isTesting = false;
//    private ConcurrentLinkedQueue<Number> dataQHR = new ConcurrentLinkedQueue<>();
//    private ConcurrentLinkedQueue<Number> dataQSPO2 = new ConcurrentLinkedQueue<>();


//    private void setDataSPO2(Queue<Integer> dataSPO2) {
//        this.dataSPO2 = dataSPO2;
//    }
//
//    private void setDataHR(Queue<Integer> dataHR) {
//        this.dataHR = dataHR;
//    }

    @FXML
    private LineChart<Number, Number> chart;

    @FXML
    private Label textSPO2, textHR, textNotification, textTimer;

    @FXML
    private Button buttonStart;

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


        prepareChart();

        disableAll();
        startChecking();
    }


    private void prepareChart() {
        chart.getData().clear();

        seriesHR = new XYChart.Series<>();
        seriesSPO2 = new XYChart.Series<>();

        // Set Name for Series
        seriesHR.setName("Heart rate");
        seriesSPO2.setName("SPO2");


        // Add Chart Series
        chart.getData().addAll(seriesHR, seriesSPO2);
    }

    Timer timer;

    TimerTask task;

    @FXML
    private void startTest(ActionEvent actionEvent) {
        beforeTest();

        Thread updateSeries = new Thread(() -> {
            try (Socket socket = new Socket("localhost", port)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                isTesting = true;
                StartTest startTest = new StartTest();
                output.write(serialize(startTest));
                output.newLine();
                output.flush();

                System.err.println("Test started");

                String line = br.readLine();
                Data data = null;
                if (line != null) {
                    data = deserializeData(line);
                }


                int counterPoints = 0, counterInspections = 0;
                while (line == null || data instanceof Inspections) {
                    if (line != null) {
                        Inspections inspections = (Inspections) data;

                        if (++counterInspections % NUMBER_OF_SKIP == 0) {
                            seriesHR.getData().add(new XYChart.Data(counterPoints, inspections.getPulse()));
                            seriesSPO2.getData().add(new XYChart.Data(counterPoints++, inspections.getSpo2()));

                            Platform.runLater(() -> {
                                textHR.setText("" + inspections.getPulse());
                                textSPO2.setText("" + inspections.getSpo2());
                            });

                        }


                        System.err.println(inspections);

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    line = br.readLine();
                    if (line != null) {
                        data = deserializeData(line);
                    }
                }


                if (data instanceof LastResearch) {
                    LastResearch lastResearch = (LastResearch) data;
                    System.err.println(lastResearch);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            afterTest();

            startChecking();
        });
        updateSeries.setDaemon(true);
        updateSeries.start();
    }

    private void beforeTest() {
        timer = new Timer();

        task = new TimerTask() {
            public void run() {
                updateTimer();
            }

        };

        disableAll();
        textNotification.setVisible(false);
        timer.schedule(task, 0, 1000);
        prepareChart();
    }

    private void afterTest() {
        isTesting = false;
        seconds = 0;
        timer.cancel();
        timer.purge();
    }

    private int seconds = 0;

    private void updateTimer() {
        Platform.runLater(() -> {
            seconds++;
            int sec =  seconds % 60;
            textTimer.setText("0" + (seconds / 60) + ":" + ((sec < 10) ? "0" + sec : "" + sec));
        });
    }

    private void startChecking() {
        Thread checkReady = new Thread(() -> {
            try (Socket socket = new Socket("localhost", port)) {
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
                    System.err.println("Status ready: " + readyStatus.isPulse());

                    if (isReady) {
                        enableAll();
                    } else {
                        disableAll();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!isTesting);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });


        checkReady.setDaemon(true);
        checkReady.start();
    }

    private void disableAll() {
        buttonStart.setDisable(true);
        textNotification.setVisible(true);
    }

    private void enableAll() {
        buttonStart.setDisable(false);
        textNotification.setVisible(false);
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
