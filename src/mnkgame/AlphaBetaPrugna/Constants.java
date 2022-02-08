package mnkgame.AlphaBetaPrugna;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Constants {
  private Constants() {}

  public static final Point UP_LEFT = new Point(-1, -1);
  public static final Point UP = new Point(-1, 0);
  public static final Point UP_RIGHT = new Point(-1, 1);
  public static final Point RIGHT = new Point(0, 1);
  public static final Point DOWN_RIGHT = new Point(1, 1);
  public static final Point DOWN = new Point(1, 0);
  public static final Point DOWN_LEFT = new Point(1, -1);
  public static final Point LEFT = new Point(0, -1);

  public static final Map<String, Point> allDirections = new LinkedHashMap<>()
  {{
    put("up-left", UP_LEFT);
    put("up", UP);
    put("up-right", UP_RIGHT);
    put("right", RIGHT);
    put("down-right", DOWN_RIGHT);
    put("down", DOWN);
    put("down-left", DOWN_LEFT);
    put("left", LEFT);
  }};
}
