package Exercise1_OOP;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Expected 3 arguments: <input.nrrd> <input.csv> <output.mp4>");
            System.exit(2);
            return;
        }

        for (String argument : args) {
            if (argument == null || argument.isBlank()) {
                System.err.println("File paths must not be blank.");
                System.exit(2);
                return;
            }
        }

        String nrrdPath = args[0];
        String csvPath = args[1];
        String outputPath = args[2];

        try {
            Application app = new Application();
            app.run(nrrdPath, csvPath, outputPath);

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Run failed: " + e.getMessage());
            System.exit(1);
        }
    }
}