package mnkgame.AlphaBetaPrugna;

import java.util.HashSet;
import java.util.Random;
import mnkgame.MNKBoard;
import mnkgame.MNKCell;
import mnkgame.MNKCellState;
import mnkgame.MNKGameState;

final public class Board extends MNKBoard {
  private Random random;

  /**
   * {@inheritDoc}
   */
  public Board(int M, int N, int K) {
    super(M, N, K);

    this.random = new Random(System.currentTimeMillis());
  }

  /**
   * {@inheritDoc}
   */
  public MNKGameState markCell(MNKCell cell) throws IndexOutOfBoundsException {
    return super.markCell(cell.i, cell.j);
  }

  /**
   * {@return random cell from the free cells list}
   */
  public MNKCell pickRandomCell() {
    MNKCell[] FC = this.getFreeCells();
    return FC[this.random.nextInt(0, FC.length)];
  }

  /**
   * {@return last marked cell from the marked cells list}
   */
  public MNKCell getLastMarkedCell() {
    return MC.getLast();
  }

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
   * {@see Board#isCellInBounds(MNKCell)}
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
}
