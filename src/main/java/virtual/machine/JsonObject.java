package virtual.machine;

import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;

public class JsonObject {
    public static com.google.gson.JsonObject readConfigFile(String filename) {
        try (FileReader reader = new FileReader(filename)) {
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
