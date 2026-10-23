package Exercise1_OOP;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryValidatorTest {

    private final GeometryValidator validator = new GeometryValidator();

    @Test
    void acceptsValidIsoscelesTriangle() {
        // Both legs have length 5; the base has length 6.
        assertAccepted(
                p(0, 4, 0),
                p(-3, 0, 0),
                p(3, 0, 0)
        );
    }

    @Test
    void acceptsTriangleWithSwappedBaseEndpoints() {
        assertAccepted(
                p(0, 4, 0),
                p(3, 0, 0),
                p(-3, 0, 0)
        );
    }

    @Test
    void rejectsTriangleWithUnequalLegs() {
        assertRejected(
                p(0, 4, 0),
                p(-3, 0, 0),
                p(4, 0, 0)
        );
    }

    @Test
    void rejectsTriangleWhenFirstPointIsNotTheApex() {
        // The shape is isosceles, but the input order is incorrect:
        // distances from the first point are 5 and 6.
        assertRejected(
                p(-3, 0, 0),
                p(0, 4, 0),
                p(3, 0, 0)
        );
    }

    @Test
    void rejectsCollinearAndNearlyCollinearTriangles() {
        assertRejected(
                p(0, 0, 0),
                p(1, 0, 0),
                p(-1, 0, 0)
        );

        // Distinct points and approximately equal legs,
        // but the angle is too close to a straight line.
        assertRejected(
                p(0, 0, 0),
                p(1, 0, 0),
                p(-1, 1e-10, 0)
        );
    }

    @Test
    void acceptsWideningAndNarrowingTrapezoids() {
        // Order: a1, a2, b, c.
        assertAccepted(
                p(-1, 0, 0),
                p(1, 0, 0),
                p(-3, 4, 0),
                p(3, 4, 0)
        );

        assertAccepted(
                p(-3, 0, 0),
                p(3, 0, 0),
                p(-1, 4, 0),
                p(1, 4, 0)
        );
    }

    @Test
    void acceptsRectangleAsEqualBaseSpecialCase() {
        assertAccepted(
                p(-2, 0, 0),
                p(2, 0, 0),
                p(-2, 3, 0),
                p(2, 3, 0)
        );
    }

    @Test
    void rejectsTrapezoidWithUnequalLegs() {
        // Bases are parallel, but legs have lengths sqrt(20)
        // and sqrt(25).
        assertRejected(
                p(-1, 0, 0),
                p(1, 0, 0),
                p(-3, 4, 0),
                p(4, 4, 0)
        );
    }

    @Test
    void rejectsNonparallelBasesEvenWhenLegsAreEqual() {
        // Left leg: (-3, 4), length 5.
        // Right leg: (0, 5), length 5.
        // The bases are not parallel.
        assertRejected(
                p(0, 0, 0),
                p(4, 0, 0),
                p(-3, 4, 0),
                p(4, 5, 0)
        );
    }

    @Test
    void rejectsNonCoplanarTrapezoidPoints() {
        // Equal leg lengths do not guarantee coplanarity.
        assertRejected(
                p(-1, 0, 0),
                p(1, 0, 0),
                p(-3, 4, 1),
                p(3, 4, -1)
        );
    }

    @Test
    void rejectsCrossedLegsFromReversedEndingBase() {
        // Swapping b and c reverses only the ending base.
        assertRejected(
                p(-1, 0, 0),
                p(1, 0, 0),
                p(3, 4, 0),
                p(-3, 4, 0)
        );
    }

    @Test
    void rejectsZeroAreaTrapezoid() {
        assertRejected(
                p(-1, 0, 0),
                p(1, 0, 0),
                p(-3, 0, 0),
                p(3, 0, 0)
        );
    }

    @Test
    void rejectsObliqueParallelogramWithEqualLegs() {
        // Parallel equal bases and equal legs, but no
        // symmetric alignment. This is not a rectangle.
        assertRejected(
                p(0, 0, 0),
                p(4, 0, 0),
                p(1, 3, 0),
                p(5, 3, 0)
        );
    }

    @Test
    void acceptsTranslatedAndTiltedShapes() {
        // Rotate the original XY shapes around the X axis,
        // then translate them. Distances must be preserved.
        assertAccepted(
                transform(0, 4),
                transform(-3, 0),
                transform(3, 0)
        );

        assertAccepted(
                transform(-1, 0),
                transform(1, 0),
                transform(-3, 4),
                transform(3, 4)
        );
    }

    @Test
    void rejectsRepeatedAndTooClosePoints() {
        assertRejected(
                p(0, 4, 0),
                p(-3, 0, 0),
                p(-3, 0, 0)
        );

        assertRejected(
                p(-1, 0, 0),
                p(1, 0, 0),
                p(-3, 4, 0),
                p(-3, 4, 0)
        );

        assertRejected(
                p(0, 0, 0),
                p(1e-10, 0, 0),
                p(0, 1, 0)
        );
    }

    @Test
    void rejectsMissingRowsAndIncorrectPointCounts() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(null)
        );

        assertHasMessage(error);

        assertRejected();
        assertRejected(p(0, 0, 0));
        assertRejected(p(0, 0, 0), p(1, 0, 0));

        assertRejected(
                p(0, 0, 0),
                p(1, 0, 0),
                p(0, 1, 0),
                p(1, 1, 0),
                p(2, 2, 0)
        );
    }

    @Test
    void rejectsNullPointsAndNonFiniteCoordinates() {
        assertRejected(
                p(0, 4, 0),
                null,
                p(3, 0, 0)
        );

        double[] invalidValues = {
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double invalid : invalidValues) {
            assertRejected(
                    p(invalid, 4, 0),
                    p(-3, 0, 0),
                    p(3, 0, 0)
            );

            assertRejected(
                    p(0, invalid, 0),
                    p(-3, 0, 0),
                    p(3, 0, 0)
            );

            assertRejected(
                    p(0, 4, invalid),
                    p(-3, 0, 0),
                    p(3, 0, 0)
            );
        }
    }

    @Test
    void appliesAbsoluteAndRelativeLegLengthTolerance() {
        // Around length 1, the tolerance is approximately 2e-8.
        assertAccepted(
                p(0, 0, 0),
                p(1, 0, 0),
                p(0, 1 + 1e-10, 0)
        );

        assertRejected(
                p(0, 0, 0),
                p(1, 0, 0),
                p(0, 1 + 1e-5, 0)
        );

        // Around length 1000, the tolerance is approximately 1e-5.
        assertAccepted(
                p(0, 0, 0),
                p(1000, 0, 0),
                p(0, 1000 + 1e-6, 0)
        );

        assertRejected(
                p(0, 0, 0),
                p(1000, 0, 0),
                p(0, 1000 + 1e-3, 0)
        );
    }

    // Helpers

    private Point3D p(double x, double y, double z) {
        return new Point3D(x, y, z);
    }

    private Point3D transform(double x, double y) {
        // cos = 0.6 and sin = 0.8, so cos² + sin² = 1.
        return p(
                x - 10,
                0.6 * y + 7,
                0.8 * y - 20
        );
    }

    private void assertAccepted(Point3D... points) {
        SliceRow row = new SliceRow(1, points);

        assertDoesNotThrow(() -> validator.validate(row));
    }

    private void assertRejected(Point3D... points) {
        SliceRow row = new SliceRow(1, points);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(row)
        );

        assertHasMessage(error);
    }

    private void assertHasMessage(IllegalArgumentException error) {
        assertNotNull(error.getMessage());
        assertFalse(error.getMessage().isBlank());
    }
}