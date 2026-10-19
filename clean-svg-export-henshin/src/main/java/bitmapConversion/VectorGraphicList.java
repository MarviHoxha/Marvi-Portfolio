package bitmapConversion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

//Diese Klasse enthält methoden um eine Liste zu erhalten die bereits einmal konvertierte bitmaps speichert

/**
 * Manages a permanent dictionary of already converted bitmaps.
 */
public class VectorGraphicList {
    public Map<String, String> convertedBitmaps;

    /**
     * Loads the already exisiting List or creates a new one.
     */
    public VectorGraphicList() {
        this.convertedBitmaps = new HashMap<>();
        File datei = new File("convertedBitmaps.json");
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.convertedBitmaps = mapper.readValue(datei, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            //ja wenns halt nicht geht haben wir ja oben bereits die Hashmap neu initialisiert
        }
    }

    /**
     * Does a LookUp of the dictionary for a given key
     * @param bitmapData the key to look up, in base64 encoded
     * @param list the dictionary to look up in
     * @return the path to the svg file associated with the key, or null if the key is not found
     */
    //schaut in der liste ob die bitmap bereits konvertiert wurde, wenn ja gibt er den svg filename zurück, wenn nein null
    public static String bitmapDataLookUp(String bitmapData, Map<String, String> list) {
        if(list.containsKey(bitmapData)) {
            return list.get(bitmapData);
        }
        return null;
    }

    /**
     * saves the dictonary to a json file
     */
    public void saveList() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("convertedBitmaps.json"), this.convertedBitmaps);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
