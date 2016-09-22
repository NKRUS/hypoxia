package ru.kit.hypoxia;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application{

    public static final boolean IS_DEBUG = true;

    public void start(Stage primaryStage) throws Exception {
//        primaryStage.setTitle("Hello World");
//        primaryStage.show();


        Stage stage = new HypoxiaStage();
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
