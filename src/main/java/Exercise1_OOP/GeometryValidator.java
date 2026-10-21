package Exercise1_OOP;

public class GeometryValidator {

    private static final double ABS_EPS = 1e-8;
    private static final double REL_EPS = 1e-8;
    private static final double ANGLE_EPS = 1e-8;

    public void validate(SliceRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Missing CSV row.");
        }

        Point3D[] points = row.getPoints();

        if (points == null
                || (points.length != 3 && points.length != 4)) {
            throw new IllegalArgumentException(
                    "Expected 3 or 4 points.");
        }

        validatePoints(points);

        if (points.length == 3) {
            validateTriangle(points);
        } else {
            validateTrapezoid(points);
        }
    }

    private void validatePoints(Point3D[] points) {
        for (Point3D point : points) {
            if (point == null
                    || !Double.isFinite(point.getX())
                    || !Double.isFinite(point.getY())
                    || !Double.isFinite(point.getZ())) {

                throw new IllegalArgumentException(
                        "All points must contain finite coordinates.");
            }
        }


        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double distance = length(
                        vector(points[i], points[j]));

                if (!Double.isFinite(distance)) {
                    throw new IllegalArgumentException(
                            "Coordinates are too large for calculation.");
                }

                if (distance <= ABS_EPS) {
                    throw new IllegalArgumentException(
                            "Points " + (i + 1) + " and " + (j + 1)
                                    + " coincide or are too close.");
                }
            }
        }
    }

    private void validateTriangle(Point3D[] points) {
        Point3D a = points[0];
        Point3D b = points[1];
        Point3D c = points[2];

        double[] ab = vector(a, b);
        double[] ac = vector(a, c);

        double abLength = length(ab);
        double acLength = length(ac);

        double[] abDirection = unit(ab);
        double[] acDirection = unit(ac);

        if (length(cross(abDirection, acDirection)) <= ANGLE_EPS) {
            throw new IllegalArgumentException(
                    "Triangle points are collinear or nearly collinear.");
        }

        if (!nearlyEqual(abLength, acLength)) {
            throw new IllegalArgumentException(
                    "Expected equal legs: distance(a,b) = distance(a,c).");
        }
    }

    private void validateTrapezoid(Point3D[] points) {
        Point3D a1 = points[0];
        Point3D a2 = points[1];
        Point3D b = points[2];
        Point3D c = points[3];

        double[] topBase = vector(a1, a2);
        double[] bottomBase = vector(b, c);
        double[] leftLeg = vector(a1, b);
        double[] rightLeg = vector(a2, c);

        double[] topDirection = unit(topBase);
        double[] bottomDirection = unit(bottomBase);
        double[] leftDirection = unit(leftLeg);


        double[] normal = cross(topDirection, leftDirection);

        if (length(normal) <= ANGLE_EPS) {
            throw new IllegalArgumentException(
                    "Trapezoid has zero or nearly zero area.");
        }

        normal = unit(normal);


        double[] a1ToC = vector(a1, c);
        double distanceFromPlane = Math.abs(dot(a1ToC, normal));

        double scale = Math.max(
                Math.max(length(topBase), length(bottomBase)),
                Math.max(
                        Math.max(length(leftLeg), length(rightLeg)),
                        length(a1ToC)));

        if (distanceFromPlane > tolerance(scale)) {
            throw new IllegalArgumentException(
                    "Trapezoid points are not coplanar.");
        }

        if (length(cross(topDirection, bottomDirection)) > ANGLE_EPS) {
            throw new IllegalArgumentException(
                    "Trapezoid bases are not parallel.");
        }


        if (dot(topDirection, bottomDirection) <= 0) {
            throw new IllegalArgumentException(
                    "Invalid point order: both bases must have "
                            + "the same direction.");
        }

        if (!nearlyEqual(length(leftLeg), length(rightLeg))) {
            throw new IllegalArgumentException(
                    "Trapezoid legs must have equal lengths.");
        }


        double centerShift =
                0.5 * dot(leftLeg, topDirection)
                        + 0.5 * dot(rightLeg, topDirection);

        if (Math.abs(centerShift) > tolerance(scale)) {
            throw new IllegalArgumentException(
                    "Trapezoid bases are not symmetrically aligned.");
        }
    }

    private boolean nearlyEqual(double first, double second) {
        double scale = Math.max(Math.abs(first), Math.abs(second));
        return Math.abs(first - second) <= tolerance(scale);
    }

    private double tolerance(double scale) {
        return ABS_EPS + REL_EPS * scale;
    }

    private double[] vector(Point3D from, Point3D to) {
        return new double[]{
                to.getX() - from.getX(),
                to.getY() - from.getY(),
                to.getZ() - from.getZ()
        };
    }

    private double length(double[] vector) {
        return Math.hypot(
                Math.hypot(vector[0], vector[1]), vector[2]);
    }

    private double[] unit(double[] vector) {
        double length = length(vector);

        return new double[]{
                vector[0] / length,
                vector[1] / length,
                vector[2] / length
        };
    }

    private double dot(double[] first, double[] second) {
        return first[0] * second[0]
                + first[1] * second[1]
                + first[2] * second[2];
    }

    private double[] cross(double[] first, double[] second) {
        return new double[]{
                first[1] * second[2] - first[2] * second[1],
                first[2] * second[0] - first[0] * second[2],
                first[0] * second[1] - first[1] * second[0]
        };
    }
}