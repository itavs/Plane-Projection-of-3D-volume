package Exercise1_OOP;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;

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

        validateOutputPath(nrrdPath, csvPath, outputPath);

        System.out.println("Loading NRRD...");
        NrrdReader.Volume volume = nrrdReader.read(nrrdPath);

        System.out.println("Reading CSV...");
        ArrayList<SliceRow> rows = csvReader.read(csvPath);

        if (rows.isEmpty()) {
            System.err.println(
                    "No readable CSV rows. No video was created.");
            return;
        }

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


                writer.writeFrame(frame);

                generationNanos += elapsed;
                writtenFrames++;
            }

        }

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

    private void validateOutputPath(
            String nrrdPath,
            String csvPath,
            String outputPath) throws IOException {

        Path output = Path.of(outputPath).toAbsolutePath().normalize();
        Path nrrd = Path.of(nrrdPath).toAbsolutePath().normalize();
        Path csv = Path.of(csvPath).toAbsolutePath().normalize();

        if (refersToSameFile(output, nrrd)
                || refersToSameFile(output, csv)) {
            throw new IllegalArgumentException(
                    "Output file must be different from both input files.");
        }
    }

    private boolean refersToSameFile(Path first, Path second)
            throws IOException {

        if (first.equals(second)) {
            return true;
        }

        return Files.exists(first)
                && Files.exists(second)
                && Files.isSameFile(first, second);
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

        return maximum > 0 ? (double) maximum : 1.0;
    }
}