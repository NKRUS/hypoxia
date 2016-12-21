package ru.kit.hypoxia;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class Main extends Application{

    public void start(Stage primaryStage) throws Exception {
//        Stage stage = new Hypoxia(20, true, false, 170, 70, 4, 120, 80, "");
//
//        stage.show();
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Гипоксический тест");
        btn.setOnAction(event -> {
            Stage s = null;
            try {
                s = new Hypoxia(20, true, false, 170, 70, 4, 120, 80, "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            s.show();
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }

    public static void main(String[] args) throws IOException {
        Application.launch(args);
    }
}
