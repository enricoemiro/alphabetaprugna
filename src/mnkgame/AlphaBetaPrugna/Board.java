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
  /**
   * Hash of the board.
   * It is updated when a cell is marked or unmarked.
   */
  public long hash;

  /** Zobrist class instance. */
  public ZobristHash zobrist;

  /** Board scores. */
  public Map<MNKCell, Integer> scores = new LinkedHashMap<>();

  /** Random class instance. */
  private Random random;

  /** How much should the score increase in each spiral. */
  private static int SCORE_INCREMENTER = 5;

  /**
   * Board constructor.
   * <p>Initialize the zobrist table and set board scores.</p>
   *
   * Time complexity: O(M * N)
   *
   * @see MNKBoard#MNKBoard(int, int, int)
   */
  public Board(int M, int N, int K) {
    super(M, N, K);

    this.setBoardScores();
    this.zobrist = new ZobristHash(M, N);
    this.random = new Random(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   *
   * Time complexity: O(1)
   */
  public MNKGameState markCell(MNKCell cell) throws IndexOutOfBoundsException {
    MNKGameState gameState = super.markCell(cell.i, cell.j);
    MNKCellState markedCellState = B[cell.i][cell.j];

    // XOR in the new cell
    MNKCell newCell = new MNKCell(cell.i, cell.j, markedCellState);
    this.hash = this.zobrist.updateZobrist(this.hash, newCell);

    return gameState;
  }

  /**
   * @see MNKBoard#unmarkCell()
   *
   * Time complexity: O(1)
   */
  public void unmarkCell() {
    MNKCell lastMarkedCell = MC.getLast();
    super.unmarkCell();

    // XOR out the last marked cell
    this.hash = this.zobrist.updateZobrist(this.hash, lastMarkedCell);
  }

  /**
   * Randomly select a cell from the free cells list.
   *
   * Time complexity: O(1)
   *
   * @return random cell from the free cells list
   */
  public MNKCell pickRandomCell() {
    MNKCell[] FC = this.getFreeCells();
    return FC[this.random.nextInt(FC.length)];
  }

  /**
   * Check if the cell in the input direction exists or
   * is out of bounds.
   *
   * Time complexity: O(1)
   *
   * @param cell  starting cell
   * @param point direction to check on
   * @return the next cell in the input direction, null if
   *         it is out of bound
   */
  public MNKCell cellNullOrExists(MNKCell cell, Point point) {
    int i = cell.i + point.x;
    int j = cell.j + point.y;

    if (!isCellInBounds(i, j)) return null;
    return new MNKCell(i, j, cellState(i, j));
  }

  /**
   * Returns the last marked cell.
   *
   * Time complexity: O(1)
   *
   * @return last marked cell from the marked cells list
   */
  public MNKCell getLastMarkedCell() { return MC.getLast(); }

  /**
   * Returns last marked cell with the input state,
   * from the marked cells list.
   *
   * Time complexity: O(n)
   *
   * @param cellState State of the cell to search for
   * @return the last cell with the input state
   */
  public MNKCell getLastMarkedCell(MNKCellState cellState) {
    if (MC.size() == 0) return null;

    for (int i = MC.size() - 1; i >= 0; i--) {
      MNKCell current = MC.get(i);
      if (current.state == cellState) return current;
    }

    return null;
  }

  /**
   * Check the gamestate of the board.
   *
   * Time complexity: O(1)
   *
   * @return true if the game state is open, false otherwise
   */
  public boolean isGameOpen() {
    return this.gameState().equals(MNKGameState.OPEN);
  }

  /**
   * Checks if the given cell is inside board bounds.
   *
   * Time complexity: O(1)
   *
   * @param MNKCell cell to check
   * @return true if the cell is inside board bounds, false otherwise
   */
  public boolean isCellInBounds(MNKCell cell) {
    boolean isValidRow = cell.i >= 0 && cell.i < M;
    boolean isValidColumn = cell.j >= 0 && cell.j < N;
    return isValidRow && isValidColumn;
  }

  /**
   * Checks if the given coordinate of the cell are inside
   * board bounds.
   *
   * Time complexity: O(1)
   *
   * @param i cell row
   * @param j cell column
   * @return true if the cell is inside board bounds, false otherwise
   * @see Board#isCellInBounds(MNKCell)
   */
  public boolean isCellInBounds(int i, int j) {
    return isCellInBounds(new MNKCell(i, j));
  }

  /**
   * Get the free cells HashSet
   *
   * Time complexity: O(1)
   *
   * @return the free cells HashSet
   */
  public HashSet<MNKCell> getFCSet() { return this.FC; }

  /**
   * Get the score associated to the given cell.
   *
   * Time complexity: O(1)
   *
   * @param cell the cell from which to take the state
   * @return the score associated to the given cell
   */
  public int getCellScore(MNKCell cell) {
    return scores.get(new MNKCell(cell.i, cell.j));
  }

  /**
   * Associate a score to each cell on the board.
   * It is called in the class constructor.
   *
   * Time complexity: Θ(M*N)
   */
  private void setBoardScores() {
    /**
     * clang-format off
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
     * clang-format on
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

      if (direction == 3) score += SCORE_INCREMENTER;
      direction = (direction + 1) % 4;
    }
  }
}
