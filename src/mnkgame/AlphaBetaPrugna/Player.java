package mnkgame.AlphaBetaPrugna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mnkgame.MNKCell;
import mnkgame.MNKCellState;
import mnkgame.MNKGameState;
import mnkgame.MNKPlayer;

final public class Player implements MNKPlayer {
  public Board board;
  public double timeoutInMillis;
  public MNKGameState myWinState, opponentWinState;
  public MNKCellState myCellState, opponentCellState;
  private double startTime, maxSearchingTime;
  private Map<MNKCell, Integer> boardScores;

  private static final double SAFETY_THRESHOLD = 0.95;
  private static final int SAFETY_HALT = Integer.MAX_VALUE / 2;
  private static final int INFINITY_POSITIVE = Integer.MAX_VALUE;
  private static final int INFINITY_NEGATIVE = Integer.MIN_VALUE;
  private static final int WINNING_SCORE = 100_000_000;
  private static final int LOSING_SCORE = -100_000_000;
  private static final int DRAWING_SCORE = 0;
  private static HashMap<String, Direction> directions = Direction.getDirections();

  /**
   * Default empty constructor
   */
  public Player() {
  }

  /**
   * Associate a score to each cell on the board.
   *
   * @param M Number of rows
   * @param N Number of columns
   */
  private void setBoardScores(int M, int N) {
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

    System.out.println(up + " " + down + " " +
                       left + " " + right);
    int score = 1;
    while (up <= down && left <= right) {
      // We move from left to right
      if (direction == 0) {
        for (int i = left; i <= right; i++) {
          MNKCell cell = new MNKCell(up, i);
          boardScores.put(cell, score);
        }
        up++;
      }

      // We move from up to down
      if (direction == 1) {
        for (int i = up; i <= down; i++) {
          MNKCell cell = new MNKCell(i, right);
          boardScores.put(cell, score);
        }
        right--;
      }

      // We move from right to left
      if (direction == 2) {
        for (int i = right; i >= left; i--) {
          MNKCell cell = new MNKCell(down, i);
          boardScores.put(cell, score);
        }
        down--;
      }

      // We move from down to up
      if (direction == 3) {
        for (int i = down; i >= up; i--) {
          MNKCell cell = new MNKCell(i, left);
          boardScores.put(cell, score);
        }
        left++;
      }

      if (direction == 3) score = score * 10;
      direction = (direction + 1) % 4;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initPlayer(int M, int N, int K, boolean first,
      int timeoutInSecs) {
    this.board = new Board(M, N, K);
    this.timeoutInMillis = (double) timeoutInSecs * 1000;

    this.myWinState = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
    this.opponentWinState = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

    this.myCellState = first ? MNKCellState.P1 : MNKCellState.P2;
    this.opponentCellState = first ? MNKCellState.P2 : MNKCellState.P1;

    this.maxSearchingTime = this.timeoutInMillis * SAFETY_THRESHOLD;

    this.boardScores = new LinkedHashMap<>(M * N);
    setBoardScores(M, N);

    for (var boardScore : this.boardScores.entrySet()) {
      MNKCell cell = boardScore.getKey();
      int score = boardScore.getValue();
      System.out.format("Cell: %s - Score: %d\n", cell, score);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
    this.startTime = System.currentTimeMillis();

    // Last available move
    if (FC.length == 1)
      return FC[0];

    // If we have the first move, we choose the middle cell
    if (MC.length == 0) {
      MNKCell cell = new MNKCell(board.M / 2, board.N / 2);
      board.markCell(cell);
      return cell;
    }

    // Update local board with the opponent last move
    if (MC.length > 0) {
      MNKCell lastMarkedCell = MC[MC.length - 1];
      board.markCell(lastMarkedCell);
    }

    // AlphaBeta algorithm to found the best cell
    MNKCell bestCell = iterativeDeepening(board);
    if (bestCell == null)
      bestCell = board.pickRandomCell();
    board.markCell(bestCell);

    return bestCell;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String playerName() {
    return "AlphaBetaPrugna";
  }

  public MNKCell iterativeDeepening(Board board) {
    MNKCell bestCell = null;
    int bestScore = INFINITY_NEGATIVE;

    for (int depth = 1; depth <= board.getFreeCells().length; depth++) {
      List<Object> values = alphaBetaAtRoot(board, depth);
      MNKCell cell = (MNKCell) values.get(0);
      int score = (int) values.get(1);

      if (isTimeFinishing() || score == SAFETY_HALT)
        break;

      if (score >= bestScore) {
        System.out.format("old: bestScore: %d - bestCell: %s\n"
            + "new: bestScore: %d - bestCell: %s\n"
            + "depth: %d\n",
            bestScore, bestCell, score, cell, depth);

        bestScore = score;
        bestCell = cell;
      }
    }

    return bestCell;
  }

  public List<Object> alphaBetaAtRoot(Board board, int depth) {
    MNKCell bestCell = null;
    int bestScore = INFINITY_NEGATIVE;

    MNKCell[] sortedMoves = sortMoves(board);
    for (MNKCell cell : sortedMoves) {
      board.markCell(cell);
      int score = alphaBeta(board, depth, INFINITY_NEGATIVE, INFINITY_POSITIVE, false);
      board.unmarkCell();

      if (isTimeFinishing() || score == SAFETY_HALT) break;

      if (score >= bestScore) {
        bestScore = score;
        bestCell = cell;
      }
    }

    return Arrays.asList(bestCell, bestScore);
  }

  public int alphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximizing) {
    if (isTimeFinishing()) return SAFETY_HALT;
    if (depth == 0 || !board.isGameOpen()) return eval(board, depth);

    MNKCell[] sortedCells = sortMoves(board);
    int eval = isMaximizing ? INFINITY_NEGATIVE : INFINITY_POSITIVE;
    for (MNKCell cell : sortedCells) {
      board.markCell(cell);

      int score = alphaBeta(board, depth - 1, alpha, beta, !isMaximizing);
      if (isMaximizing) alpha = Math.max(alpha, eval = Math.max(eval, score));
      else beta = Math.min(beta, eval = Math.min(eval, score));

      board.unmarkCell();
      if (beta <= alpha) break;
    }

    return eval;
  }

  private int eval(Board board, int depth) {
    MNKGameState state = board.gameState();

    if (state.equals(myWinState)) return WINNING_SCORE + depth;
    else if (state.equals(opponentWinState)) return LOSING_SCORE + depth;
    else if (state.equals(MNKGameState.DRAW)) return DRAWING_SCORE + depth;

    int myEval = (int) new Eval(board.getLastMarkedCell()).evalCell();

//    MNKCellState spaghettiCode = board.getLastMarkedCell().state == myCellState
//          ? opponentCellState : myCellState;
//
//     MNKCell prova = new MNKCell(board.getLastMarkedCell().i,
//     board.getLastMarkedCell().j, spaghettiCode);
//
//     int opponentEval = (int) new Eval(prova).evalCell();
//
      return myEval + depth; // + opponentEval;
  }

  /**
   * Sort the cells in a circular way (starting from up-left).
   *
   * @param lastCell Last marked cell
   * @param FCSize Free cells size
   * @return list of sorted moves
   */
  private List<MNKCell> sortCellsInCircularWay(MNKCell lastCell, int FCSize) {
    Map<MNKCell, String> sortedFreeCells = new LinkedHashMap<>(FCSize);
    Map<String, Direction> copyDirections = Direction.getDirections();
    List<Direction> invalidDirections = new ArrayList<Direction>();

    for (int i = 1; i <= board.K; i++) {
      for (var direction : copyDirections.entrySet()) {
        int x = lastCell.i + direction.getValue().point.x * i;
        int y = lastCell.j + direction.getValue().point.y * i;

        // If for the current value of "i" the cell is out of bounds of
        // the board it will surely be for the following ones too,
        // we can therefore add this "direction" in the list of invalid directions.

        // If the cell is out of the board bounds for the current
        // "i" value, it will certainly be out of bounds for the next ones,
        // so we can add this "direction" to the list of invalid directions.

        // Se per il valore di "i" corrente la cella è fuori dai limiti
        // della board lo sarà sicuramente anche per i successivi,
        // possiamo quindi aggiungere tale "direction" nella lista
        // delle direzioni non valide.

        // Se la cella è fuori dai limiti della board per il valore di
        // "i" corrente lo sarà sicuramente anche per i successivi,
        // dunque possiamo aggiungere tale "direction" nella lista
        // delle direzioni non valide.
        if (!board.isCellInBounds(x, y)) {
          invalidDirections.add(direction.getValue());
          continue;
        }

        // Controlliamo lo stato della cella nella board,
        // se questo è Free allora la inseriamo nelle "sortedFreeCells"
        if (board.cellState(x, y) == MNKCellState.FREE) {
          MNKCell cell = new MNKCell(x, y);
          sortedFreeCells.putIfAbsent(cell, null);
        }
      }

      // Eliminiamo da copyDirections le direzioni non valide
      for (var notValidDirection : invalidDirections)
        copyDirections.remove(notValidDirection, null);

      // Se non abbiamo più direzioni in cui muoverci non ha
      // senso continuare e quindi possiamo uscire dal for.
      if (copyDirections.size() == 0) break;

      // Restore to initial state invalidDirections
      invalidDirections.removeAll(invalidDirections);
    }

    return Arrays.asList(sortedFreeCells.keySet().toArray(new MNKCell[0]));
  }

  /**
   * Given the board, sort the free cells as follows:
   * - my moves ∩ opponent moves
   * - my moves
   * - opponent moves
   * - all the others
   *
   * @param board
   * @return array of sorted moves
   */
  private MNKCell[] sortMoves(Board board) {
    HashSet<MNKCell> FC = board.getFCSet();

    //
    MNKCell myLastMarkedCell = board.getLastMarkedCell(myCellState);
    MNKCell opponentLastMarkedCell = board.getLastMarkedCell(opponentCellState);

    if (myLastMarkedCell == null || opponentLastMarkedCell == null)
      return board.getFreeCells();

    // Ordina le mosse in modo circolare
    List<MNKCell> mySortedCells = sortCellsInCircularWay(myLastMarkedCell, FC.size());
    List<MNKCell> opponentSortedCells = sortCellsInCircularWay(opponentLastMarkedCell, FC.size());

    List<MNKCell> sortedFreeCellsList = new ArrayList<MNKCell>();

    // Intersection between mySortedCells and opponentSortedCells
    List<MNKCell> intersection = new ArrayList<>();

    // We look for the common elements between
    // mysortedCells and opponentsSortedCells
    // and insert them in itersection
    for (MNKCell cell: mySortedCells) {
      if (mySortedCells.contains(cell) && opponentSortedCells.contains(cell))
        intersection.add(cell);
    }
    sortedFreeCellsList.addAll(intersection);

    // We put the remaining elements
    // of mySortedCells in sortedFreeCellsList
    for (MNKCell cell : mySortedCells) {
      if (!sortedFreeCellsList.contains(cell))
        sortedFreeCellsList.add(cell);
    }

    // We put the remaining elements of opponentSortedCells
    // in sortedFreeCellsList
    for (MNKCell cell : opponentSortedCells) {
      if (!sortedFreeCellsList.contains(cell))
        sortedFreeCellsList.add(cell);
    }

    // merge remaining free cells
    for (MNKCell cell: FC) {
      if (!sortedFreeCellsList.contains(cell)) sortedFreeCellsList.add(cell);
    }

    return sortedFreeCellsList.toArray(new MNKCell[0]);
  }

  /**
   * {@return true if the maxSearchingTime has been exceeded, false otherwise}
   */
  private boolean isTimeFinishing() {
    double elapsedTime = System.currentTimeMillis() - this.startTime;
    return elapsedTime > this.maxSearchingTime;
  }

  final public class Eval {
    private MNKCell startCell;

    public Eval(MNKCell startCell) {
      this.startCell = startCell;
    }

    /**
     * PROBLEMI:
     * - celle ai limiti bisogna considerarle come possibili linee vincenti
     * altrimenti si sballano i calcoli
     *
     *
     *
     * @param direction
     * @return The score of a k-line of cells in the input direction
     */
    private long evalDirection(Direction direction) {
      long score = 0;
      int numberOfConsecutiveCells = 0;
      Point point = direction.point;
      boolean hasTouchedEnemyCell = false;
      boolean hasTouchedFreeCell = false;

      // viene settata a true quando siamo sicuri che la cella non è fuori
      // dalla Board alla prima iterazione. In questo modo evitiamo che venga
      // assegnato
      // uno score a una cella non raggiungibile.
      // Dobbiamo controllarlo prima di creare la cella perchè non si può
      // cambiare lo stato di una cella.
      boolean hasEnteredInCycle = false;

      for (int i = 1; i <= board.K; i++) {
        int x = startCell.i + point.x * i;
        int y = startCell.j + point.y * i;

        if (!board.isCellInBounds(x, y)) break;

        hasEnteredInCycle = true;
        MNKCell cell = new MNKCell(x, y, board.cellState(x, y));
        boolean isStartCellState = cell.state == this.startCell.state;
        boolean isFreeCellState = cell.state == MNKCellState.FREE;

        if (isStartCellState) {
          score += 1;

          if (!hasTouchedFreeCell) numberOfConsecutiveCells++;
        } else if (isFreeCellState) {
          hasTouchedFreeCell = true;
        } else {
          hasTouchedEnemyCell = true;
          break;
        }
      }

      if (!hasTouchedEnemyCell && hasEnteredInCycle)
        score += 1;

      // score = numberOfConsecutiveCells >= board.K - 2
      //   ? score + numberOfConsecutiveCells
      //   : score;

      return score + series(numberOfConsecutiveCells);
    }

    public long evalCell() {
      long score = 0;

      for (var direction : directions.entrySet())
        score += this.evalDirection(direction.getValue());

      MNKCell startCellWithoutState = new MNKCell(this.startCell.i, this.startCell.j);
      int cellScoreInBoard = boardScores.get(startCellWithoutState);
      return score + cellScoreInBoard;
    }

    /**
     *
     *
     * @param numberOfConsecutiveCells
     * @return
     */
    private int series(int numberOfConsecutiveCells) {
      int K = board.K;

      if (K > 2 && numberOfConsecutiveCells == K - 1) {
        return 100_000;
      } else if (K > 3 && numberOfConsecutiveCells == K - 2) {
        return 100;
      } else if (K > 4 && numberOfConsecutiveCells == K - 3) {
        return 10;
      }

      return 0;
    }
  }
}
