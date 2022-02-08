package mnkgame.AlphaBetaPrugna;

import java.util.Random;

import mnkgame.MNKCell;
import mnkgame.MNKCellState;

public class ZobristHash {
  final private Random random;
  final public long table[][][];

  public ZobristHash(int M, int N) {
    this.random = new Random(System.currentTimeMillis());
    this.table = new long[2][M][N];
    this.initZobrist(M, N);
  }

  public long updateZobrist(long zobrist, MNKCell cell) {
    return zobrist ^ table[player(cell)][cell.i][cell.j];
  }

  private void initZobrist(int M, int N) {
    for (int i = 0; i < 2; i++)
      for (int row = 0; row < M; row++)
        for (int col = 0; col < N; col++)
          table[i][row][col] = this.random.nextLong();
  }

  private int player(MNKCell cell) {
    if (cell.state == MNKCellState.FREE) {
      System.out.println("ZobristHash: free cell in player()");
      System.exit(1);
    }

    return cell.state == MNKCellState.P1 ? 0 : 1;
  }
}
