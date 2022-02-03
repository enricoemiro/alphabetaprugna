package mnkgame.AlphaBetaPrugna;

import java.lang.Integer;

public class Point {
  public Integer x;
  public Integer y;

  public Point(Integer x, Integer y) {
    this.x = x;
    this.y = y;
  }

  public Point(Point point) {
    this.x = point.x;
    this.y = point.y;
  }

  public String toString() {
    return String.format("(%d, %d)", x, y);
  }
}
