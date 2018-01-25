package ru.kit.hypoxia;


import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import javafx.scene.paint.Color;
import org.json.JSONObject;
import ru.kit.SoundManager;
import ru.kit.SoundManagerSingleton;
import ru.kit.hypoxia.commands.*;
import ru.kit.hypoxia.control.LineChartWithMarker;
import ru.kit.hypoxia.dto.Data;
import ru.kit.hypoxia.dto.Inspections;
import ru.kit.hypoxia.dto.LastResearch;
import ru.kit.hypoxia.dto.ReadyStatus;
import ru.kit.hypoxia.service.Sounds;

import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static ru.kit.hypoxia.Util.deserializeData;
import static ru.kit.hypoxia.Util.serialize;

public class HypoxiaController {

    private boolean constraints;
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

    private XYChart.Series<Number, Number> seriesHR;
    private XYChart.Series<Number, Number> seriesSPO2;

    private Thread checkReadyThread, updateTimerThread, stopTest;

    private static Service<Void> updateSeriesSevice;

    boolean isTesting = false;

    private int SPO2Rest, smallestSPO2;

    @FXML
    GridPane forChart;


    private LineChartWithMarker<Number, Number> chart;


    @FXML
    Label textSPO2, textHR, textSignal, textNotification, textMaskaOff,
            textMaskaOn, textTimer, textRecoveryTime, textFallTime, textHypI, textSPO2Rest, textSPO2soSmall;

    @FXML
    private Button buttonStart, ok_button, cancel_button;
    private Hypoxia stage;

    @FXML
    private AnchorPane badEndScreen;

    volatile boolean isStageClosed = false;




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

        Hypoxia.soundManager.disposeAllSounds();
        Hypoxia.soundManager.playSound(Sounds.HYP_MALE_TEST_STARTED, SoundManager.SoundType.VOICE);
        Hypoxia.soundManager.pushSoundToTrackQueueWithDelay(Sounds.HYP_FEMALE_HOLD_MASK_IN_HANDS, SoundManager.SoundType.VOICE, 5000);

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

    void closeConnections() {
        System.out.println("Closing sounds");
        Hypoxia.soundManager.disposeAllSounds();
        isStageClosed = true;
        closeSocketConnection();

    }

    private void closeSocketConnection() {
        System.out.println("Starting closing connections");
        if (checkReadyThread != null) checkReadyThread.interrupt();
        if (updateTimerThread != null) updateTimerThread.interrupt();
        if (updateSeriesSevice != null) updateSeriesSevice.cancel();
        if (stopTest != null) stopTest.interrupt();

    }

