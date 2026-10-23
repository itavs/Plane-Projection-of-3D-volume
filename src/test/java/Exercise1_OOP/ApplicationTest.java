package Exercise1_OOP;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    @TempDir
    Path tempDirectory;

    private static final int SIZE = 10;

    private static final String INVALID_GEOMETRY =
            "0,0,0,1,0,0,2,0,0";

    @Test
    void preservesFrameOrderAndUsesVolumeWideWhiteLevel()
            throws Exception {

        // The maximum, 100, is on a slice absent from the CSV.
        Path volume = writeVolume(25, 0, 50, 100);
        Path csv = writeCsv(
                rectangle(2) + "\n"
                        + rectangle(0) + "\n"
                        + rectangle(1));

        Path output = tempDirectory.resolve("ordered.mp4");

        run(volume, csv, output);

        List<BufferedImage> frames = decode(output);

        assertEquals(3, frames.size());

        // CSV order: z=2, z=0, z=1.
        assertCenterGray(frames.get(0), 128);
        assertCenterGray(frames.get(1), 64);
        assertCenterGray(frames.get(2), 0);
    }

    @Test
    void acceptsBothTriangleAndTrapezoid() throws Exception {
        Path volume = writeVolume(100);

        String triangle = "5,1,0,2,5,0,8,5,0";
        String trapezoid = "4,1,0,6,1,0,1,5,0,9,5,0";

        Path csv = writeCsv(triangle + "\n" + trapezoid);
        Path output = tempDirectory.resolve("shapes.mp4");

        run(volume, csv, output);

        List<BufferedImage> frames = decode(output);

        assertEquals(2, frames.size());

        for (BufferedImage frame : frames) {
            assertEquals(640, frame.getWidth());
            assertEquals(480, frame.getHeight());
            assertCenterGray(frame, 255);
        }
    }

    @Test
    void skipsParsingAndGeometryErrorsAndKeepsValidOrder()
            throws Exception {

        Path volume = writeVolume(100, 0);

        Path csv = writeCsv(
                rectangle(0) + "\n"
                        + "invalid,row\n"
                        + INVALID_GEOMETRY + "\n"
                        + rectangle(1));

        Path output = tempDirectory.resolve("mixed.mp4");

        String errors = runCapturingErrors(volume, csv, output);

        assertTrue(errors.contains("Skipping CSV line 2:"));
        assertTrue(errors.contains("Skipping CSV line 3:"));
        assertFalse(errors.contains("Skipping CSV line 1:"));
        assertFalse(errors.contains("Skipping CSV line 4:"));

        List<BufferedImage> frames = decode(output);

        assertEquals(2, frames.size());
        assertCenterGray(frames.get(0), 255);
        assertCenterGray(frames.get(1), 0);
    }

    @Test
    void emptyCsvDoesNotCreateOrOverwriteOutput()
            throws Exception {

        Path volume = writeVolume(100);
        Path csv = writeCsv("");

        assertNoOutputCreatedOrOverwritten(volume, csv);
    }

    @Test
    void unparseableCsvDoesNotCreateOrOverwriteOutput()
            throws Exception {

        Path volume = writeVolume(100);
        Path csv = writeCsv("invalid,row\n1,2,3\n");

        assertNoOutputCreatedOrOverwritten(volume, csv);
    }

    @Test
    void invalidGeometryDoesNotCreateOrOverwriteOutput()
            throws Exception {

        Path volume = writeVolume(100);
        Path csv = writeCsv(INVALID_GEOMETRY);

        assertNoOutputCreatedOrOverwritten(volume, csv);
    }

    @Test
    void nonPositiveVolumesProduceBlackVideo()
            throws Exception {

        // Exercises the fallback white level when no voxel is positive.
        for (int value : new int[]{0, -20}) {
            Path volume = writeVolume(value);
            Path csv = writeCsv(rectangle(0));

            Path output = tempDirectory.resolve(
                    "non-positive-" + value + ".mp4");

            run(volume, csv, output);

            List<BufferedImage> frames = decode(output);

            assertEquals(1, frames.size());
            assertCenterGray(frames.get(0), 0);
        }
    }

    @Test
    void missingOrMalformedNrrdPropagatesIOException()
            throws Exception {

        Path missing = tempDirectory.resolve("missing.nrrd");
        Path malformed = tempDirectory.resolve("malformed.nrrd");

        Files.writeString(
                malformed,
                "This is not an NRRD file.",
                StandardCharsets.US_ASCII);

        Path csv = writeCsv(rectangle(0));

        for (Path volume : new Path[]{missing, malformed}) {
            Path output = tempDirectory.resolve("protected.mp4");
            byte[] original = {10, 20, 30, 40};

            Files.write(output, original);

            assertThrows(
                    IOException.class,
                    () -> run(volume, csv, output));

            assertArrayEquals(original, Files.readAllBytes(output));
        }
    }

    @Test
    void missingCsvPropagatesIOExceptionAndPreservesOutput()
            throws Exception {

        Path volume = writeVolume(100);
        Path missingCsv = tempDirectory.resolve("missing.csv");
        Path output = tempDirectory.resolve("protected.mp4");

        byte[] original = {10, 20, 30, 40};
        Files.write(output, original);

        assertThrows(
                IOException.class,
                () -> run(volume, missingCsv, output));

        assertArrayEquals(original, Files.readAllBytes(output));
    }

    @Test
    void outputOpenFailurePropagatesIOException()
            throws Exception {

        Path volume = writeVolume(100);
        Path csv = writeCsv(rectangle(0));

        // The parent directory deliberately does not exist.
        Path output = tempDirectory
                .resolve("missing-directory")
                .resolve("result.mp4");

        assertThrows(
                IOException.class,
                () -> run(volume, csv, output));

        assertFalse(Files.exists(output));
    }

    @Test
    void skipsOversizedFrameAndContinuesToNextRow()
            throws Exception {

        Path volume = writeVolume(100);

        // Valid rectangle, but its frame exceeds the size limit.
        String oversized =
                "0,0,0,5000,0,0,0,5000,0,5000,5000,0";

        Path csv = writeCsv(
                oversized + "\n" + rectangle(0));

        Path output = tempDirectory.resolve("after-oversized.mp4");

        String errors = runCapturingErrors(volume, csv, output);

        assertTrue(errors.contains("Skipping CSV line 1:"));
        assertFalse(errors.contains("Skipping CSV line 2:"));

        List<BufferedImage> frames = decode(output);

        assertEquals(1, frames.size());
        assertCenterGray(frames.get(0), 255);
    }

    private void run(Path volume, Path csv, Path output)
            throws IOException {

        new Application().run(
                volume.toString(),
                csv.toString(),
                output.toString());
    }

    private String runCapturingErrors(
            Path volume,
            Path csv,
            Path output) throws IOException {

        PrintStream originalError = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        try (PrintStream replacement = new PrintStream(
                captured, true, StandardCharsets.UTF_8)) {

            System.setErr(replacement);
            run(volume, csv, output);

        } finally {
            System.setErr(originalError);
        }

        return captured.toString(StandardCharsets.UTF_8);
    }

    private void assertNoOutputCreatedOrOverwritten(
            Path volume,
            Path csv) throws IOException {

        Path absentOutput = tempDirectory.resolve("absent.mp4");

        run(volume, csv, absentOutput);

        assertFalse(
                Files.exists(absentOutput),
                "No video should be created without valid frames.");

        Path existingOutput = tempDirectory.resolve("existing.mp4");
        byte[] original = {10, 20, 30, 40};

        Files.write(existingOutput, original);

        run(volume, csv, existingOutput);

        assertArrayEquals(
                original,
                Files.readAllBytes(existingOutput),
                "An existing output must remain unchanged.");
    }

    private Path writeCsv(String content) throws IOException {
        Path csv = tempDirectory.resolve("slices.csv");

        Files.writeString(csv, content, StandardCharsets.UTF_8);

        return csv;
    }

    private String rectangle(int z) {
        // Point order: top-left, top-right, bottom-left, bottom-right.
        return "1,1," + z
                + ",7,1," + z
                + ",1,7," + z
                + ",7,7," + z;
    }

    private Path writeVolume(int... valuesByZ) throws IOException {
        Path volume = tempDirectory.resolve("volume.nrrd");

        String header =
                "NRRD0005\n"
                        + "type: short\n"
                        + "dimension: 3\n"
                        + "sizes: " + SIZE + " " + SIZE
                        + " " + valuesByZ.length + "\n"
                        + "encoding: raw\n"
                        + "endian: little\n"
                        + "\n";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(header.getBytes(StandardCharsets.US_ASCII));

        // Each Z slice has a constant signed 16-bit value.
        // X varies fastest, then Y, then Z.
        for (int value : valuesByZ) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    bytes.write(value & 0xff);
                    bytes.write((value >>> 8) & 0xff);
                }
            }
        }

        Files.write(volume, bytes.toByteArray());

        return volume;
    }

    private List<BufferedImage> decode(Path video) throws Exception {
        List<BufferedImage> frames = new ArrayList<>();

        try (SeekableByteChannel channel =
                     NIOUtils.readableChannel(video.toFile())) {

            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            Picture picture;

            while ((picture = grab.getNativeFrame()) != null) {
                frames.add(AWTUtil.toBufferedImage(picture));
            }
        }

        return frames;
    }

    private void assertCenterGray(
            BufferedImage image,
            int expected) {

        int rgb = image.getRGB(
                image.getWidth() / 2,
                image.getHeight() / 2);

        int[] channels = {
                (rgb >>> 16) & 0xff,
                (rgb >>> 8) & 0xff,
                rgb & 0xff
        };

        for (int actual : channels) {
            assertTrue(
                    Math.abs(actual - expected) <= 10,
                    "Expected gray near " + expected
                            + ", but received " + actual);
        }
    }
}