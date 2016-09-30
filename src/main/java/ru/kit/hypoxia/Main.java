package ru.kit.hypoxia;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application{

    public static final boolean IS_DEBUG = true;

    public void start(Stage primaryStage) throws Exception {
//        primaryStage.setTitle("Hello World");
//        primaryStage.show();


        Stage stage = new Hypoxia(20, true, false, 170, 70, 4, 120, 80, "");
        stage.show();
//        stage.close();
//
//        System.err.println("123");
//        primaryStage.close();

    }

    public static void main(String[] args) throws IOException {
        Application.launch(args);
    }
}
