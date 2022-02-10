package mnkgame.AlphaBetaPrugna;

import static mnkgame.AlphaBetaPrugna.Constants.*;

import mnkgame.MNKCell;
import mnkgame.MNKCellState;

public class Eval {
  /** Local board */
  private Board board;
  /** Cell to be evaluated */
  private MNKCell startCell;
  /** Score of the cell to evaluate */
  private int startCellScore;
  /** State of the cell to evaluate */
  private MNKCellState startCellState;
  /** Opposite state of the cell to evaluate */
  private MNKCellState opponentCellState;
  /** K - 1 series score */
  private static int KMINUSONE = 100;
  /** K - 2 series score */
  private static int KMINUSTWO = 30;
  /** K - 3 series score */
  private static int KMINUSTHIRD = 15;

  public Eval(Board board, MNKCell startCell) {
    this.board = board;
    this.startCell = startCell;
    this.startCellScore = this.board.getCellScore(this.startCell);
    this.startCellState = this.startCell.state;
    if (this.startCellState == MNKCellState.P1)
      this.opponentCellState = MNKCellState.P2;
    else
      this.opponentCellState = MNKCellState.P1;
  }

  /**
   * Evaluate all directions from the start cel
   *
   * @return the score associated to startCell
   */
  public int eval() {
    return evalRow() + evalColumn() + evalDiagonal() + evalAntidiagonal()
            + startCellScore;
  }

  /**
   * Calls evalSideDirection on the two sides in input.
   *
   * @param firstSide first side of a direction
   * @param secondSide second side of a direction
   * @param stateToMatch state of the cells for which to evaluate the series
   */
  private int baseEval(
          Point firstSide, Point secondSide, MNKCellState stateToMatch) {
    SideDirection firstDirection = new SideDirection();
    SideDirection secondDirection = new SideDirection();

    firstDirection.evalSideDirection(firstSide, stateToMatch);
    secondDirection.evalSideDirection(secondSide, stateToMatch);

    return firstDirection.evalUnion(secondDirection);
  }

  /**
   * Calls baseEval on the two direction of the row.
   */
  private int evalRow() {
    Point left = new Point(LEFT);
    Point right = new Point(RIGHT);

    return baseEval(left, right, startCellState);
  }

  /**
   * Calls baseEval on the two direction of the column.
   */
  private int evalColumn() {
    Point up = new Point(UP);
    Point down = new Point(DOWN);

    return baseEval(up, down, startCellState);
  }

  /**
   * Calls baseEval on the two direction of the diagonal.
   */
  private int evalDiagonal() {
    Point upLeft = new Point(UP_LEFT);
    Point downRight = new Point(DOWN_RIGHT);

    return baseEval(upLeft, downRight, startCellState);
  }

  /**
   * Calls baseEval on the two direction of the antidiagonal.
   */
  private int evalAntidiagonal() {
    Point upRight = new Point(UP_RIGHT);
    Point downLeft = new Point(DOWN_LEFT);

    return baseEval(upRight, downLeft, startCellState);
  }

  /** Class that implements metods of one side evaluation */
  final private class SideDirection {
    /** Cell that close the one side series */
    public MNKCell sideCellEnd;
    /** Number of consecutive alignments */
    public int consecutiveAlignments;
    /** True if there is a cell that close the series */
    private boolean hasTouchedOtherState;

    public SideDirection() {
      this.sideCellEnd = null;
      this.consecutiveAlignments = 0;
      this.hasTouchedOtherState = false;
    }

    /**
     * Evaluate a single side direction.
     *
     * @param side Side to move on
     * @param stateToMatch State of the cell to be matched
     */
    public void evalSideDirection(Point side, MNKCellState stateToMatch) {
      int distance = 1;

      while (!hasTouchedOtherState) {
        MNKCell nextCell =
                board.cellNullOrExists(startCell, side.multiply(distance));
        if (nextCell == null) break;

        // If the state of the nextCell is different from ours, it means that we
        // have touched either a free cell or the opposite cell. So we need to
        // exit the loop and save the nextCell.
        if (nextCell.state != stateToMatch) {
          hasTouchedOtherState = true;
          sideCellEnd = nextCell;

          // Otherwise it means that the state of the nextCell is equal to
          // stateToMatch, so we increase the number of consecutive cells
        } else {
          consecutiveAlignments++;
        }

        distance++;
      }
    }

    /**
     * Evaluate the number of consecutive cells there are by joining the two
     * sides of a direction.
     *
     * @param other
     * @param stateToMatch
     * @return
     */
    public int evalUnion(SideDirection other) {
      int totalConsecutiveAlignments =
              1 + this.consecutiveAlignments + other.consecutiveAlignments;

      return evalIncreaser(other)
              + evalConsecutiveAlignments(totalConsecutiveAlignments);
    }

