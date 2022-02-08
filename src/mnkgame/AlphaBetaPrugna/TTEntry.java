package mnkgame.AlphaBetaPrugna;

public class TTEntry {
  public static enum Flag { EXACT, ALPHA, BETA }

  public int depth;
  public int score;

  /**
   * Node Type:
   * - Exact: means this is an exact score for the board.
   * - Alpha: means the value of the node was at most equal to score.
   * - Beta: means the value is at least equal to score.
   */
  public Flag flag;

  public TTEntry() {
    this.depth = -1;
    this.score = -1;
    this.flag = null;
  }

  public String toString() {
    return String.format("depth: %d, score: %d, flag: %s",
                         depth, score, flag.toString());
  }
}
