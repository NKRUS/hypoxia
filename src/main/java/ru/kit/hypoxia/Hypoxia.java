package ru.kit.hypoxia;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Hypoxia extends Stage {
    private static final int windowWidth = 950;
    private static final int windowHeight = 550;
    private HypoxiaController controller;
    private static final String SERVICE_NAME = "OxiService";


    public Hypoxia(int age, boolean isMale, boolean isFemale, int height, int width, int activityLevel, int systBP, int diastBP, String path) throws IOException {

        execService("stop");

        execService("start");


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kit/hypoxia/fxml/hypoxia.fxml"));
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

        controller.setStage(this);


        this.setScene(new Scene(root));
        this.setMinWidth(windowWidth);
        this.setMinHeight(windowHeight);


    }

    @Override
    public void close() {
        super.close();
        //controller.afterTest();
    }

    private void execService(String command){


        try {
            Process p = null;
            p = Runtime.getRuntime().exec("net " + command + " " + SERVICE_NAME);


            p.waitFor();


            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                System.err.println(line);
                line = reader.readLine();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


}

