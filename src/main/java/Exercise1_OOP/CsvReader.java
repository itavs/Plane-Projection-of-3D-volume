package Exercise1_OOP;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class CsvReader {

    public ArrayList<SliceRow> read(String filePath) throws IOException {
        ArrayList<SliceRow> rows = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(
                Path.of(filePath), StandardCharsets.UTF_8)) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                try {
                    SliceRow row = parseRow(line, lineNumber);
                    rows.add(row);
                } catch (IllegalArgumentException e) {
                    System.err.println(
                            "Skipping CSV line " + lineNumber
                                    + ": " + e.getMessage());
                }
            }
        }

        return rows;
    }

    private SliceRow parseRow(String line, int lineNumber) {
        if (line.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty line.");
        }

        String[] fields = line.split(",", -1);

        if (fields.length != 9 && fields.length != 12) {
            throw new IllegalArgumentException(
                    "Expected 9 or 12 coordinates, but found "
                            + fields.length + ".");
        }

        int pointCount = fields.length / 3;
        Point3D[] points = new Point3D[pointCount];

        for (int i = 0; i < pointCount; i++) {
            int start = i * 3;

            double x = parseCoordinate(fields[start], start + 1);
            double y = parseCoordinate(fields[start + 1], start + 2);
            double z = parseCoordinate(fields[start + 2], start + 3);

            points[i] = new Point3D(x, y, z);
        }

        return new SliceRow(lineNumber, points);
    }

    private double parseCoordinate(String field, int fieldNumber) {
        double value;

        try {
            value = Double.parseDouble(field.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Field " + fieldNumber
                            + " must contain a number.", e);
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Field " + fieldNumber
                            + " must contain a finite number.");
        }

        return value;
    }
}