package Exercise1_OOP;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FrameGeneratorTest {

    @TempDir
    Path tempDirectory;

    private final FrameGenerator generator = new FrameGenerator();
    private final GeometryValidator validator = new GeometryValidator();

    @Test
    void samplesRectangleWithCorrectDimensionsAndRowOrder()
            throws IOException {

        long[][] actual = generate(
                varyingVolume(),
                p(1, 1, 2), p(3, 1, 2),
                p(1, 3, 2), p(3, 3, 2)
        );

        // The starting base at y=1 is skipped.
        // The first scan line is y=2; the final one is y=3.
        assertFrame(new long[][]{
                {221, 222, 223},
                {231, 232, 233}
        }, actual);
    }

    @Test
    void samplesTriangleWithExpectedValuesAndPadding()
            throws IOException {

        // Equal legs of length 5; base width 6.
        long[][] actual = generate(
                varyingVolume(),
                p(3, 1, 2),
                p(0, 5, 2),
                p(6, 5, 2)
        );

        assertFrame(new long[][]{
                {0, 0, 212, 213, 0, 0, 0},
                {0, 221, 222, 223, 0, 0, 0},
                {0, 231, 232, 233, 234, 0, 0},
                {240, 241, 242, 243, 244, 0, 0},
                {250, 251, 252, 253, 254, 255, 256}
        }, actual);
    }

    @Test
    void centersWideningTrapezoidOnConstantVolume()
            throws IOException {

        long[][] actual = generate(
                constantVolume(),
                p(4, 1, 2), p(6, 1, 2),
                p(1, 5, 2), p(9, 5, 2)
        );

        assertFrame(new long[][]{
                {0, 0, 100, 100, 100, 100, 0, 0, 0},
                {0, 100, 100, 100, 100, 100, 0, 0, 0},
                {0, 100, 100, 100, 100, 100, 100, 0, 0},
                {100, 100, 100, 100, 100, 100, 100, 0, 0},
                {100, 100, 100, 100, 100, 100, 100, 100, 100}
        }, actual);
    }

    @Test
    void centersNarrowingTrapezoidOnConstantVolume()
            throws IOException {

        long[][] actual = generate(
                constantVolume(),
                p(1, 1, 2), p(9, 1, 2),
                p(4, 5, 2), p(6, 5, 2)
        );

        assertFrame(new long[][]{
                {100, 100, 100, 100, 100, 100, 100, 0, 0},
                {0, 100, 100, 100, 100, 100, 100, 0, 0},
                {0, 100, 100, 100, 100, 100, 0, 0, 0},
                {0, 0, 100, 100, 100, 100, 0, 0, 0},
                {0, 0, 0, 100, 100, 100, 0, 0, 0}
        }, actual);
    }

    @Test
    void returnsZeroAtBothXBoundaries() throws IOException {
        long[][] actual = generate(
                constantVolume(),
                p(-1, 1, 2), p(10, 1, 2),
                p(-1, 2, 2), p(10, 2, 2)
        );

        // x=-1 and x=10 are outside; x=0 through x=9 are inside.
        assertFrame(new long[][]{
                {0, 100, 100, 100, 100, 100,
                        100, 100, 100, 100, 100, 0}
        }, actual);
    }

    @Test
    void checksYAndZVolumeBoundaries() throws IOException {
        NrrdReader.Volume volume = constantVolume();

        // Scan lines at y=-1, 0, 1.
        assertFrame(new long[][]{
                {0, 0},
                {100, 100},
                {100, 100}
        }, generate(
                volume,
                p(1, -2, 2), p(2, -2, 2),
                p(1, 1, 2), p(2, 1, 2)
        ));

        // Scan lines at y=9 and y=10.
        assertFrame(new long[][]{
                {100, 100},
                {0, 0}
        }, generate(
                volume,
                p(1, 8, 2), p(2, 8, 2),
                p(1, 10, 2), p(2, 10, 2)
        ));

        for (double z : new double[]{-0.25, 0, 9.75, 10}) {
            long expected = (z == 0 || z == 9.75) ? 100 : 0;

            assertFrame(new long[][]{
                    {expected, expected}
            }, generate(
                    volume,
                    p(1, 1, z), p(2, 1, z),
                    p(1, 2, z), p(2, 2, z)
            ));
        }
    }

    @Test
    void rejectsNegativeFractionalSampleBeforeIntegerConversion()
            throws IOException {

        long[][] actual = generate(
                constantVolume(),
                p(-0.25, 1, 2), p(1.75, 1, 2),
                p(-0.25, 2, 2), p(1.75, 2, 2)
        );

        // Samples occur at x=-0.25, 0.75, 1.75.
        assertFrame(new long[][]{
                {0, 100, 100}
        }, actual);
    }

    @Test
    void appliesFloorSamplingAndClampsFractionalFinalStep()
            throws IOException {

        long[][] actual = generate(
                varyingVolume(),
                p(1.25, 1.25, 2.5), p(3.75, 1.25, 2.5),
                p(1.25, 3.75, 2.5), p(3.75, 3.75, 2.5)
        );

        // Leg length 2.5 -> 3 rows.
        // Base width 2.5 -> 4 columns, of which 3 are sampled.
        // Sampled y values: 2.25, 3.25, 3.75.
        assertFrame(new long[][]{
                {221, 222, 223, 0},
                {231, 232, 233, 0},
                {231, 232, 233, 0}
        }, actual);
    }

    @Test
    void samplesTiltedRectangleAcrossDifferentZLayers()
            throws IOException {

        // Each leg changes y by 3 and z by 4: length 5.
        long[][] actual = generate(
                varyingVolume(),
                p(1, 1, 1), p(3, 1, 1),
                p(1, 4, 5), p(3, 4, 5)
        );

        assertFrame(new long[][]{
                {111, 112, 113},
                {221, 222, 223},
                {321, 322, 323},
                {431, 432, 433},
                {541, 542, 543}
        }, actual);
    }

    @Test
    void returnsBlackFrameForShapeEntirelyOutsideVolume()
            throws IOException {

        assertFrame(new long[][]{
                {0, 0, 0},
                {0, 0, 0}
        }, generate(
                constantVolume(),
                p(20, 1, 2), p(22, 1, 2),
                p(20, 3, 2), p(22, 3, 2)
        ));
    }

    @Test
    void rejectsFrameExceedingPixelLimit() throws IOException {
        NrrdReader.Volume volume = constantVolume();

        SliceRow row = new SliceRow(1, new Point3D[]{
                p(0, 0, 0), p(4000, 0, 0),
                p(0, 4000, 0), p(4000, 4000, 0)
        });

        validator.validate(row);

        // Requested size: 4000 rows × 4001 columns.
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(row, volume)
        );

        assertNotNull(error.getMessage());
        assertTrue(error.getMessage().contains("pixel limit"));
    }

    @Test
    void rejectsMissingInputsAndIncorrectPointCount()
            throws IOException {

        NrrdReader.Volume volume = constantVolume();

        SliceRow valid = new SliceRow(1, new Point3D[]{
                p(1, 1, 2), p(3, 1, 2),
                p(1, 3, 2), p(3, 3, 2)
        });

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(null, volume)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(valid, null)
        );

        SliceRow invalid = new SliceRow(
                1, new Point3D[]{p(0, 0, 0), p(1, 0, 0)}
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(invalid, volume)
        );
    }

    // Helpers

    private Point3D p(double x, double y, double z) {
        return new Point3D(x, y, z);
    }

    private long[][] generate(
            NrrdReader.Volume volume, Point3D... points) {

        SliceRow row = new SliceRow(1, points);

        // Match the application workflow:
        // validate geometry before generating a frame.
        validator.validate(row);

        return generator.generate(row, volume);
    }

    private NrrdReader.Volume constantVolume() throws IOException {
        return new NrrdReader().read(
                Path.of("data", "constant_100_10x10x10.nrrd").toString()
        );
    }

    private NrrdReader.Volume varyingVolume() throws IOException {
        String header = "NRRD0005\n"
                + "type: uint16\n"
                + "dimension: 3\n"
                + "sizes: 10 10 10\n"
                + "endian: little\n"
                + "encoding: raw\n\n";

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.write(header.getBytes(StandardCharsets.US_ASCII));

        for (int z = 0; z < 10; z++) {
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    int value = x + 10 * y + 100 * z;
                    content.write(value & 0xFF);
                    content.write((value >>> 8) & 0xFF);
                }
            }
        }

        Path file = Files.createTempFile(
                tempDirectory, "varying-volume-", ".nrrd"
        );

        Files.write(file, content.toByteArray());

        return new NrrdReader().read(file.toString());
    }

    private void assertFrame(long[][] expected, long[][] actual) {
        assertEquals(expected.length, actual.length, "Frame height");

        for (int row = 0; row < expected.length; row++) {
            assertArrayEquals(
                    expected[row],
                    actual[row],
                    "Incorrect pixels in row " + row
            );
        }
    }
}