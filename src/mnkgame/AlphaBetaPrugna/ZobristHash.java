package mnkgame.AlphaBetaPrugna;

import java.util.Random;

import mnkgame.MNKCell;
import mnkgame.MNKCellState;

public class ZobristHash {
  /** Random class instance. */
  final private Random random;

  /**
   * <p>The table entries have the following structure:</p>
   *
   * <p>
   *   <code>[i]: cell_11  ...  cell_1n
   *                ...    ...    ...
   *              cell_1n  ...  cell_mn
   *   </code>
   * <p>
   *
   * where the:
   * <ul>
   *   <li>[i] represent the i-th player index (0 or 1).</li>
   *   <li>cell_ij: represent the i-th row and j-th column of the cell in the
   *       board (in this coordinates we store the random value of the i-th
   *       player).
   *   </li>
   * </ul>
   */
  final public long table[][][];

  /**
   * ZobristHash constructor.
   * Create and initilize the Zobrist table with random long values.
   *
   * <p>Time complexity:
   *    <code>Θ(2(M*N)) = Θ(M*N)<code>
   * </p>
   *
   * @param M the number of rows of the board
   * @param N the number of columns of the board
   * @see ZobristHash#initZobrist(int, int)
   */
  public ZobristHash(int M, int N) {
    this.random = new Random(System.currentTimeMillis());
    this.table = new long[2][M][N];
    this.initZobrist(M, N);
  }

  /**
   * Calculate the new zobrist key.
   *
   * @param zobrist old zobrist key
   * @param cell last marked/unmarked cell
   * @return new zobrist key
   */
  public long updateZobrist(long zobrist, MNKCell cell) {
    return zobrist ^ table[player(cell)][cell.i][cell.j];
  }

  /**
   * Initialize the zobrist table with random long values.
   *
   * <p>Time complexity: <code>Θ(2(M*N)) = Θ(M*N)</code></p>
   *
   * @param players the number of players
   * @param M the number of rows of the board
   * @param N the number of columns of the board
   */
  private void initZobrist(int M, int N) {
    for (int i = 0; i < 2; i++)
      for (int row = 0; row < M; row++)
        for (int col = 0; col < N; col++)
          table[i][row][col] = this.random.nextLong();
  }

  /**
   * Returns 0 or 1 depending on which player the cell belongs to.
   *
   * <p>Time complexity: <code>O(1)</code></p>
   *
   * @param cell
   * @return 0 if the state of the cell is P1, 1 otherwise
   */
  private int player(MNKCell cell) {
    if (cell.state == MNKCellState.FREE) {
      System.out.println("ZobristHash: free cell in player()");
      System.exit(1);
    }

    return cell.state == MNKCellState.P1 ? 0 : 1;
  }
}
