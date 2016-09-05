package ru.kit.hypoxia;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HypoxiaStage extends Stage {
    public HypoxiaStage() throws IOException {
        Parent root = new FXMLLoader(getClass().getResource("ru/kit/hypoxia/fxml/hypoxia.fxml")).load();
        this.setScene(new Scene(root, 720, 580));


    }
}
