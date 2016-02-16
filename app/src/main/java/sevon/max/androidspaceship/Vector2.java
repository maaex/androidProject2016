package sevon.max.androidspaceship;

/**
 * Created by Max on 2016-01-16.
 * A 2-dimensional vector with some simple operations.
 */
public class Vector2 {

    private float x;
    private float y;

    /**
     * Creates a Vector.
     * @param x The vector's x-coordinate.
     * @param y The vector's y-coordinate.
     */
    public Vector2 (float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns a vector representing v1 added to v2.
     * @param v1 The first vector.
     * @param v2 The second vector.
     */
    public static Vector2 add(Vector2 v1, Vector2 v2) {
        return new Vector2(v1.x + v2.x, v1.y + v2.y);
    }

    /**
     * Returns a vector representing v multiplied with a scalar.
     * @param v The vector to multiply.
     * @param scalar The scalar to multiply with.
     */
    public static Vector2 multiply(Vector2 v, float scalar) {
        return new Vector2(v.x * scalar, v.y * scalar);
    }

    /**
     * Returns the dot product of the two vectors.
     * @param v1 The first vector.
     * @param v2 The second vector.
     */
    public static float dot(Vector2 v1, Vector2 v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    /**
     * Uses pythagoras theorem to calculate and return the length of the vector.
     * @return The length of the vector.
     */
    public float length() {
        return (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    /**
     * Returns a vector representing v, normalized.
     * @param v The vector to normalize.
     */
    public static Vector2 normalize(Vector2 v) {
        float length = v.length();
        float x = v.x;
        float y = v.y;

        if(v.x != 0)
            x = v.x / length;
        if(v.y != 0)
            y = v.y / length;

        return new Vector2(x,y);
    }

    public float getX() { return x; }
    public float getY() { return y; }

    /**
     * Returns the angle between v1 and v2.
     * @param v1 Vector 1.
     * @param v2 Vector 2.
     * @return The angle between v1 and v2.
     */
    public static float getAngle(Vector2 v1, Vector2 v2) {
        Vector2 v1n = normalize(v1);
        Vector2 v2n = normalize(v2);
        return (float) Math.acos(dot(v1n, v2n));
    }
}
