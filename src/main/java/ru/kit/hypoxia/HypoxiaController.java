package ru.kit.hypoxia;


import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HypoxiaController {

    private Queue<Integer> dataSPO2;
    private Queue<Integer> dataHR;

    private static final int MAX_DATA_POINTS = 7302;
    private int xSeriesData = 0;
    private XYChart.Series<Number, Number> seriesHR = new XYChart.Series<>();
    private XYChart.Series<Number, Number> seriesSPO2 = new XYChart.Series<>();
    private ExecutorService executor;
    private ConcurrentLinkedQueue<Number> dataQHR = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Number> dataQSPO2 = new ConcurrentLinkedQueue<>();


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
        try {
            PulseOxiEquipment pulseOxiEquipment = new PulseOxiEquipment(40, true, 70, 170, 120, 80);
            this.setDataHR(this.dataHR);
            this.setDataSPO2(this.dataSPO2);
            Thread oxiThread = new Thread(pulseOxiEquipment::run);
            oxiThread.setDaemon(true);
            oxiThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }


        executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        AddToQueue addToQueue = new AddToQueue();
        executor.execute(addToQueue);
        //-- Prepare Timeline
        prepareTimeline();

    }



    private class AddToQueue implements Runnable {
        public void run() {
            try {
                // add a item of random data to queue
                dataQHR.add(Math.random());
                dataQSPO2.add(Math.random());

                Thread.sleep(50);
                executor.execute(this);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    //-- Timeline gets called in the JavaFX Main thread
    private void prepareTimeline() {
        // Every frame to take any data from queue and add to chart
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                addDataToSeries();
            }
        }.start();
    }

    private void addDataToSeries() {
        while (!dataQSPO2.isEmpty() && !dataQHR.isEmpty()) {
            seriesHR.getData().add(new XYChart.Data<>(xSeriesData, dataQHR.remove()));
            seriesSPO2.getData().add(new XYChart.Data<>(xSeriesData++, dataQSPO2.remove()));
        }


    }
}
