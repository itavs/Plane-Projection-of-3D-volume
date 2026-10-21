package Exercise1_OOP;

public class Point3D {

    private final double _x, _y, _z;

    public Point3D(double x, double y, double z) {
        this._x = x;
        this._y = y;
        this._z = z;
    }

    public double getX() {
        return this._x;
    }

    public double getY() {
        return this._y;
    }

    public double getZ() {
        return this._z;
    }
}
