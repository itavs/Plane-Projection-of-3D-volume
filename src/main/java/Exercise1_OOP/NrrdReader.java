package Exercise1_OOP;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class NrrdReader {

    public Volume read(String filePath) throws IOException {
        try (InputStream file = new BufferedInputStream(
                Files.newInputStream(Path.of(filePath)))) {

            Map<String, String> header = readHeader(file);

            if (!"3".equals(header.get("dimension"))) {
                throw new IOException("Expected a 3D NRRD file.");
            }

            int[] sizes = parseSizes(header.get("sizes"));
            DataType type = parseType(header.get("type"));

            if (header.containsKey("data file")
                    || header.containsKey("datafile")) {
                throw new IOException(
                        "External NRRD data files are not supported.");
            }

            checkNoSkip(header, "byte skip");
            checkNoSkip(header, "line skip");

            boolean littleEndian = true;

            if (type.bytes > 1) {
                String endian = header.get("endian");

                if ("little".equals(endian)) {
                    littleEndian = true;
                } else if ("big".equals(endian)) {
                    littleEndian = false;
                } else {
                    throw new IOException(
                            "Missing or unsupported endian field.");
                }
            }

            long byteCount = (long) sizes[0]
                    * sizes[1] * sizes[2] * type.bytes;

            if (byteCount > Integer.MAX_VALUE - 8L) {
                throw new IOException(
                        "Volume is too large for this reader's storage.");
            }

            InputStream decoded;
            String encoding = header.get("encoding");

            if ("gzip".equals(encoding) || "gz".equals(encoding)) {
                decoded = new GZIPInputStream(file);
            } else if ("raw".equals(encoding)) {
                decoded = file;
            } else {
                throw new IOException(
                        "Unsupported NRRD encoding: " + encoding);
            }

            try (DataInputStream data = new DataInputStream(
                    new BufferedInputStream(decoded))) {

                byte[] values = new byte[(int) byteCount];


                data.readFully(values);

                return new Volume(
                        sizes[0], sizes[1], sizes[2],
                        type.bytes, type.signed,
                        littleEndian, values);
            }
        }
    }

    private Map<String, String> readHeader(InputStream input)
            throws IOException {

        String firstLine = readLine(input);

        if (firstLine == null
                || !firstLine.matches("NRRD000[1-5]")) {
            throw new IOException("Invalid NRRD header.");
        }

        Map<String, String> header = new HashMap<>();
        String line;

        while ((line = readLine(input)) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                return header;
            }


            if (line.startsWith("#") || line.contains(":=")) {
                continue;
            }

            int separator = line.indexOf(':');

            if (separator <= 0) {
                throw new IOException("Invalid header line: " + line);
            }

            String key = line.substring(0, separator)
                    .trim().toLowerCase(Locale.ROOT);

            String value = line.substring(separator + 1).trim();

            if (header.putIfAbsent(key, value) != null) {
                throw new IOException("Duplicate header field: " + key);
            }
        }

        throw new IOException("Missing empty line after NRRD header.");
    }

    private String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;

        while ((value = input.read()) != -1) {
            if (value == '\n') {
                break;
            }

            if (value != '\r') {
                line.write(value);
            }
        }

        if (value == -1 && line.size() == 0) {
            return null;
        }

        return new String(
                line.toByteArray(), StandardCharsets.US_ASCII);
    }

    private int[] parseSizes(String value) throws IOException {
        if (value == null) {
            throw new IOException("Missing sizes field.");
        }

        String[] parts = value.trim().split("\\s+");

        if (parts.length != 3) {
            throw new IOException("Expected exactly three dimensions.");
        }

        int[] sizes = new int[3];

        for (int i = 0; i < sizes.length; i++) {
            try {
                sizes[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid dimension size.", e);
            }

            if (sizes[i] < 1 || sizes[i] >= 1024) {
                throw new IOException(
                        "Each dimension must be between 1 and 1023.");
            }
        }

        return sizes;
    }

    private DataType parseType(String value) throws IOException {
        if (value == null) {
            throw new IOException("Missing type field.");
        }

        String type = value.toLowerCase(Locale.ROOT)
                .trim().replaceAll("\\s+", " ");

        switch (type) {
            case "uchar":
            case "unsigned char":
            case "uint8":
            case "uint8_t":
                return new DataType(1, false);

            case "ushort":
            case "unsigned short":
            case "unsigned short int":
            case "uint16":
            case "uint16_t":
                return new DataType(2, false);

            case "uint":
            case "unsigned int":
            case "uint32":
            case "uint32_t":
                return new DataType(4, false);

            case "char":
            case "signed char":
            case "int8":
            case "int8_t":
                return new DataType(1, true);

            case "short":
            case "short int":
            case "signed short":
            case "signed short int":
            case "int16":
            case "int16_t":
                return new DataType(2, true);

            case "int":
            case "signed int":
            case "int32":
            case "int32_t":
                return new DataType(4, true);

            default:
                throw new IOException(
                        "Unsupported NRRD data type: " + value);
        }
    }

    private void checkNoSkip(Map<String, String> header, String field)
            throws IOException {

        String value = header.get(field);

        if (value != null && !"0".equals(value)) {
            throw new IOException(
                    "Non-zero " + field + " is not supported.");
        }
    }

    private static class DataType {
        private final int bytes;
        private final boolean signed;

        private DataType(int bytes, boolean signed) {
            this.bytes = bytes;
            this.signed = signed;
        }
    }

    public static class Volume {
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;

        private final int bytesPerValue;
        private final boolean signed;
        private final boolean littleEndian;
        private final byte[] data;

        private Volume(
                int sizeX, int sizeY, int sizeZ,
                int bytesPerValue, boolean signed,
                boolean littleEndian, byte[] data) {

            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.bytesPerValue = bytesPerValue;
            this.signed = signed;
            this.littleEndian = littleEndian;
            this.data = data;
        }

        public int getSizeX() {
            return sizeX;
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeZ() {
            return sizeZ;
        }

        public boolean contains(int x, int y, int z) {
            return x >= 0 && x < sizeX
                    && y >= 0 && y < sizeY
                    && z >= 0 && z < sizeZ;
        }

        public long get(int x, int y, int z) {
            if (!contains(x, y, z)) {
                return 0;
            }

            int offset = ((z * sizeY + y) * sizeX + x)
                    * bytesPerValue;

            long value = 0;

            for (int i = 0; i < bytesPerValue; i++) {
                int shift = littleEndian
                        ? 8 * i
                        : 8 * (bytesPerValue - 1 - i);

                value |= (data[offset + i] & 0xFFL) << shift;
            }

            int bits = bytesPerValue * 8;

            if (signed && (value & (1L << (bits - 1))) != 0) {
                value -= 1L << bits;
            }

            return value;
        }
    }
}