package mnkgame.AlphaBetaPrugna;

public class Point {
  /** Abscissa */
  public int x;

  /** Ordinate */
  public int y;

  /** Point constructor. */
  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  /** Point copy constructor. */
  public Point(Point point) {
    this.x = point.x;
    this.y = point.y;
  }

  /**
   * Multiply each coordinate by a scalar.
   *
   * @param scalar scalar value to multiply by
   * @return current instance with updated coordinates
   */
  public Point multiply(int scalar) {
    this.x *= scalar;
    this.y *= scalar;
    return this;
  }

  /** {@inheritDoc} */
  public String toString() { return String.format("(%d, %d)", x, y); }
}
