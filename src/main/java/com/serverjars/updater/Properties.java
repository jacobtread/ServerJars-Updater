package com.serverjars.updater;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jacobtread
 * -
 * created on 7/11/20 at 11:27 PM
 */
public class Properties {

     static void save(Map<String, String> properties, File file) {
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            output.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        try {
            if (file.exists() || file.createNewFile()) {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                bufferedWriter.write(output.toString());
                bufferedWriter.close();
                Updater.log(Updater.center("serverjars.properties save successfully"));
            }
        } catch (IOException e) {
            Updater.err("Unable to create serverjars.properties");
        }
    }

    static Map<String, String> parse(String text) {
        Map<String, String> properties = new HashMap<>();
        for (String line : text.split("\n")) {
            String[] parts = line.split("=");
            if (parts.length == 2) {
                properties.put(parts[0], parts[1]);
            }
        }
        return properties;
    }

}
