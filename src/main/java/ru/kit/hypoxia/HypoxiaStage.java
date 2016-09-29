package ru.kit.hypoxia;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HypoxiaStage extends Stage {
    private static final int windowWidth = 950;
    private static final int windowHeight = 550;
    private HypoxiaController controller;
    private static final String SERVICE_NAME = "OxiService";


    public HypoxiaStage() throws IOException {


        execService("stop");

        execService("start");


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ru/kit/hypoxia/fxml/hypoxia.fxml"));
        Parent root = loader.load();

        HypoxiaController controller = (HypoxiaController)loader.getController();
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
            p = Runtime.getRuntime().exec("net " + command +" " + SERVICE_NAME);


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

