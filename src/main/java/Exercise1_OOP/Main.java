package Exercise1_OOP;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Application app = new Application();


        String nrrdPath = "C:/OOP_Test/test_volume_500.nrrd";
        String csvPath = "C:/OOP_Test/planes.csv";
        String outputPath = "C:/OOP_Test/result.mp4";

        try {
            app.run(nrrdPath, csvPath, outputPath);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Run failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}