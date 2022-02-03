package mnkgame.AlphaBetaPrugna;

import java.util.LinkedHashMap;

public class Direction {
  public final Point point;

  public Direction(Point point) {
    this.point = point;
  }

  /**
   * ---------------------------------------
   * |     up-left |   up   |     up-right |
   * ---------------------------------------
   * |        left |        |        right |
   * ---------------------------------------
   * | bottom-left | bottom | bottom-right |
   * ---------------------------------------
   */
  public static LinkedHashMap<String, Direction> getDirections() {
    return new LinkedHashMap<String, Direction>() {
      {
        put("up-left", new Direction(new Point(-1, -1)));
        put("up", new Direction(new Point(0, -1)));
        put("up-right", new Direction(new Point(1, -1)));

        put("left", new Direction(new Point(-1, 0)));
        put("right", new Direction(new Point(1, 0)));

        put("bottom-left", new Direction(new Point(-1, 1)));
        put("bottom", new Direction(new Point(0, 1)));
        put("bottom-right", new Direction(new Point(1, 1)));
      }
    };
  }
}