    @FXML
    private void startTest() {
        beforeTest();
        Hypoxia.soundManager.pushSoundToTrackQueue(Sounds.HYP_MALE_TIME_TO_END_5_MINUTES, SoundManager.SoundType.VOICE);
        updateSeriesSevice = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        try (Socket socket = new Socket("localhost", port);
                             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                            Command startTest = new StartTest();
                            output.write(serialize(startTest));
                            output.newLine();
                            output.flush();

                            System.err.println("Test started");

                            int counterPoints = 0, counterInspections = 0, spo2, pulse, signal;
                            String line;

                            while (!isStageClosed && isTesting) {
                                line = br.readLine();
                                if (line == null) break;
                                Data data = deserializeData(line);

                                if (data instanceof Inspections) {
                                    Inspections inspections = (Inspections) data;

                                    spo2 = inspections.getSpo2();
                                    pulse = inspections.getPulse();
                                    signal = inspections.getSignal();
                                    System.err.println("pulse: " + pulse + " --- spo2: " + spo2);

                                    final int spo2Temp = spo2;
                                    final int pulseTemp = pulse;
                                    final int signalTemp = signal;
                                    Platform.runLater(() -> {
                                        textHR.setText("" + pulseTemp);
                                        textSPO2.setText("" + spo2Temp);
                                        textSignal.setText("" + signalTemp);
                                    });

                                    if (++counterInspections % NUMBER_OF_SKIP == 0) {
                                        seriesHR.getData().add(new XYChart.Data<>(counterPoints, pulse));
                                        seriesSPO2.getData().add(new XYChart.Data<>(counterPoints++, spo2));

                                        double upperBound = ((NumberAxis) chart.getXAxis()).getUpperBound();
                                        if (counterPoints > 0.8 * upperBound) {
                                            ((NumberAxis) chart.getXAxis()).setUpperBound(upperBound * 1.33);
                                        }
                                        if (seconds == 1) {
                                            currentStage = 1;
                                        }

                                        if (seconds <= firstTime) {
                                            if (spo2 > SPO2Rest) SPO2Rest = spo2;
                                        }

                                        if (seconds == firstTime + 1 && currentStage == 1) {
                                            currentStage = 2;

                                            final int xValue = counterPoints - 1;
                                            smallestSPO2 = SPO2Rest;
                                            Platform.runLater(() -> {
                                                textSPO2Rest.setText("" + SPO2Rest);
                                                Hypoxia.soundManager.playSound(Sounds.HYP_MALE_WEAR_ON_MASK, SoundManager.SoundType.VOICE);
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

                                        if (currentStage == 2) {
                                            if (seconds == 135 && smallestSPO2 >= SPO2Rest) {
                                                Util.showAlert(Alert.AlertType.WARNING, null, null,
                                                        "Проверьте плотность прилегания гипоксической маски и гермитичность дыхательной цепи!", false);
                                            }
                                            smallestSPO2 = (spo2 != 0 && spo2 < smallestSPO2) ? spo2 : smallestSPO2;
                                            if (seconds >= secondTime || (spo2 != 0 && (spo2 <= 90 || spo2<=SPO2Rest-7))) {
                                                currentStage = 3;
                                                System.err.println("Наименьшее SPO2: " + smallestSPO2);

                                                timeOfFall = seconds - firstTime;
                                                System.err.println("Время падения: " + timeOfFall);
                                                timeOfStartOfRecovery = seconds;

                                                final int xValue = counterPoints - 1;
                                                Platform.runLater(() -> {
                                                    textFallTime.setText("" + timeOfFall);
                                                    Hypoxia.soundManager.playSound(Sounds.HYP_MALE_TAKE_OFF, SoundManager.SoundType.VOICE);
                                                    Hypoxia.soundManager.pushSoundToTrackQueue(Sounds.HYP_MALE_MEASURING_RECOVERY_PARAMETERS, SoundManager.SoundType.VOICE);
                                                    XYChart.Data<Number, Number> verticalMarker = new XYChart.Data<>(xValue, 0);
                                                    markers.push(verticalMarker);
                                                    chart.addVerticalValueMarker(verticalMarker);
                                                });

                                                textNotification.setVisible(false);
                                                textMaskaOn.setVisible(false);
                                                textMaskaOff.setVisible(true);
                                            }
                                        }

                                        if (currentStage == 3 && ((spo2 >= 95 || spo2 >= SPO2Rest) || (timeOfStartOfRecovery<secondTime && seconds >= secondTime))) {
                                            currentStage = 4;
                                            timeOfRecovery = seconds - timeOfStartOfRecovery;
                                            double hypIndex = getHypIValue(timeOfFall, timeOfRecovery);

                                            System.err.println("Время восстановления: " + timeOfRecovery);

                                            Platform.runLater(() -> {
                                                if (timeOfRecovery != 0) {
                                                    textRecoveryTime.setText("" + timeOfRecovery);
                                                } else {
                                                    textRecoveryTime.setText("---");
                                                }
                                                Hypoxia.soundManager.pushSoundToTrackQueue(Sounds.HYP_MALE_RECOVERY_COMPLETED, SoundManager.SoundType.VOICE);
                                                textHypI.setText(String.valueOf(hypIndex));
                                                ok_button.setDisable(false);
                                            });
                                            isTesting = false;
                                        }
                                    }

                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }else {
                                    System.out.println("Not inspection consumed. isTesting - false");
                                    isTesting = false;
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            System.err.println("Start test thread stopped!");
                            afterTest();
                        }
                        return null;
                    }
                };
            }
        };
        updateSeriesSevice.start();
        updateSeriesSevice.setOnSucceeded(event -> {
            System.out.println("Try reset Test");
            if (currentStage == 0 && !isStageClosed) {
                afterTest();
                beforeTest();
                updateSeriesSevice.reset();
                updateSeriesSevice.start();
            }
        });
    }

    private double getHypIValue(int timeOfFall, int timeOfRecovery) {
        //Проверки
        //if(timeOfFall<timeOfRecovery) throw new IllegalArgumentException("timeOfFall can't be smaller than timeOfRecovery");
        if (timeOfRecovery < 0) throw new IllegalArgumentException("timeOfRecovery can't be negative");
        //Логика
        System.out.println("SPO2Rest = " + SPO2Rest);
        System.out.println("smallestSPO2 = " + smallestSPO2);
        int differenceSPO2 = SPO2Rest - smallestSPO2;
        if (timeOfRecovery > 0 || differenceSPO2 >= 7) {
            double hypIndex = timeOfFall * 1.0 / timeOfRecovery;
            if (hypIndex > 9) return 9;
            if (hypIndex < 1) return 1;
            return Math.round(hypIndex);
        } else if (timeOfRecovery == 0) {
            if (differenceSPO2 < 1) return 9;
            else if (differenceSPO2 == 1) return 8;
            else if (differenceSPO2 == 2) return 7;
            else if (differenceSPO2 == 3) return 6;
            else if (differenceSPO2 == 4) return 5;
            else if (differenceSPO2 == 5) return 4;
            else if (differenceSPO2 == 6) return 3;
        }
        //С логикой не очень
        throw new IllegalArgumentException("Can't calculate HypI Value");
    }

    @FXML
    private void cancel(ActionEvent actionEvent) {
        try {
            closeConnections();
        } finally {
            stage.close();
        }
    }

    @FXML
    private void ok(ActionEvent actionEvent) {
        writeJSON();
        writeJSON();
        try {
            closeConnections();
        } finally {
            stage.close();
        }

    }


    private void beforeTest() {
        isTesting = true;
        checkReadyThread.interrupt();


        updateTimerThread = new UpdateTimerThread(this);
        updateTimerThread.start();

        disableAll();
        textNotification.setVisible(false);


        prepareChart();

        SPO2Rest = 0;

        ok_button.setDisable(true);
        while (!markers.isEmpty()) {
            chart.removeVerticalValueMarker(markers.pop());
        }


        textSPO2Rest.setText("---");
        textFallTime.setText("---");
        textRecoveryTime.setText("---");
        textTimer.setText("00:00");
    }

    void afterTest() {
        if (currentStage != 4 && currentStage != 0) badEndScreen.setVisible(true);
        if (updateTimerThread != null) updateTimerThread.interrupt();
        isTesting = false;
        seconds = 0;
        System.err.println("After test");

    }

    private int seconds = 0;

    void updateTimer() {
        seconds++;
        Platform.runLater(() -> {
            int sec = seconds % 60;
            textTimer.setText("0" + (seconds / 60) + ":" + ((sec < 10) ? "0" + sec : "" + sec));
        });

    }

    private void startChecking() {
        checkReadyThread = new CheckReadyThread(this);
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

    public boolean isConstraints() {
        return constraints;
    }

    public void setConstraints(boolean constraints) {
        this.constraints = constraints;
        if (isConstraints())
            Util.showAlert(Alert.AlertType.WARNING, "Внимание!", null, "Прохождение гипоксического теста не рекомендовано!", true);
    }

    private JSONObject createJSON() {
        JSONObject obj = new JSONObject();
        double hypI = 0;
        try {
            hypI = Double.parseDouble(textHypI.getText());
        } catch (Exception ex) {
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
                if (var2 != null) {
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
        try (Socket socket = new Socket("localhost", HypoxiaController.port);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            Command command = new Launch(controller.getAge(), controller.isMan(),
                    controller.getWeight(), controller.getHeight(), controller.getActivityLevel(),
                    controller.getSystBP(), controller.getDiastBP(), HypoxiaController.secondsForTest);
            output.write(serialize(command));
            output.newLine();
            output.flush();
            System.err.println(command);

            boolean isReady = false;
            String data;
            do {
                System.err.println("Checking ready...");
                CheckStatus checkStatus = new CheckStatus();

                output.write(serialize(checkStatus));
                output.newLine();
                output.flush();

                data = br.readLine();
                if (data!=null) {
                    ReadyStatus readyStatus = (ReadyStatus) deserializeData(data);
                    isReady = readyStatus.isPulse();

                    if (isReady) {
                        System.err.println("Ready - OK!");
                        controller.enableAll();
                        data = br.readLine();
                        if (data!=null) {
                            Inspections inspections = (Inspections) deserializeData(data);
                            Platform.runLater(() -> {
                                controller.textHR.setText("" + inspections.getPulse());
                                controller.textSPO2.setText("" + inspections.getSpo2());
                                controller.textSignal.setText("" + inspections.getSignal());
                            });
                        }

                    } else {
                        System.err.println("Ready - Not Ready!");
                        controller.disableAll();
                        Platform.runLater(() -> {
                            controller.textHR.setText("0");
                            controller.textSPO2.setText("0");
                            controller.textSignal.setText("0");
                        });
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupt checking ready");
                    }
                }
            }while (!controller.isTesting && !isInterrupted() && !controller.isStageClosed);
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
        while (!isInterrupted() && !controller.isStageClosed) {
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