    /**
     * Example: 5x5
     *
     * BLOCK ONE: |null|x|x|x|null| or
     *            |OPPONENT|x|x|x|OPPONENT|
     *
     * @param other the other side of current direction
     * @return true if we are in BlockOne configurations, false otherwise
     */
    private boolean increaserBlockOne(SideDirection other) {
      boolean areSideNull =
              this.sideCellEnd == null & other.sideCellEnd == null;
      if (areSideNull) return true;

      if (this.sideCellEnd != null && other.sideCellEnd != null)
        return this.sideCellEnd.state == opponentCellState
                && other.sideCellEnd.state == opponentCellState;

      return false;
    }

    /**
     * Example: 5x5
     *
     * BLOCK TWO: |null|x|x|x|FREE| or
     *            |FREE|x|x|x|null|
     *
     * @param other the other side of current direction
     * @return true if we are in BlockTwo configurations, false otherwise
     */
    private boolean increaserBlockTwo(SideDirection other) {
      boolean isFirstSideNull = this.sideCellEnd == null;
      boolean isSecondSideNull = other.sideCellEnd == null;

      if (isFirstSideNull && !isSecondSideNull)
        return other.sideCellEnd.state == MNKCellState.FREE;

      else if (!isFirstSideNull && isSecondSideNull)
        return this.sideCellEnd.state == MNKCellState.FREE;

      return false;
    }

    /**
     * Example: 5x5
     *
     * BLOCK THIRD: |null|x|x|x|OPPONENT| or
     *              |OPPONENT|x|x|x|null|
     *
     * @param other the other side of current direction
     * @return true if we are in BlockThird configurations, false otherwise
     */
    private boolean increaserBlockThird(SideDirection other) {
      boolean isFirstSideNull = this.sideCellEnd == null;
      boolean isSecondSideNull = other.sideCellEnd == null;

      if (isFirstSideNull && !isSecondSideNull)
        return other.sideCellEnd.state == opponentCellState;

      else if (!isFirstSideNull && isSecondSideNull)
        return this.sideCellEnd.state == opponentCellState;

      return false;
    }

    /**
     * Example: 5x5
     *
     * BLOCK FOURTH: |OPPONENT|x|x|x|FREE| or
     *               |FREE|x|x|x|OPPONENT|
     *
     * @param other the other side of current direction
     * @return true if we are in BlockFourth configurations, false otherwise
     */
    private boolean increaserBlockFourth(SideDirection other) {
      boolean areSideNotNull =
              this.sideCellEnd != null & other.sideCellEnd != null;

      if (areSideNotNull) {
        boolean isFirstSideFree = this.sideCellEnd.state == MNKCellState.FREE;
        boolean isSecondStateFree =
                other.sideCellEnd.state == MNKCellState.FREE;

        return (!isFirstSideFree && isSecondStateFree)
                || (isFirstSideFree && !isSecondStateFree);
      }

      return false;
    }

    /**
     * Example: 5x5
     *
     * BLOCK FOURTH: |FREE|x|x|x|FREE|
     *
     * @param other the other side of current direction
     * @return true if we are in BlockFifth configurations, false otherwise
     */
    private boolean increaserBlockFifth(SideDirection other) {
      boolean areSideNotNull =
              this.sideCellEnd != null & other.sideCellEnd != null;

      if (areSideNotNull) {
        boolean isFirstSideFree = this.sideCellEnd.state == MNKCellState.FREE;
        boolean isSecondStateFree =
                other.sideCellEnd.state == MNKCellState.FREE;

        return isFirstSideFree && isSecondStateFree;
      }

      return false;
    }

    /**
     * Calculate the increaser of the configuration.
     *
     * @param other The other side of current direction
     * @return the value to add to the score
     */
    private int evalIncreaser(SideDirection other) {
      int K = board.K;

      if (K > 3) {
        if (increaserBlockOne(other) || increaserBlockThird(other))
          return 0;

        else if (increaserBlockTwo(other) || increaserBlockFourth(other))
          return 1;

        else if (increaserBlockFifth(other))
          return 2;
      }

      return 0;
    }

    /**
     * Assign a high score based on the number of alignments.
     * More specifically we consider only the series:
     * - k-1
     * - k-2
     * - k-3
     *
     * @param consecutiveAlignments number of consecutive alignments
     * @return score given to the series
     */
    private int evalConsecutiveAlignments(int consecutiveAlignments) {
      int K = board.K;

      if (K > 2 && consecutiveAlignments == K - 1)
        return KMINUSONE;
      else if (K > 3 && consecutiveAlignments == K - 2)
        return KMINUSTWO;
      else if (K > 4 && consecutiveAlignments == K - 3)
        return KMINUSTHIRD;

      return 0;
    }
  }
}
