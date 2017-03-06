package ru.kit.hypoxia.service;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by Kit on 22.02.2017.
 */
public class Sounds {
   /* private static String soundsProperties = "/sounds.properties";
    private static final Properties properties = new Properties();
    static {
        try {
            properties.load(Sounds.class.getResourceAsStream(soundsProperties));
            for (Object o :properties.values()) {
                System.out.println("Loaded: " + (String)o);
            }

        } catch (IOException e) {
            System.err.println("Can not load properties file");
            e.printStackTrace();
        }
    }*/

    public static final String HYP_MALE_TEST_STARTED = "sounds/voice/HYP_MALE_test_started.wav";
    public static final String HYP_MALE_TIME_TO_END_5_MINUTES = "sounds/voice/HYP_MALE_time_to_end_5_minutes.wav";
    public static final String HYP_FEMALE_HOLD_MASK_IN_HANDS = "sounds/voice/HYP_FEMALE_hold_mask_in_hands.wav";
    public static final String HYP_FEMALE_ON_SIGNAL_PRESS_TO_FACE = "sounds/voice/HYP_FEMALE_on_signal_press_to_face.wav";
    public static final String HYP_MALE_TAKE_OFF = "sounds/voice/HYP_MALE_take_off.wav";
    public static final String HYP_MALE_MEASURING_RECOVERY_PARAMETERS = "sounds/voice/HYP_MALE_measuring_recovery_parameters.wav";
    public static final String HYP_MALE_RECOVERY_COMPLETED = "sounds/voice/HYP_MALE_recovery_completed.wav";
    public static final String HYP_MALE_WEAR_ON_MASK = "sounds/voice/HYP_MALE_wear_on_mask.wav";
}
