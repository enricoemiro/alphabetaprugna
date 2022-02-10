package mnkgame.AlphaBetaPrugna;

public class TTEntry {
  public static enum Flag {
    /** means the value of the board was an EXACT score. */
    EXACT,
    /** means the value of the board was AT MOST equal to score. */
    ALPHA,
    /** means the value of the board was AT LEAST equal to score. */
    BETA
  }

  /** Depth of the board configuration. */
  public int depth;

  /** Score of the board configuration. */
  public int score;

  /** Flag that describe the type of the score. */
  public Flag flag;

  /** TTEntry default constructor. */
  public TTEntry() {
    this.depth = -1;
    this.score = -1;
    this.flag = null;
  }

  /** {@inheritDoc} */
  public String toString() {
    return String.format(
            "depth: %d, score: %d, flag: %s", depth, score, flag.toString());
  }
}
