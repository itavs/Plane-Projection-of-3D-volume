package Exercise1_OOP;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NrrdReaderTest {

    private NrrdReader.Volume volume;

    @BeforeEach
    void loadVolume() throws IOException {
        Path file = Path.of(
                "data",
                "constant_100_10x10x10.nrrd"
        );

        volume = new NrrdReader().read(file.toString());
    }

    @Test
    void readsCorrectDimensions() {
        assertEquals(10, volume.getSizeX());
        assertEquals(10, volume.getSizeY());
        assertEquals(10, volume.getSizeZ());
    }

    @Test
    void readsAllThousandValuesAs100() {
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
    void returnsZeroOutsideEachVolumeBoundary() {
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

            assertFalse(
                    volume.contains(x, y, z),
                    "Outside point reported as inside"
            );

            assertEquals(
                    0L,
                    volume.get(x, y, z),
                    "Expected zero at (" + x
                            + ", " + y + ", " + z + ")"
            );
        }
    }
}