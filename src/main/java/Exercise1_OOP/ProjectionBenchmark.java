package Exercise1_OOP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class ProjectionBenchmark {

    private static final int WARMUP_ROUNDS = 3;
    private static final int MEASUREMENT_ROUNDS = 5;

    /*
     * Keep the latest generated frame observable.
     * Only one frame is retained here, not the entire sequence.
     */
    private static volatile long[][] lastGeneratedFrame;

    public static void main(String[] args) {
        if (args.length != 2
                || args[0].isBlank()
                || args[1].isBlank()) {

            System.err.println(
                    "Expected 2 arguments: <input.nrrd> <input.csv>");
            System.exit(2);
            return;
        }

        try {
            runBenchmark(args[0], args[1]);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Benchmark failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runBenchmark(
            String nrrdPath,
            String csvPath) throws IOException {

        System.out.println("Loading volume and slice definitions...");

        NrrdReader.Volume volume = new NrrdReader().read(nrrdPath);
        ArrayList<SliceRow> rows = new CsvReader().read(csvPath);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "The CSV contains no readable slice definitions.");
        }

        GeometryValidator validator = new GeometryValidator();

        for (SliceRow row : rows) {
            try {
                validator.validate(row);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid geometry at CSV line "
                                + row.getLineNumber() + ": "
                                + e.getMessage(),
                        e);
            }
        }

        FrameGenerator generator = new FrameGenerator();

        System.out.printf(
                "Volume dimensions: %d x %d x %d%n",
                volume.getSizeX(),
                volume.getSizeY(),
                volume.getSizeZ());

        System.out.println("Slices per round: " + rows.size());
        System.out.println(
                "Timing scope: FrameGenerator.generate calls only.");

        System.out.println();
        System.out.println("Starting warm-up...");

        for (int round = 1; round <= WARMUP_ROUNDS; round++) {
            for (SliceRow row : rows) {
                lastGeneratedFrame = generator.generate(row, volume);
            }

            System.out.println(
                    "Warm-up round " + round + " completed.");
        }

        System.out.println();
        System.out.println("Starting measurements...");

        long totalNanos = 0;
        double sumFps = 0;
        double minimumFps = Double.POSITIVE_INFINITY;
        double maximumFps = 0;

        for (int round = 1; round <= MEASUREMENT_ROUNDS; round++) {
            long generationNanos = 0;

            for (SliceRow row : rows) {
                long start = System.nanoTime();

                long[][] frame = generator.generate(row, volume);

                long elapsed = System.nanoTime() - start;
                generationNanos += elapsed;

                // Outside the timed region.
                lastGeneratedFrame = frame;
            }

            if (generationNanos <= 0) {
                throw new IllegalArgumentException(
                        "Measured duration must be positive.");
            }

            double seconds = generationNanos / 1_000_000_000.0;
            double fps = rows.size() / seconds;

            totalNanos += generationNanos;
            sumFps += fps;
            minimumFps = Math.min(minimumFps, fps);
            maximumFps = Math.max(maximumFps, fps);

            System.out.printf(
                    Locale.ROOT,
                    "Round %d: %d frames, %.3f ms, %.2f FPS%n",
                    round,
                    rows.size(),
                    generationNanos / 1_000_000.0,
                    fps);
        }

        long totalFrames = (long) rows.size() * MEASUREMENT_ROUNDS;

        double meanFps = sumFps / MEASUREMENT_ROUNDS;
        double combinedFps =
                totalFrames / (totalNanos / 1_000_000_000.0);

        System.out.println();
        System.out.println("Benchmark summary:");

        System.out.printf(
                Locale.ROOT,
                "Mean of round rates: %.2f FPS%n",
                meanFps);

        System.out.printf(
                Locale.ROOT,
                "Combined throughput: %.2f FPS%n",
                combinedFps);

        System.out.printf(
                Locale.ROOT,
                "Range: %.2f - %.2f FPS%n",
                minimumFps,
                maximumFps);

        System.out.println("Measured frames: " + totalFrames);
        System.out.println(
                "Warm-up frames are excluded from these results.");
    }
}