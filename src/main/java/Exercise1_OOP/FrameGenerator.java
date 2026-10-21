package Exercise1_OOP;

public class FrameGenerator {

    private static final long MAX_FRAME_PIXELS = 16_000_000L;

    public long[][] generate(
            SliceRow row,
            NrrdReader.Volume volume) {

        if (row == null || volume == null) {
            throw new IllegalArgumentException(
                    "Row and volume must not be null.");
        }

        Point3D[] points = row.getPoints();

        if (points.length != 3 && points.length != 4) {
            throw new IllegalArgumentException(
                    "Expected 3 or 4 points.");
        }


        Point3D leftStart;
        Point3D rightStart;
        Point3D leftEnd;
        Point3D rightEnd;

        if (points.length == 3) {
            // Triangle: a, b, c
            leftStart = points[0];
            rightStart = points[0];
            leftEnd = points[1];
            rightEnd = points[2];
        } else {
            // Trapezoid: a1, a2, b, c
            leftStart = points[0];
            rightStart = points[1];
            leftEnd = points[2];
            rightEnd = points[3];
        }

        double legLength = distance(leftStart, leftEnd);

        double topWidth = distance(leftStart, rightStart);
        double bottomWidth = distance(leftEnd, rightEnd);
        double maxWidth = Math.max(topWidth, bottomWidth);

        if (!Double.isFinite(legLength)
                || !Double.isFinite(maxWidth)
                || legLength <= 0
                || maxWidth <= 0) {

            throw new IllegalArgumentException(
                    "Invalid frame dimensions.");
        }

        double requestedHeight = Math.ceil(legLength);

        double requestedWidth = Math.ceil(maxWidth) + 1;

        if (requestedHeight > MAX_FRAME_PIXELS
                || requestedWidth > MAX_FRAME_PIXELS
                || requestedHeight * requestedWidth > MAX_FRAME_PIXELS) {

            throw new IllegalArgumentException(
                    "Requested frame exceeds the pixel limit.");
        }

        int height = (int) requestedHeight;
        int width = (int) requestedWidth;


        long[][] frame = new long[height][width];

        for (int y = 0; y < height; y++) {


            double travelled = Math.min(y + 1.0, legLength);
            double t = travelled / legLength;

            double leftX = interpolate(
                    leftStart.getX(), leftEnd.getX(), t);
            double leftY = interpolate(
                    leftStart.getY(), leftEnd.getY(), t);
            double leftZ = interpolate(
                    leftStart.getZ(), leftEnd.getZ(), t);

            double rightX = interpolate(
                    rightStart.getX(), rightEnd.getX(), t);
            double rightY = interpolate(
                    rightStart.getY(), rightEnd.getY(), t);
            double rightZ = interpolate(
                    rightStart.getZ(), rightEnd.getZ(), t);

            double dx = rightX - leftX;
            double dy = rightY - leftY;
            double dz = rightZ - leftZ;

            double rowLength = Math.hypot(Math.hypot(dx, dy), dz);

            int startColumn = (int) Math.floor(
                    (maxWidth - rowLength) / 2.0);

            startColumn = Math.max(0, startColumn);

            if (rowLength == 0) {
                frame[y][startColumn] =
                        sample(volume, leftX, leftY, leftZ);
                continue;
            }

            double stepX = dx / rowLength;
            double stepY = dy / rowLength;
            double stepZ = dz / rowLength;

            int lastSample = (int) Math.floor(rowLength);

            for (int i = 0;
                 i <= lastSample && startColumn + i < width;
                 i++) {

                double x = leftX + i * stepX;
                double sampleY = leftY + i * stepY;
                double z = leftZ + i * stepZ;

                frame[y][startColumn + i] =
                        sample(volume, x, sampleY, z);
            }
        }

        return frame;
    }

    private long sample(
            NrrdReader.Volume volume,
            double x, double y, double z) {

        if (!Double.isFinite(x)
                || !Double.isFinite(y)
                || !Double.isFinite(z)
                || x < 0 || x >= volume.getSizeX()
                || y < 0 || y >= volume.getSizeY()
                || z < 0 || z >= volume.getSizeZ()) {

            return 0;
        }

        return volume.get(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z));
    }

    private double interpolate(double start, double end, double t) {
        return start + t * (end - start);
    }

    private double distance(Point3D first, Point3D second) {
        double dx = second.getX() - first.getX();
        double dy = second.getY() - first.getY();
        double dz = second.getZ() - first.getZ();

        return Math.hypot(Math.hypot(dx, dy), dz);
    }
}