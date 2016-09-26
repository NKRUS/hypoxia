package ru.kit.hypoxia;


import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import ru.kit.hypoxia.commands.*;
import ru.kit.hypoxia.control.LineChartWithMarker;
import ru.kit.hypoxia.dto.Data;
import ru.kit.hypoxia.dto.Inspections;
import ru.kit.hypoxia.dto.LastResearch;
import ru.kit.hypoxia.dto.ReadyStatus;

import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class HypoxiaController {


    private final static int port = 8085;
    private final static int firstTime = 15;
    private final static int secondTime = 600;//315;
    private int currentStage = 0;
    private int timeOfFall = 0;
    private int timeOfRecovery = 0;
    private int timeOfStartOfRecovery = 0;


    private Stack<XYChart.Data<Number, Number>> markers = new Stack<>();

    private static final int secondsForTest = 600;

    private static final int NUMBER_OF_SKIP = 5;
    private static final int MAX_DATA_POINTS = 3000;//;7302 / 123 * secondsForTest / NUMBER_OF_SKIP;
    private static final int MAX_DATA_VALUES = 150;


    private XYChart.Series<Number, Number> seriesHR;
    private XYChart.Series<Number, Number> seriesSPO2;


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

    private int sumOfSPO2Rest = 0;

    private int countOfSPO2Rest = 0;

    @FXML
    GridPane forChart;


    private LineChartWithMarker<Number, Number> chart;


    @FXML
    private Label textSPO2, textHR, textNotification, textMaskaOff,
            textMaskaOn, textTimer, textRecoveryTime, textFallTime, textHypI, textSPO2Rest;

    @FXML
    private Button buttonStart, ok_button, cancel_button;
    private HypoxiaStage stage;

    @FXML
    private void initialize() {
        chart = new LineChartWithMarker<>(new NumberAxis(), new NumberAxis());


        chart.getStylesheets().add("ru/kit/hypoxia/css/linechart.css");

        forChart.getChildren().add(chart);

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
        yAxis.setTickLabelFill(Color.WHITE);


        chart.setVerticalGridLinesVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setLegendVisible(false);

        prepareChart();

        disableAll();
        startChecking();


//        XYChart.Data<Number, Number> verticalMarker = new XYChart.Data<>(50, 0);
//        markers.push(verticalMarker);
//        chart.addVerticalValueMarker(verticalMarker);
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

    private Timer timer;

    private TimerTask task;

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
                //while (line == null || data instanceof Inspections) {
                while (data instanceof Inspections) {
                    System.err.println(line);
                    if (line != null) {
                        Inspections inspections = (Inspections) data;

                        if (++counterInspections % NUMBER_OF_SKIP == 0) {
                            seriesHR.getData().add(new XYChart.Data(counterPoints, inspections.getPulse()));
                            seriesSPO2.getData().add(new XYChart.Data(counterPoints++, inspections.getSpo2()));

                            double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
                            if (counterPoints > 0.8 * upperBound) {
                                ((NumberAxis) chart.getXAxis()).setUpperBound(upperBound * 1.33);
                            }
                            if (seconds == 1) {
                                currentStage = 1;
                            }

                            if (seconds <= firstTime) {
                                sumOfSPO2Rest += inspections.getSpo2();
                                countOfSPO2Rest += 1;
                            }

                            if (currentStage == 1 && seconds == firstTime + 1) {
                                currentStage = 2;

                                final int xValue = counterPoints - 1;
                                Platform.runLater(() -> {
                                    textSPO2Rest.setText("" + sumOfSPO2Rest / countOfSPO2Rest);

                                    XYChart.Data<Number, Number> verticalMarker = new XYChart.Data<>(xValue, 0);
                                    markers.push(verticalMarker);
                                    chart.addVerticalValueMarker(verticalMarker);
                                });


                                textNotification.setVisible(false);
                                textMaskaOn.setVisible(true);
                                textMaskaOff.setVisible(false);
                            }

                            if (currentStage == 2 && (seconds == secondTime || inspections.getSpo2() <= 80)) {
                                currentStage = 3;

                                timeOfFall = seconds - firstTime;
                                System.err.println("Время падения: " + timeOfFall);
                                timeOfStartOfRecovery = seconds;


                                final int xValue = counterPoints - 1;
                                Platform.runLater(() -> {
                                    textFallTime.setText("" + timeOfFall);


                                    XYChart.Data<Number, Number> verticalMarker = new XYChart.Data<>(xValue, 0);
                                    markers.push(verticalMarker);
                                    chart.addVerticalValueMarker(verticalMarker);
                                });

                                textNotification.setVisible(false);
                                textMaskaOn.setVisible(true);
                                textMaskaOff.setVisible(false);
                            }


//                            if (currentStage == 3) {
//                                sumOfSPO2Recovery += inspections.getSpo2();
//                                countOfSPO2Recovery += 1;
//                            }

                            if (currentStage == 3 && inspections.getSpo2() >= 95) {
                                currentStage = 4;

                                timeOfRecovery = seconds - timeOfStartOfRecovery;

                                System.err.println("Время восстановления: " + timeOfRecovery);

                                Platform.runLater(() -> {
                                    textRecoveryTime.setText("" + timeOfRecovery);

                                    DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
                                    otherSymbols.setDecimalSeparator('.');
                                    textHypI.setText(new DecimalFormat("#.##", otherSymbols).format(timeOfFall * 1.0 / timeOfRecovery));
                                    ok_button.setDisable(false);
                                });

                                stopTest();

                            }

                            Platform.runLater(() -> {
                                textHR.setText("" + inspections.getPulse());
                                textSPO2.setText("" + inspections.getSpo2());
                            });

                        }


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

    private void stopTest() {
        Thread stopTest = new Thread(() -> {
            try (Socket socket = new Socket("localhost", port)) {
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                StopTest stopTestCommand = new StopTest();
                output.write(serialize(stopTestCommand));
                output.newLine();
                output.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        stopTest.setDaemon(true);
        stopTest.start();
    }

    @FXML
    private void cancel(ActionEvent actionEvent) {
        stage.close();
    }

    @FXML
    private void ok(ActionEvent actionEvent) {
        stage.close();
    }


    private void beforeTest() {
        timer = new Timer();

        task = new TimerTask() {
            public void run() {

                System.err.println("IN timer");
                while (isTesting)
                    updateTimer();
            }

        };


        disableAll();
        textNotification.setVisible(false);

        timer.schedule(task, 0, 1000);
        prepareChart();

        sumOfSPO2Rest = 0;
        countOfSPO2Rest = 0;

        ok_button.setDisable(true);
        while (!markers.isEmpty()) {
            chart.removeVerticalValueMarker(markers.pop());
        }
    }

    public void afterTest() {
        isTesting = false;
        seconds = 0;
        System.err.println("After test");
        timer.cancel();
        timer.purge();
    }

    private int seconds = 0;

    private void updateTimer() {
        Platform.runLater(() -> {
            seconds++;
            int sec = seconds % 60;
            //System.err.println(seconds);
            textTimer.setText("0" + (seconds / 60) + ":" + ((sec < 10) ? "0" + sec : "" + sec));
        });

    }

    private void startChecking() {
        Thread checkReady = new Thread(() -> {
            try (Socket socket = new Socket("localhost", port)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Command command = new Launch(20, true, 70, 170, 120, 80, secondsForTest);
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
                    //System.err.println("Status ready: " + readyStatus.isPulse());

                    if (isReady) {
                        enableAll();
                        Inspections inspections = (Inspections) deserializeData(br.readLine());
                        Platform.runLater(() -> {
                            textHR.setText("" + inspections.getPulse());
                            textSPO2.setText("" + inspections.getSpo2());
                        });

                    } else {
                        disableAll();
                        Platform.runLater(() -> {
                            textHR.setText("0");
                            textSPO2.setText("0");
                        });
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
        textMaskaOn.setVisible(false);
        textMaskaOff.setVisible(false);
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


    public void setStage(HypoxiaStage stage) {
        this.stage = stage;

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                //afterTest();
            }
        });

    }
}
