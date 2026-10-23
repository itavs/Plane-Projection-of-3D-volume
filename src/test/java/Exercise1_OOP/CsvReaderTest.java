package Exercise1_OOP;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {

    @TempDir
    Path tempDirectory;

    private final CsvReader reader = new CsvReader();

    private static final String THREE_POINTS =
            "1,2,3,4,5,6,7,8,9";

    private static final String FOUR_POINTS =
            "10,11,12,13,14,15,16,17,18,19,20,21";

    @Test
    void readsThreePointsWithCorrectCoordinates() throws IOException {
        ArrayList<SliceRow> rows = readText(THREE_POINTS);

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getLineNumber());

        assertPoints(
                rows.get(0),
                1, 2, 3,
                4, 5, 6,
                7, 8, 9
        );
    }

    @Test
    void readsFourPointsWithCorrectCoordinates() throws IOException {
        ArrayList<SliceRow> rows = readText(FOUR_POINTS);

        assertEquals(1, rows.size());

        assertPoints(
                rows.get(0),
                10, 11, 12,
                13, 14, 15,
                16, 17, 18,
                19, 20, 21
        );
    }

    @Test
    void preservesRowOrderAndLineNumbers() throws IOException {
        ArrayList<SliceRow> rows = readText(
                THREE_POINTS + "\n" + FOUR_POINTS + "\n" + THREE_POINTS
        );

        assertEquals(3, rows.size());

        for (int i = 0; i < rows.size(); i++) {
            assertEquals(i + 1, rows.get(i).getLineNumber());
        }

        assertPoints(rows.get(0), 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertPoints(rows.get(1),
                10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
        assertPoints(rows.get(2), 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    void acceptsWhitespaceNegativeAndFractionalCoordinates()
            throws IOException {

        ArrayList<SliceRow> rows = readText(
                "  -1.5 , 2.25 , 3 , 4 , -5 , 6.5 , 7 , 8 , 9  "
        );

        assertEquals(1, rows.size());
        assertPoints(rows.get(0),
                -1.5, 2.25, 3, 4, -5, 6.5, 7, 8, 9);
    }

    @Test
    void acceptsBomAndWindowsLineEndings() throws IOException {
        ArrayList<SliceRow> rows = readText(
                "\uFEFF" + THREE_POINTS + "\r\n"
                        + FOUR_POINTS + "\r\n"
        );

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getLineNumber());
        assertEquals(2, rows.get(1).getLineNumber());

        assertPoints(rows.get(0), 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertPoints(rows.get(1),
                10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
    }

    @Test
    void rejectsIncorrectFieldCounts() throws IOException {
        String[] invalidRows = {
                "1,2,3",
                "1,2,3,4,5,6,7,8",
                "1,2,3,4,5,6,7,8,9,10",
                "1,2,3,4,5,6,7,8,9,10,11",
                "1,2,3,4,5,6,7,8,9,10,11,12,13"
        };

        for (String row : invalidRows) {
            assertTrue(
                    readText(row).isEmpty(),
                    "Expected rejection of: " + row
            );
        }
    }

    @Test
    void rejectsEmptyNonnumericAndQuotedFields() throws IOException {
        String[] invalidRows = {
                ",2,3,4,5,6,7,8,9",
                "1,2,3,4,,6,7,8,9",
                "1,2,3,4,5,6,7,8,",
                "1,2,3,4,   ,6,7,8,9",
                "hello,2,3,4,5,6,7,8,9",
                "\"1\",2,3,4,5,6,7,8,9"
        };

        for (String row : invalidRows) {
            assertTrue(
                    readText(row).isEmpty(),
                    "Expected rejection of: " + row
            );
        }
    }

    @Test
    void rejectsNonFiniteCoordinatesInEveryField() throws IOException {
        String[] invalidValues = {
                "NaN", "Infinity", "-Infinity", "1e309"
        };

        for (String invalid : invalidValues) {
            for (int field = 0; field < 9; field++) {
                String[] fields = THREE_POINTS.split(",");
                fields[field] = invalid;

                assertTrue(
                        readText(String.join(",", fields)).isEmpty(),
                        "Expected rejection of " + invalid
                                + " in field " + (field + 1)
                );
            }
        }
    }

    @Test
    void skipsInvalidRowsAndReportsOriginalLineNumbers()
            throws IOException {

        String csv = THREE_POINTS + "\n"
                + "invalid\n"
                + "\n"
                + FOUR_POINTS + "\n"
                + "1,2,3,4,5,6,7,8,NaN";

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalErrorStream = System.err;

        ArrayList<SliceRow> rows;

        try (PrintStream captureStream = new PrintStream(
                captured, true, StandardCharsets.UTF_8)) {

            System.setErr(captureStream);

            try {
                rows = readText(csv);
            } finally {
                System.setErr(originalErrorStream);
            }
        }

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getLineNumber());
        assertEquals(4, rows.get(1).getLineNumber());

        assertPoints(rows.get(0), 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertPoints(rows.get(1),
                10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);

        String[] messages = captured
                .toString(StandardCharsets.UTF_8)
                .lines()
                .toArray(String[]::new);

        assertEquals(3, messages.length);

        int[] expectedLines = {2, 3, 5};

        for (int i = 0; i < expectedLines.length; i++) {
            String prefix = "Skipping CSV line "
                    + expectedLines[i] + ": ";

            assertTrue(messages[i].startsWith(prefix));
            assertFalse(
                    messages[i].substring(prefix.length()).isBlank(),
                    "The error should explain why the row was skipped"
            );
        }
    }

    @Test
    void returnsEmptyListForEmptyOrEntirelyInvalidFiles()
            throws IOException {

        assertTrue(readText("").isEmpty());
        assertTrue(readText("\n   \ninvalid\n1,2,3").isEmpty());
    }

    @Test
    void leavesGeometryValidationToTheGeometryValidator()
            throws IOException {

        // Repeated points are numeric input, even though they
        // do not define a valid triangle.
        ArrayList<SliceRow> rows = readText(
                "1,1,1,1,1,1,1,1,1"
        );

        assertEquals(1, rows.size());
        assertPoints(rows.get(0), 1, 1, 1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    void throwsIOExceptionForMissingFile() {
        Path missing = tempDirectory.resolve("missing.csv");

        assertThrows(
                IOException.class,
                () -> reader.read(missing.toString())
        );
    }

    // Helpers: create temporary input and compare parsed coordinates.

    private ArrayList<SliceRow> readText(String content)
            throws IOException {

        Path file = Files.createTempFile(
                tempDirectory, "slices-", ".csv"
        );

        Files.writeString(file, content, StandardCharsets.UTF_8);

        return reader.read(file.toString());
    }

    private void assertPoints(SliceRow row, double... expected) {
        Point3D[] points = row.getPoints();

        assertEquals(expected.length / 3, points.length);

        for (int i = 0; i < points.length; i++) {
            assertEquals(expected[i * 3], points[i].getX(), 1e-12);
            assertEquals(expected[i * 3 + 1], points[i].getY(), 1e-12);
            assertEquals(expected[i * 3 + 2], points[i].getZ(), 1e-12);
        }
    }
}