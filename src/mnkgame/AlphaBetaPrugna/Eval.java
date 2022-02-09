package mnkgame.AlphaBetaPrugna;

import static mnkgame.AlphaBetaPrugna.Constants.*;

import mnkgame.MNKCell;
import mnkgame.MNKCellState;

public class Eval {
  private Board board;
  private MNKCell startCell;
  private int startCellScore;
  private MNKCellState startCellState;
  private MNKCellState opponentCellState;

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

  public int eval() {
    return evalRow() +
           evalColumn() +
           evalDiagonal() +
           evalAntidiagonal() +
           startCellScore;
  }

  private int baseEval(Point firstSide, Point secondSide, MNKCellState stateToMatch) {
    SideDirection firstDirection = new SideDirection();
    SideDirection secondDirection = new SideDirection();

    firstDirection.evalSideDirection(firstSide, stateToMatch);
    secondDirection.evalSideDirection(secondSide, stateToMatch);

    return firstDirection.evalUnion(secondDirection);
  }

  private int evalRow() {
    Point left = new Point(LEFT);
    Point right = new Point(RIGHT);

    return baseEval(left, right, startCellState);
  }

  private int evalColumn() {
    Point up = new Point(UP);
    Point down = new Point(DOWN);

    return baseEval(up, down, startCellState);
  }

  private int evalDiagonal() {
    Point upLeft = new Point(UP_LEFT);
    Point downRight = new Point(DOWN_RIGHT);

    return baseEval(upLeft, downRight, startCellState);
  }

  private int evalAntidiagonal() {
    Point upRight = new Point(UP_RIGHT);
    Point downLeft = new Point(DOWN_LEFT);

    return baseEval(upRight, downLeft, startCellState);
  }

  final private class SideDirection {
    public MNKCell sideCellEnd;
    public int consecutiveAlignments;
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
        MNKCell nextCell = board.cellNullOrExists(startCell, side.multiply(distance));
        if (nextCell == null) break;

        // Se lo stato della nextCell è diverso dal nostro, significa
        // che abbiamo toccato o una cella libera o la cella opposta.
        // Quindi dobbiamo uscire dal ciclo e salvare la nextCell.
        if (nextCell.state != stateToMatch) {
          hasTouchedOtherState = true;
          sideCellEnd = nextCell;

        // Altrimenti significa che lo stato della nextCell è uguale
        // a stateToMatch, dunque incrementiamo il numero di
        // celle consecutive
        } else {
          consecutiveAlignments++;
        }

        distance++;
      }
    }

    /**
     * Dobbiamo valutare il numero di allineamenti consecutivi
     * che vi sono unendo le direzioni.
     *
     * @param other
     * @param stateToMatch
     * @return
     */
    public int evalUnion(SideDirection other) {
      int totalConsecutiveAlignments = 1 + this.consecutiveAlignments +
                                       other.consecutiveAlignments;

      int increaser = evalIncreaser(other);
      // if (stateToMatch == startCellState) totalConsecutiveAlignments += 1;

      return increaser + evalConsecutiveAlignments(totalConsecutiveAlignments);
    }

    /**
     * Example: 5x5
     *
     * BLOCK ONE: |null|x|x|x|null| or
     *            |OPPONENT|x|x|x|OPPONENT|
     *
     * @param other
     * @return
     */
    private boolean increaserBlockOne(SideDirection other) {
      boolean areSideNull = this.sideCellEnd == null & other.sideCellEnd == null;
      if (areSideNull) return true;

      if (this.sideCellEnd != null && other.sideCellEnd != null)
        return this.sideCellEnd.state == opponentCellState &&
               other.sideCellEnd.state == opponentCellState;

      return false;
    }

    /**
     * Example: 5x5
     *
     * BLOCK TWO: |null|x|x|x|FREE| or
     *            |FREE|x|x|x|null|
     *
     * @param other
     * @return
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
     * @param other
     * @return
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
     * @param other
     * @return
     */
    private boolean increaserBlockFourth(SideDirection other) {
      boolean areSideNotNull = this.sideCellEnd != null & other.sideCellEnd != null;

      if (areSideNotNull) {
        boolean isFirstSideFree = this.sideCellEnd.state == MNKCellState.FREE;
        boolean isSecondStateFree = this.sideCellEnd.state == MNKCellState.FREE;

        return (!isFirstSideFree && isSecondStateFree) ||
               (isFirstSideFree && !isSecondStateFree);
      }

      return false;
    }

    /**
     * Example: 5x5
     *
     * BLOCK FOURTH: |FREE|x|x|x|FREE|
     *
     * @param other
     * @return
     */
    private boolean increaserBlockFifth(SideDirection other) {
      boolean areSideNotNull = this.sideCellEnd != null & other.sideCellEnd != null;

      if (areSideNotNull) {
        boolean isFirstSideFree = this.sideCellEnd.state == MNKCellState.FREE;
        boolean isSecondStateFree = this.sideCellEnd.state == MNKCellState.FREE;

        return isFirstSideFree && isSecondStateFree;
      }

      return false;
    }

    /**
     *
     *
     * @param other
     * @return
     */
    private int evalIncreaser(SideDirection other) {
      int K = board.K;

      if (K > 3) {
        if (increaserBlockOne(other) ||
            increaserBlockThird(other)) return 0;

        else if (increaserBlockTwo(other) ||
                 increaserBlockFourth(other)) return 1;

        else if (increaserBlockFifth(other)) return 2;
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
     * @param consecutiveAlignments Number of cells aligned
     * @return score given to the series
     */
    private int evalConsecutiveAlignments(int consecutiveAlignments) {
      int K = board.K;

      if (K > 2 && consecutiveAlignments == K - 1) return 1000;
      else if (K > 3 && consecutiveAlignments == K - 2) return 100;
      else if (K > 4 && consecutiveAlignments == K - 3) return 10;

      return 0;
    }
  }
}
