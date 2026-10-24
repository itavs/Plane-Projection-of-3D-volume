package Exercise1_OOP;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SyntheticVolumeGenerator {

    private static final int SIZE = 500;

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println(
                    "Expected 1 argument: <output.nrrd>");
            System.exit(2);
            return;
        }

        try {
            Path output = Path.of(args[0]);
            generate(output);

            System.out.println(
                    "NRRD saved to: " + output.toAbsolutePath());
            System.out.println("Dimensions: 500 x 500 x 500");
            System.out.println("Voxel type: unsigned 8-bit");
            System.out.println("Encoding: raw");
            System.out.println("Voxel data size: 125,000,000 bytes");

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Generation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void generate(Path output) throws IOException {
        String header =
                "NRRD0005\n"
                        + "# Deterministic synthetic volume, version 1\n"
                        + "type: uint8\n"
                        + "dimension: 3\n"
                        + "sizes: 500 500 500\n"
                        + "encoding: raw\n"
                        + "\n";

        /*
         * CREATE_NEW prevents accidental replacement of an
         * existing file. The parent directory must already exist.
         */
        try (OutputStream stream = new BufferedOutputStream(
                Files.newOutputStream(
                        output,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE))) {

            stream.write(header.getBytes(StandardCharsets.US_ASCII));

            byte[] row = new byte[SIZE];

            // NRRD storage order: X varies fastest, then Y, then Z.
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        row[x] = (byte) voxelValue(x, y, z);
                    }

                    stream.write(row);
                }
            }
        }
    }

    private static int voxelValue(int x, int y, int z) {
        // Background gradient: values from 0 to 127.
        int value = (x + 2 * y + 3 * z) % 128;

        int dx = x - 250;
        int dy = y - 250;
        int dz = z - 250;

        int squaredDistance = dx * dx + dy * dy + dz * dz;

        // A bright sphere containing a brighter central sphere.
        if (squaredDistance <= 80 * 80) {
            return 255;
        }

        if (squaredDistance <= 170 * 170) {
            return 200;
        }

        return value;
    }
}