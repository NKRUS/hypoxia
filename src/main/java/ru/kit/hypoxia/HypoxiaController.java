package ru.kit.hypoxia;


import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

public class HypoxiaController {

    @FXML private void initialize() {
        System.err.println(chart.getClass());
    }

    @FXML
    private LineChart<Integer, Long> chart;

    @FXML
    private void startTest(ActionEvent actionEvent) {
        if (chart.getData().size() != 1) {
            XYChart.Series<Integer, Long> series = new XYChart.Series<Integer, Long>();
            ObservableList<XYChart.Data<Integer, Long>> data = series.getData();
            data.add(new XYChart.Data<Integer, Long>(0, 15L));
            data.add(new XYChart.Data<Integer, Long>(1, 16L));
            data.add(new XYChart.Data<Integer, Long>(10, 17L));
            data.add(new XYChart.Data<Integer, Long>(15, 18L));
            chart.getData().add(series);
        } else {
            ((XYChart.Series<Integer, Long>)chart.getData().get(0)).getData().add(new XYChart.Data<Integer, Long>(10, 150L));
        }

    }
}
