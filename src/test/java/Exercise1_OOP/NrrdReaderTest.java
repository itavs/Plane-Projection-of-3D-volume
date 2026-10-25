package Exercise1_OOP;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class NrrdReaderTest {

    @TempDir
    Path tempDirectory;

    private final NrrdReader reader = new NrrdReader();

    // Existing dataset checks

    private NrrdReader.Volume readConstantVolume() throws IOException {
        return reader.read(
                Path.of("data", "constant_100_10x10x10.nrrd").toString()
        );
    }

    @Test
    void readsCorrectDimensions() throws IOException {
        NrrdReader.Volume volume = readConstantVolume();

        assertEquals(10, volume.getSizeX());
        assertEquals(10, volume.getSizeY());
        assertEquals(10, volume.getSizeZ());
    }

    @Test
    void readsAllThousandValuesAs100() throws IOException {
        NrrdReader.Volume volume = readConstantVolume();

        for (int z = 0; z < 10; z++) {
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    assertEquals(
                            100L,
                            volume.get(x, y, z),
                            "Unexpected value at (" + x
                                    + ", " + y + ", " + z + ")"
                    );
                }
            }
        }
    }

    @Test
    void returnsZeroOutsideEachVolumeBoundary() throws IOException {
        NrrdReader.Volume volume = readConstantVolume();

        int[][] outsidePoints = {
                {-1, 5, 5},
                {10, 5, 5},
                {5, -1, 5},
                {5, 10, 5},
                {5, 5, -1},
                {5, 5, 10}
        };

        for (int[] point : outsidePoints) {
            int x = point[0];
            int y = point[1];
            int z = point[2];

            assertFalse(volume.contains(x, y, z));
            assertEquals(
                    0L,
                    volume.get(x, y, z),
                    "Expected zero at (" + x
                            + ", " + y + ", " + z + ")"
            );
        }
    }

    // Axis order and integer decoding

    @Test
    void preservesAxisOrderInNonCubicVolume() throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // NRRD stores the first axis (x) fastest.
        // Values are written as little-endian unsigned 16-bit integers.
        for (int z = 0; z < 5; z++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 3; x++) {
                    int value = x + 10 * y + 100 * z;
                    payload.write(value & 0xFF);
                    payload.write((value >>> 8) & 0xFF);
                }
            }
        }

        Path file = writeFixture(
                header("uint16", "3 4 5", "raw", "little"),
                payload.toByteArray()
        );

        NrrdReader.Volume volume = reader.read(file.toString());

        assertEquals(3, volume.getSizeX());
        assertEquals(4, volume.getSizeY());
        assertEquals(5, volume.getSizeZ());

        for (int z = 0; z < 5; z++) {
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 3; x++) {
                    assertEquals(
                            x + 10L * y + 100L * z,
                            volume.get(x, y, z),
                            "Incorrect axis mapping at (" + x
                                    + ", " + y + ", " + z + ")"
                    );
                }
            }
        }
    }

    @Test
    void readsUnsigned8BitBoundaryValues() throws IOException {
        assertValues(
                "uint8", null,
                bytes(0x00, 0x7F, 0x80, 0xFF),
                0L, 127L, 128L, 255L
        );
    }

    @Test
    void readsUnsigned16BitValuesInBothByteOrders() throws IOException {
        long[] expected = {0L, 32767L, 32768L, 65535L};

        assertValues(
                "uint16", "little",
                bytes(0x00, 0x00, 0xFF, 0x7F,
                        0x00, 0x80, 0xFF, 0xFF),
                expected
        );

        assertValues(
                "uint16", "big",
                bytes(0x00, 0x00, 0x7F, 0xFF,
                        0x80, 0x00, 0xFF, 0xFF),
                expected
        );
    }

    @Test
    void readsUnsigned32BitValuesInBothByteOrders() throws IOException {
        long[] expected = {
                0L, 2147483647L, 2147483648L, 4294967295L
        };

        assertValues(
                "uint32", "little",
                bytes(
                        0x00, 0x00, 0x00, 0x00,
                        0xFF, 0xFF, 0xFF, 0x7F,
                        0x00, 0x00, 0x00, 0x80,
                        0xFF, 0xFF, 0xFF, 0xFF
                ),
                expected
        );

        assertValues(
                "uint32", "big",
                bytes(
                        0x00, 0x00, 0x00, 0x00,
                        0x7F, 0xFF, 0xFF, 0xFF,
                        0x80, 0x00, 0x00, 0x00,
                        0xFF, 0xFF, 0xFF, 0xFF
                ),
                expected
        );
    }

    @Test
    void readsSignedIntegerValues() throws IOException {
        assertValues(
                "int8", null,
                bytes(0x80, 0xFF, 0x00, 0x7F),
                -128L, -1L, 0L, 127L
        );

        assertValues(
                "int16", "little",
                bytes(0x00, 0x80, 0xFF, 0xFF,
                        0x00, 0x00, 0xFF, 0x7F),
                -32768L, -1L, 0L, 32767L
        );

        assertValues(
                "int16", "big",
                bytes(0x80, 0x00, 0xFF, 0xFF,
                        0x00, 0x00, 0x7F, 0xFF),
                -32768L, -1L, 0L, 32767L
        );

        assertValues(
                "int32", "little",
                bytes(
                        0x00, 0x00, 0x00, 0x80,
                        0xFF, 0xFF, 0xFF, 0xFF,
                        0x00, 0x00, 0x00, 0x00,
                        0xFF, 0xFF, 0xFF, 0x7F
                ),
                -2147483648L, -1L, 0L, 2147483647L
        );

        assertValues(
                "int32", "big",
                bytes(
                        0x80, 0x00, 0x00, 0x00,
                        0xFF, 0xFF, 0xFF, 0xFF,
                        0x00, 0x00, 0x00, 0x00,
                        0x7F, 0xFF, 0xFF, 0xFF
                ),
                -2147483648L, -1L, 0L, 2147483647L
        );
    }

    // Encodings and valid header variations

    @Test
    void readsEquivalentRawAndGzipData() throws IOException {
        byte[] payload = bytes(0, 127, 128, 255);
        long[] expected = {0L, 127L, 128L, 255L};

        for (String encoding : new String[]{"raw", "gzip", "gz"}) {
            byte[] stored = encoding.equals("raw")
                    ? payload : gzip(payload);

            Path file = writeFixture(
                    header("uint8", "4 1 1", encoding, null),
                    stored
            );

            NrrdReader.Volume volume = reader.read(file.toString());

            for (int x = 0; x < expected.length; x++) {
                assertEquals(
                        expected[x], volume.get(x, 0, 0), encoding
                );
            }
        }
    }

    @Test
    void acceptsCommentsCustomFieldsAndWindowsLineEndings()
            throws IOException {

        String text = "NRRD0005\r\n"
                + "# Test comment\r\n"
                + "description:=test volume\r\n"
                + "type: unsigned char\r\n"
                + "dimension: 3\r\n"
                + "sizes: 1 1 1\r\n"
                + "encoding: raw\r\n"
                + "\r\n";

        Path file = writeFixture(text, bytes(100));

        assertEquals(100L, reader.read(file.toString()).get(0, 0, 0));
    }

    // Invalid input

    @Test
    void rejectsInvalidDimensions() throws IOException {
        String valid = header("uint8", "1 1 1", "raw", null);

        assertRejected(
                valid.replace("dimension: 3", "dimension: 2"),
                bytes(100)
        );

        for (String sizes : new String[]{
                "0 1 1", "-1 1 1", "1024 1 1",
                "1 1", "1 1 1 1", "x 1 1"
        }) {
            assertRejected(
                    header("uint8", sizes, "raw", null),
                    bytes(100)
            );
        }
    }

    @Test
    void rejectsMissingRequiredFields() throws IOException {
        String valid = header("uint16", "1 1 1", "raw", "little");

        for (String field : new String[]{
                "dimension: 3\n",
                "sizes: 1 1 1\n",
                "type: uint16\n",
                "encoding: raw\n",
                "endian: little\n"
        }) {
            assertRejected(valid.replace(field, ""), bytes(100, 0));
        }
    }

    @Test
    void rejectsMalformedHeaders() throws IOException {
        String valid = header("uint8", "1 1 1", "raw", null);

        assertRejected(
                valid.replace("NRRD0005", "INVALID"),
                bytes(100)
        );

        assertRejected(
                valid.replace("sizes: 1 1 1", "sizes 1 1 1"),
                bytes(100)
        );

        assertRejected(
                valid.replace(
                        "dimension: 3\n",
                        "dimension: 3\ndimension: 3\n"
                ),
                bytes(100)
        );

        // No blank line separating header and payload.
        assertRejected(
                valid.substring(0, valid.length() - 1),
                new byte[0]
        );
    }

    @Test
    void rejectsUnsupportedFormats() throws IOException {
        assertRejected(
                header("float", "1 1 1", "raw", "little"),
                bytes(0, 0, 0, 0)
        );

        assertRejected(
                header("uint8", "1 1 1", "ascii", null),
                bytes(100)
        );

        assertRejected(
                header("uint16", "1 1 1", "raw", "unknown"),
                bytes(100, 0)
        );

        String valid = header("uint8", "1 1 1", "raw", null);

        for (String extra : new String[]{
                "data file: external.raw",
                "byte skip: 1",
                "line skip: 1"
        }) {
            assertRejected(
                    valid.replace("\n\n", "\n" + extra + "\n\n"),
                    bytes(100)
            );
        }
    }

    @Test
    void rejectsIncompleteRawAndGzipPayloads() throws IOException {
        // The header requires two voxels, but only one is supplied.
        assertRejected(
                header("uint8", "2 1 1", "raw", null),
                bytes(100)
        );

        assertRejected(
                header("uint8", "2 1 1", "gzip", null),
                gzip(bytes(100))
        );
    }

    @Test
    void rejectsInvalidGzipStream() throws IOException {
        assertRejected(
                header("uint8", "1 1 1", "gzip", null),
                bytes(1, 2, 3, 4)
        );
    }

    @Test
    void rejectsMissingFile() {
        Path missing = tempDirectory.resolve("missing.nrrd");

        assertThrows(
                IOException.class,
                () -> reader.read(missing.toString())
        );
    }

    @Test
    void rejectsGzipWithCorruptedChecksum() throws IOException {
        byte[] compressed = gzip(bytes(100));

        // Corrupt the checksum stored in the gzip trailer.
        compressed[compressed.length - 8] ^= 1;

        assertRejected(
                header("uint8", "1 1 1", "gzip", null),
                compressed
        );
    }

    @Test
    void rejectsGzipWithCorruptedSize() throws IOException {
        byte[] compressed = gzip(bytes(100));

        // Corrupt the original-size field stored in the trailer.
        compressed[compressed.length - 4] ^= 1;

        assertRejected(
                header("uint8", "1 1 1", "gzip", null),
                compressed
        );
    }

    @Test
    void rejectsGzipWithTruncatedTrailer() throws IOException {
        byte[] compressed = gzip(bytes(100));

        // Remove between one and eight trailer bytes.
        for (int missing = 1; missing <= 8; missing++) {
            byte[] truncated = java.util.Arrays.copyOf(
                    compressed,
                    compressed.length - missing
            );

            assertRejected(
                    header("uint8", "1 1 1", "gzip", null),
                    truncated
            );
        }
    }

    // Fixture helpers

    private String header(
            String type, String sizes, String encoding, String endian) {

        return "NRRD0005\n"
                + "type: " + type + "\n"
                + "dimension: 3\n"
                + "sizes: " + sizes + "\n"
                + (endian == null ? "" : "endian: " + endian + "\n")
                + "encoding: " + encoding + "\n\n";
    }

    private Path writeFixture(String header, byte[] payload)
            throws IOException {

        Path file = Files.createTempFile(
                tempDirectory, "volume-", ".nrrd"
        );

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.write(header.getBytes(StandardCharsets.US_ASCII));
        content.write(payload);
        Files.write(file, content.toByteArray());

        return file;
    }

    private byte[] bytes(int... values) {
        byte[] result = new byte[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }

        return result;
    }

    private byte[] gzip(byte[] payload) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(payload);
        }

        return compressed.toByteArray();
    }

    private void assertValues(
            String type,
            String endian,
            byte[] payload,
            long... expected) throws IOException {

        Path file = writeFixture(
                header(type, expected.length + " 1 1", "raw", endian),
                payload
        );

        NrrdReader.Volume volume = reader.read(file.toString());

        for (int x = 0; x < expected.length; x++) {
            assertEquals(
                    expected[x],
                    volume.get(x, 0, 0),
                    type + ", endian=" + endian + ", x=" + x
            );
        }
    }

    private void assertRejected(String header, byte[] payload)
            throws IOException {

        Path file = writeFixture(header, payload);

        IOException error = assertThrows(
                IOException.class,
                () -> reader.read(file.toString())
        );

        assertNotNull(error.getMessage());
        assertFalse(error.getMessage().isBlank());
    }
}