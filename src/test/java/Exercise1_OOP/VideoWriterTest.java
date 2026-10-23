package Exercise1_OOP;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VideoWriterTest {

    @TempDir
    Path tempDirectory;

    private static final int SIZE = 64;
    private static final int COLOR_TOLERANCE = 10;

    @Test
    void rejectsInvalidConstructorArguments() {
        String output = tempDirectory.resolve("invalid.mp4").toString();

        for (String path : new String[]{null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new VideoWriter(path, SIZE, SIZE, 30, 100)
            );
        }

        int[][] invalidSizes = {
                {0, 64},
                {64, 0},
                {-2, 64},
                {64, -2},
                {63, 64},
                {64, 63}
        };

        for (int[] size : invalidSizes) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new VideoWriter(
                            output, size[0], size[1], 30, 100)
            );
        }

        for (int fps : new int[]{0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new VideoWriter(output, SIZE, SIZE, fps, 100)
            );
        }

        for (double white : new double[]{
                0,
                -1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new VideoWriter(output, SIZE, SIZE, 30, white)
            );
        }
    }

    @Test
    void doesNotCreateFileWithoutFrames() throws IOException {
        Path output = tempDirectory.resolve("empty.mp4");

        try (VideoWriter writer = writer(output)) {
            assertEquals(0, writer.getFrameCount());
            assertFalse(Files.exists(output));
        }

        assertFalse(Files.exists(output));
    }

    @Test
    void preservesExistingFileWhenNoFramesAreWritten()
            throws IOException {

        Path output = tempDirectory.resolve("existing.mp4");
        byte[] original = {10, 20, 30, 40};
        Files.write(output, original);

        try (VideoWriter writer = writer(output)) {
            assertEquals(0, writer.getFrameCount());
        }

        assertArrayEquals(original, Files.readAllBytes(output));
    }

    @Test
    void rejectsInvalidFramesWithoutCreatingOutput()
            throws IOException {

        Path output = tempDirectory.resolve("invalid-frames.mp4");

        long[][][] invalidFrames = {
                null,
                new long[0][],
                new long[][]{null},
                new long[][]{{}},
                new long[][]{{1, 2}, {3}},
                new long[][]{{1, 2}, null}
        };

        try (VideoWriter writer = writer(output)) {
            for (long[][] frame : invalidFrames) {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> writer.writeFrame(frame)
                );

                assertEquals(0, writer.getFrameCount());
            }
        }

        assertFalse(Files.exists(output));
    }

    @Test
    void canContinueAfterRejectingAnInvalidFrame() throws Exception {
        Path output = tempDirectory.resolve("recovery.mp4");

        try (VideoWriter writer = writer(output)) {
            writer.writeFrame(solid(0));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> writer.writeFrame(new long[][]{{1, 2}, {3}})
            );

            assertEquals(1, writer.getFrameCount());

            writer.writeFrame(solid(100));

            assertEquals(2, writer.getFrameCount());
        }

        List<BufferedImage> frames = decode(output);

        assertEquals(2, frames.size());
        assertGray(frames.get(0), 32, 32, 0);
        assertGray(frames.get(1), 32, 32, 255);
    }

    @Test
    void closesTwiceSafelyAndRejectsFurtherWrites() throws Exception {
        Path output = tempDirectory.resolve("closed.mp4");

        try (VideoWriter writer = writer(output)) {
            writer.writeFrame(solid(100));
            writer.close();

            assertDoesNotThrow(writer::close);

            assertThrows(
                    IllegalStateException.class,
                    () -> writer.writeFrame(solid(0))
            );

            assertEquals(1, writer.getFrameCount());
        }

        assertEquals(1, decode(output).size());
    }

    @Test
    void reportsOutputOpenFailureWithoutCountingAFrame()
            throws IOException {

        Path output = tempDirectory
                .resolve("missing-directory")
                .resolve("output.mp4");

        try (VideoWriter writer = writer(output)) {
            assertThrows(
                    IOException.class,
                    () -> writer.writeFrame(solid(100))
            );

            assertEquals(0, writer.getFrameCount());
        }

        assertFalse(Files.exists(output));
    }

    @Test
    void mapsGrayscaleCorrectlyBeforeEncoding() throws IOException {
        Path output = tempDirectory.resolve("before-encoding.mp4");

        try (VideoWriter writer = writer(output)) {
            BufferedImage image = writer.createImage(new long[][]{
                    {0, 25},
                    {50, 100}
            });

            int[][] samples = {
                    {16, 16, 0},
                    {48, 16, 64},
                    {16, 48, 128},
                    {48, 48, 255}
            };

            for (int[] sample : samples) {
                int x = sample[0];
                int y = sample[1];
                int expected = sample[2];

                int rgb = image.getRGB(x, y);

                assertEquals(
                        expected,
                        (rgb >>> 16) & 0xFF,
                        "Red at (" + x + ", " + y + ")"
                );

                assertEquals(
                        expected,
                        (rgb >>> 8) & 0xFF,
                        "Green at (" + x + ", " + y + ")"
                );

                assertEquals(
                        expected,
                        rgb & 0xFF,
                        "Blue at (" + x + ", " + y + ")"
                );
            }
        }

        // Creating an image alone should not create a video file.
        assertFalse(Files.exists(output));
    }

    @Test
    void mapsIntensitiesAndPreservesFrameOrder() throws Exception {
        Path output = tempDirectory.resolve("grayscale.mp4");

        long[] values = {-100, 0, 25, 50, 100, 200};
        int[] expected = {0, 0, 64, 128, 255, 255};

        try (VideoWriter writer = writer(output)) {
            for (int i = 0; i < values.length; i++) {
                writer.writeFrame(solid(values[i]));
                assertEquals(i + 1, writer.getFrameCount());
            }
        }

        List<BufferedImage> frames = decode(output);

        assertEquals(values.length, frames.size());

        for (int i = 0; i < expected.length; i++) {
            assertGray(frames.get(i), 32, 32, expected[i]);
        }
    }

    @Test
    void usesFixedWhiteLevelAcrossFramesWithDifferentMaxima()
            throws Exception {

        Path output = tempDirectory.resolve("fixed-white.mp4");

        try (VideoWriter writer = writer(output)) {
            writer.writeFrame(new long[][]{
                    {50, 50},
                    {50, 50}
            });

            writer.writeFrame(new long[][]{
                    {50, 100},
                    {50, 100}
            });
        }

        List<BufferedImage> frames = decode(output);

        assertEquals(2, frames.size());

        assertGray(frames.get(0), 16, 32, 128);
        assertGray(frames.get(1), 16, 32, 128);
        assertGray(frames.get(1), 48, 32, 255);
    }

    @Test
    void preservesWideAndTallAspectRatiosWithBlackMargins()
            throws Exception {

        Path output = tempDirectory.resolve("aspect-ratio.mp4");

        try (VideoWriter writer = writer(output)) {
            // Wide image: black margins above and below.
            writer.writeFrame(new long[][]{
                    {100, 100, 100, 100},
                    {100, 100, 100, 100}
            });

            // Tall image: black margins to the left and right.
            writer.writeFrame(new long[][]{
                    {100, 100},
                    {100, 100},
                    {100, 100},
                    {100, 100}
            });
        }

        List<BufferedImage> frames = decode(output);

        assertEquals(2, frames.size());

        BufferedImage wide = frames.get(0);

        assertGray(wide, 32, 8, 0);
        assertGray(wide, 32, 24, 255);
        assertGray(wide, 32, 40, 255);
        assertGray(wide, 32, 56, 0);

        BufferedImage tall = frames.get(1);

        assertGray(tall, 8, 32, 0);
        assertGray(tall, 24, 32, 255);
        assertGray(tall, 40, 32, 255);
        assertGray(tall, 56, 32, 0);
    }

    @Test
    void preservesPixelOrientationWhenUpscaling() throws Exception {
        Path output = tempDirectory.resolve("orientation.mp4");

        try (VideoWriter writer = writer(output)) {
            writer.writeFrame(new long[][]{
                    {0, 25},
                    {50, 100}
            });
        }

        List<BufferedImage> frames = decode(output);

        assertEquals(1, frames.size());

        BufferedImage image = frames.get(0);

        assertGray(image, 16, 16, 0);
        assertGray(image, 48, 16, 64);
        assertGray(image, 16, 48, 128);
        assertGray(image, 48, 48, 255);
    }

    @Test
    void writesRequestedDimensionsFrameCountAndPlaybackRate()
            throws Exception {

        Path output = tempDirectory.resolve("metadata.mp4");

        try (VideoWriter writer = new VideoWriter(
                output.toString(), 640, 480, 30, 100)) {

            writer.writeFrame(solid(0));
            writer.writeFrame(solid(50));
            writer.writeFrame(solid(100));
        }

        assertTrue(Files.size(output) > 0);

        List<BufferedImage> frames = decode(output);

        assertEquals(3, frames.size());

        for (BufferedImage image : frames) {
            assertEquals(640, image.getWidth());
            assertEquals(480, image.getHeight());
        }

        try (SeekableByteChannel channel =
                     NIOUtils.readableChannel(output.toFile())) {

            FrameGrab grab = FrameGrab.createFrameGrab(channel);

            int count = grab.getVideoTrack()
                    .getMeta()
                    .getTotalFrames();

            double duration = grab.getVideoTrack()
                    .getMeta()
                    .getTotalDuration();

            assertEquals(3, count);
            assertEquals(3.0 / 30.0, duration, 1e-6);
            assertEquals(30.0, count / duration, 1e-6);
        }
    }

    // Helpers

    private VideoWriter writer(Path output) {
        return new VideoWriter(
                output.toString(), SIZE, SIZE, 30, 100
        );
    }

    private long[][] solid(long value) {
        return new long[][]{
                {value, value},
                {value, value}
        };
    }

    private List<BufferedImage> decode(Path file) throws Exception {
        List<BufferedImage> frames = new ArrayList<>();

        try (SeekableByteChannel channel =
                     NIOUtils.readableChannel(file.toFile())) {

            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            Picture picture;

            while ((picture = grab.getNativeFrame()) != null) {
                frames.add(AWTUtil.toBufferedImage(picture));
            }
        }

        return frames;
    }

    private void assertGray(
            BufferedImage image, int x, int y, int expected) {

        int rgb = image.getRGB(x, y);

        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;

        for (int actual : new int[]{red, green, blue}) {
            assertTrue(
                    Math.abs(actual - expected) <= COLOR_TOLERANCE,
                    "At (" + x + ", " + y + "): expected gray "
                            + expected + ", but channel value was "
                            + actual
            );
        }
    }
}