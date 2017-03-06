package ru.kit.hypoxia;


import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import ru.kit.hypoxia.commands.Command;
import ru.kit.hypoxia.dto.Data;

import java.io.IOException;

public class Util {
    public static Data deserializeData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Data.class);
    }

    public static Command deserializeCommand(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Command.class);
    }

    public static String serialize(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }
    public static void showAlert(Alert.AlertType type, String title, String headerText, String contextText, boolean wait){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contextText);

        if(wait){
            alert.showAndWait();
        }else alert.show();
    }
}
