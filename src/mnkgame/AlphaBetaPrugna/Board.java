package mnkgame.AlphaBetaPrugna;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import mnkgame.MNKBoard;
import mnkgame.MNKCell;
import mnkgame.MNKCellState;
import mnkgame.MNKGameState;

final public class Board extends MNKBoard {
  private Random random;
  private ZobristHash zobrist;

  public long hash;
  public Map<MNKCell, Integer> scores = new LinkedHashMap<>();

  /**
   * {@inheritDoc}
   */
  public Board(int M, int N, int K) {
    super(M, N, K);

    this.setBoardScores();
    this.zobrist = new ZobristHash(M, N);
    this.random = new Random(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   */
  public MNKGameState markCell(MNKCell cell) throws IndexOutOfBoundsException {
    MNKGameState gameState = super.markCell(cell.i, cell.j);
    MNKCellState markedCellState = B[cell.i][cell.j];
    // System.out.println(new MNKCell(cell.i, cell.j, markedCellState));
    // ZOBRIST
    this.hash = this.zobrist.updateZobrist(this.hash,
                                           new MNKCell(cell.i, cell.j, markedCellState));

    return gameState;
  }

  public void unmarkCell() {
    MNKCell lastMarkedCell = MC.getLast();
    super.unmarkCell();
    // ZOBRIST
    this.hash = this.zobrist.updateZobrist(this.hash, lastMarkedCell);
  }

  /**
   * {@return random cell from the free cells list}
   */
  public MNKCell pickRandomCell() {
    MNKCell[] FC = this.getFreeCells();
    return FC[this.random.nextInt(0, FC.length)];
  }

  /**
   * Check if the cell in the input direction exists or
   * is out of bounds.
   *
   * @param cell
   * @param point
   * @return
   */
  public MNKCell cellNullOrExists(MNKCell cell, Point point) {
    int i = cell.i + point.x;
    int j = cell.j + point.y;

    if (!isCellInBounds(i, j)) return null;
    return new MNKCell(i, j, cellState(i, j));
  }

  /**
   * @return last marked cell from the marked cells list
   */
  public MNKCell getLastMarkedCell() {
    return MC.getLast();
  }

  /**
   *
   * @param cellState
   * @return
   */
  public MNKCell getLastMarkedCell(MNKCellState cellState) {
    if (MC.size() == 0)
      return null;

    for (int i = MC.size() - 1; i >= 0; i--) {
      MNKCell current = MC.get(i);
      if (current.state == cellState)
        return current;
    }

    return null;
  }

  /**
   * {@return true if the game state is open, false otherwise}
   */
  public boolean isGameOpen() {
    return this.gameState().equals(MNKGameState.OPEN);
  }

  /**
   * {@return true if the cell is inside board bounds, false otherwise}
   */
  public boolean isCellInBounds(MNKCell cell) {
    boolean isValidRow = cell.i >= 0 && cell.i < M;
    boolean isValidColumn = cell.j >= 0 && cell.j < N;
    return isValidRow && isValidColumn;
  }

  /**
   * @see Board#isCellInBounds(MNKCell)
   */
  public boolean isCellInBounds(int i, int j) {
    return isCellInBounds(new MNKCell(i, j));
  }

  /**
   * {@return the free cells HashSet}
   */
  public HashSet<MNKCell> getFCSet() {
    return this.FC;
  }

  /**
   *
   */
  public int getCellScore(MNKCell cell) {
    return scores.get(new MNKCell(cell.i, cell.j));
  }

  /** Associate a score to each cell on the board */
  private void setBoardScores() {
    /**
     * Example: 4x4
     *
     * up ->    |  1 |  2 |  3 |  4 |
     *          |  5 |  6 |  7 |  8 |
     *          |  9 | 10 | 11 | 12 |
     * down ->  | 13 | 14 | 15 | 16 |
     *            ^              ^
     *            |              |
     *          left           right
     *
     * Direction:
     *  - 0 : LEFT  -> RIGHT
     *  - 1 : UP    -> DOWN
     *  - 2 : RIGHT -> LEFT
     *  - 3 : DOWN  -> UP
     */
    int up = 0;
    int down = M - 1;
    int left = 0;
    int right = N - 1;
    int direction = 0;

    int score = 1;
    while (up <= down && left <= right) {
      // We move from left to right
      if (direction == 0) {
        for (int i = left; i <= right; i++) {
          MNKCell cell = new MNKCell(up, i);
          scores.put(cell, score);
        }
        up++;
      }

      // We move from up to down
      if (direction == 1) {
        for (int i = up; i <= down; i++) {
          MNKCell cell = new MNKCell(i, right);
          scores.put(cell, score);
        }
        right--;
      }

      // We move from right to left
      if (direction == 2) {
        for (int i = right; i >= left; i--) {
          MNKCell cell = new MNKCell(down, i);
          scores.put(cell, score);
        }
        down--;
      }

      // We move from down to up
      if (direction == 3) {
        for (int i = down; i >= up; i--) {
          MNKCell cell = new MNKCell(i, left);
          scores.put(cell, score);
        }
        left++;
      }

      if (direction == 3) score += 10;
      direction = (direction + 1) % 4;
    }
  }
}
