package ru.kit.hypoxia;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HypoxiaStage extends Stage {
    private static final int windowWidth = 950;
    private static final int windowHeight = 550;

    public HypoxiaStage() throws IOException {
        Parent root = new FXMLLoader(getClass().getResource("/ru/kit/hypoxia/fxml/hypoxia.fxml")).load();

        this.setScene(new Scene(root));
        this.setMinWidth(windowWidth);
        this.setMinHeight(windowHeight);

//        this.show();
    }
}
