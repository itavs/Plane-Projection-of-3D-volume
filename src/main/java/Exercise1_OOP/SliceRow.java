package Exercise1_OOP;

public class SliceRow {

    private final int _lineNumber;
    private final Point3D[] _points;

    public SliceRow(int lineNumber, Point3D[] points) {
        this._lineNumber = lineNumber;
        this._points = points.clone();
    }

    public int getLineNumber() {
        return this._lineNumber;
    }

    public Point3D[] getPoints() {
        return this._points.clone();
    }
}