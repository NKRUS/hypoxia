package ru.kit.hypoxia;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.kit.SoundManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Hypoxia extends Stage {
    private static final int windowWidth = 950;
    private static final int windowHeight = 550;
    private static final String SERVICE_NAME = "OxiService";


    public Hypoxia(int age, boolean isMale, int height, int width, int activityLevel, int systBP, int diastBP, boolean constraints, String path, SoundManager soundManager) throws IOException {


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kit/hypoxia/fxml/hypoxia.fxml"));
        HypoxiaController hypoxiaController = new HypoxiaController(soundManager);
        loader.setController(hypoxiaController);
        Parent root = loader.load();

        HypoxiaController controller = loader.getController();

        controller.setAge(age);
        controller.setMan(isMale);
        controller.setHeight(height);
        controller.setWeight(width);
        controller.setActivityLevel(activityLevel);
        controller.setSystBP(systBP);
        controller.setDiastBP(diastBP);
        controller.setPath(path);
        controller.setConstraints(constraints);

        controller.setStage(this);

        this.setOnCloseRequest(event -> {
            try {
                controller.closeConnections();
            } finally {
                this.close();
            }
        });

        this.setScene(new Scene(root));
        this.setMinWidth(windowWidth);
        this.setMinHeight(windowHeight);



    }

    @Override
    public void close() {
        super.close();
        //controller.afterTest();
    }


}

