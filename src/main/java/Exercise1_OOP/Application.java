package Exercise1_OOP;

import java.io.IOException;
import java.util.ArrayList;

public class Application {

    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 480;
    private static final int PLAYBACK_FPS = 30;

    private final NrrdReader nrrdReader;
    private final CsvReader csvReader;
    private final GeometryValidator validator;
    private final FrameGenerator frameGenerator;

    public Application() {
        this.nrrdReader = new NrrdReader();
        this.csvReader = new CsvReader();
        this.validator = new GeometryValidator();
        this.frameGenerator = new FrameGenerator();
    }

    public void run(
            String nrrdPath,
            String csvPath,
            String outputPath) throws IOException {

        System.out.println("Loading NRRD...");
        NrrdReader.Volume volume = nrrdReader.read(nrrdPath);

        System.out.println("Reading CSV...");
        ArrayList<SliceRow> rows = csvReader.read(csvPath);

        if (rows.isEmpty()) {
            System.err.println(
                    "No readable CSV rows. No video was created.");
            return;
        }

        // Use one grayscale mapping for the entire video.
        double whiteValue = findWhiteValue(volume);

        int writtenFrames = 0;
        int skippedRows = 0;
        long generationNanos = 0;

        try (VideoWriter writer = new VideoWriter(
                outputPath,
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                PLAYBACK_FPS,
                whiteValue)) {

            for (SliceRow row : rows) {
                long[][] frame;
                long elapsed;

                try {
                    validator.validate(row);

                    long start = System.nanoTime();
                    frame = frameGenerator.generate(row, volume);
                    elapsed = System.nanoTime() - start;

                } catch (IllegalArgumentException e) {
                    skippedRows++;

                    System.err.println(
                            "Skipping CSV line " + row.getLineNumber()
                                    + ": " + e.getMessage());

                    continue;
                }

                // Video writing errors stop the run;
                // they are not treated as invalid CSV rows.
                writer.writeFrame(frame);

                generationNanos += elapsed;
                writtenFrames++;
            }

        } // VideoWriter.close() finishes the video here.

        if (writtenFrames == 0) {
            System.err.println(
                    "No frames could be generated. No video was created.");
            return;
        }

        System.out.println("Video saved to: " + outputPath);
        System.out.println("Frames written: " + writtenFrames);

        System.out.println(
                "Rows skipped after CSV parsing: " + skippedRows);

        if (generationNanos > 0) {
            double seconds = generationNanos / 1_000_000_000.0;
            double generationFps = writtenFrames / seconds;

            System.out.printf(
                    "Frame generation rate: %.2f FPS%n",
                    generationFps);
        }
    }

    private double findWhiteValue(NrrdReader.Volume volume) {
        long maximum = 0;

        for (int z = 0; z < volume.getSizeZ(); z++) {
            for (int y = 0; y < volume.getSizeY(); y++) {
                for (int x = 0; x < volume.getSizeX(); x++) {
                    maximum = Math.max(
                            maximum, volume.get(x, y, z));
                }
            }
        }

        // VideoWriter requires a positive white value.
        return maximum > 0 ? (double) maximum : 1.0;
    }
}