package ru.kit.hypoxia;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application{

    public void start(Stage primaryStage) throws Exception {
        Stage stage = new Hypoxia(20, true, false, 170, 70, 4, 120, 80, "");
        stage.show();
    }

    public static void main(String[] args) throws IOException {
        Application.launch(args);
    }
}
