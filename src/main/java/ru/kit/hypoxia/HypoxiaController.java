package ru.kit.hypoxia;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import javafx.scene.paint.Color;
import org.json.JSONObject;
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

import static ru.kit.hypoxia.Util.deserializeData;
import static ru.kit.hypoxia.Util.serialize;

public class HypoxiaController {

    private int age;
    private boolean isMan;
    private int weight;
    private int height;
    private int activityLevel;
    private int systBP;
    private int diastBP;
    private String path;

    final static int port = 8085;
    private final static int firstTime = 15;
    private final static int secondTime = 315;//315;
    private int currentStage = 0;
    private int timeOfFall = 0;
    private int timeOfRecovery = 0;
    private int timeOfStartOfRecovery = 0;


    private Stack<XYChart.Data<Number, Number>> markers = new Stack<>();

    static final int secondsForTest = 600;

    private static final int NUMBER_OF_SKIP = 5;
    private static final int MAX_DATA_POINTS = 3000;//;7302 / 123 * secondsForTest / NUMBER_OF_SKIP;
    private static final int MAX_DATA_VALUES = 150;

    private static int RECOVERY_TIME_TO_SHOW_ON_SCREEN;


    private XYChart.Series<Number, Number> seriesHR;
    private XYChart.Series<Number, Number> seriesSPO2;

    private Thread checkReadyThread, updateTimerThread;

    boolean isTesting = false;
//    private ConcurrentLinkedQueue<Number> dataQHR = new ConcurrentLinkedQueue<>();
//    private ConcurrentLinkedQueue<Number> dataQSPO2 = new ConcurrentLinkedQueue<>();


//    private void setDataSPO2(Queue<Integer> dataSPO2) {
//        this.dataSPO2 = dataSPO2;
//    }
//
//    private void setDataHR(Queue<Integer> dataHR) {
//        this.dataHR = dataHR;
//    }

    private int sumOfSPO2Rest, countOfSPO2Rest, SPO2Rest;


    @FXML
    GridPane forChart;


    private LineChartWithMarker<Number, Number> chart;


    @FXML
    Label textSPO2, textHR, textNotification, textMaskaOff,
            textMaskaOn, textTimer, textRecoveryTime, textFallTime, textHypI, textSPO2Rest, textSPO2soSmall;

    @FXML
    private Button buttonStart, ok_button, cancel_button;
    private Hypoxia stage;

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

   //private Timer timer;

    @FXML
    private void exit(ActionEvent actionEvent) {
        stage.close();
    }

    @FXML
    private void startTest(ActionEvent actionEvent) {
        beforeTest();

        Thread updateSeries = new Thread(() -> {
            try (Socket socket = new Socket("localhost", port)) {


                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));


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
                    //System.err.println(line);
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
                                    SPO2Rest = sumOfSPO2Rest / countOfSPO2Rest;
                                    textSPO2Rest.setText("" + SPO2Rest);

                                    XYChart.Data<Number, Number> verticalMarker = new XYChart.Data<>(xValue, 0);
                                    markers.push(verticalMarker);
                                    chart.addVerticalValueMarker(verticalMarker);

                                    if (SPO2Rest < 93) {
                                        textSPO2soSmall.setVisible(true);
                                    }
                                });


                                textNotification.setVisible(false);
                                textMaskaOn.setVisible(true);
                                textMaskaOff.setVisible(false);


                            }

                            if (currentStage == 2 && (seconds == secondTime || inspections.getSpo2() <= (SPO2Rest - 5))) {
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
                                textMaskaOn.setVisible(false);
                                textMaskaOff.setVisible(true);
                            }


//                            if (currentStage == 3) {
//                                sumOfSPO2Recovery += inspections.getSpo2();
//                                countOfSPO2Recovery += 1;
//                            }

                            if (currentStage == 3 && (inspections.getSpo2() >= 95 || inspections.getSpo2() >= SPO2Rest)) {
                                currentStage = 4;

                                if(seconds - timeOfStartOfRecovery != 0){
                                    timeOfRecovery = seconds - timeOfStartOfRecovery;
                                }else{
                                    timeOfRecovery = 20; //TODO must fix one day, but not today
                                    RECOVERY_TIME_TO_SHOW_ON_SCREEN = timeOfRecovery;
                                }


                                System.err.println("Время восстановления: " + timeOfRecovery);

                                Platform.runLater(() -> {
                                    if(RECOVERY_TIME_TO_SHOW_ON_SCREEN==0) {
                                        textRecoveryTime.setText("" + timeOfRecovery);
                                    }else {
                                        textRecoveryTime.setText("---");
                                    }


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
        writeJSON();
        stage.close();
    }


    private void beforeTest() {
        isTesting = true;
        checkReadyThread.interrupt();


        updateTimerThread = new UpdateTimerThread(this);
        updateTimerThread.setDaemon(true);
        updateTimerThread.start();

        disableAll();
        textNotification.setVisible(false);


        prepareChart();

        sumOfSPO2Rest = 0;
        countOfSPO2Rest = 0;

        ok_button.setDisable(true);
        while (!markers.isEmpty()) {
            chart.removeVerticalValueMarker(markers.pop());
        }


        textHR.setText("---");
        textSPO2Rest.setText("---");
        textFallTime.setText("---");
        textRecoveryTime.setText("---");
        textTimer.setText("00:00");


    }

    void afterTest() {
        updateTimerThread.interrupt();
        isTesting = false;
        seconds = 0;
        System.err.println("After test");

    }

    private int seconds = 0;

    void updateTimer() {
        seconds++;
        Platform.runLater(() -> {
            int sec = seconds % 60;
            //System.err.println(seconds);
            textTimer.setText("0" + (seconds / 60) + ":" + ((sec < 10) ? "0" + sec : "" + sec));
        });

    }

    private void startChecking() {
        checkReadyThread = new CheckReadyThread(this);
        checkReadyThread.setDaemon(true);
        checkReadyThread.start();
    }

    void disableAll() {
        buttonStart.setDisable(true);
        textNotification.setVisible(true);
        textMaskaOn.setVisible(false);
        textMaskaOff.setVisible(false);
        textSPO2soSmall.setVisible(false);
    }

    void enableAll() {
        buttonStart.setDisable(false);
        textNotification.setVisible(false);
    }


    public void setStage(Hypoxia stage) {
        this.stage = stage;



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

    public int getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(int activityLevel) {
        this.activityLevel = activityLevel;
    }

    private JSONObject createJSON() {
        JSONObject obj = new JSONObject();
        double hypI = 0;
        try{
            hypI = Double.parseDouble(textHypI.getText());
        }catch (Exception ex){
            System.err.println("HypI is not a number");
        }
        obj.put("HypI", hypI);
        return obj;
    }

    private void writeJSON() {
        try {
            BufferedWriter e = new BufferedWriter(new FileWriter(new File(path.concat("hypo_output_file.json"))));
            Throwable var2 = null;

            try {
                e.write(this.createJSON().toString());
            } catch (Throwable var12) {
                var2 = var12;
                throw var12;
            } finally {
                if(var2 != null) {
                    try {
                        e.close();
                    } catch (Throwable var11) {
                        var2.addSuppressed(var11);
                    }
                } else {
                    e.close();
                }

            }
        } catch (IOException var14) {
            var14.printStackTrace();
        }

    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}


class CheckReadyThread extends Thread {

    HypoxiaController controller;

    public CheckReadyThread(HypoxiaController controller) {
        this.controller = controller;
    }

    public void run() {
        try (Socket socket = new Socket("localhost", HypoxiaController.port)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            Command command = new Launch(controller.getAge(), controller.isMan(),
                    controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                    controller.getSystBP(), controller.getDiastBP(), HypoxiaController.secondsForTest);
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
                    controller.enableAll();
                    Inspections inspections = (Inspections) deserializeData(br.readLine());
                    Platform.runLater(() -> {
                        controller.textHR.setText("" + inspections.getPulse());
                        controller.textSPO2.setText("" + inspections.getSpo2());
                    });

                } else {
                    controller.disableAll();
                    Platform.runLater(() -> {
                        controller.textHR.setText("0");
                        controller.textSPO2.setText("0");
                    });
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.err.println("Interrupt checking ready");
                }
            } while (!controller.isTesting && !isInterrupted());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}

class UpdateTimerThread extends Thread {
    private HypoxiaController controller;

    UpdateTimerThread(HypoxiaController controller) {
        this.controller = controller;
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("Interrupt timer");
                break;
            }
            controller.updateTimer();
        }
    }
}