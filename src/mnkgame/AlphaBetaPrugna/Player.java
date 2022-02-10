package mnkgame.AlphaBetaPrugna;

import static mnkgame.AlphaBetaPrugna.Constants.allDirections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import mnkgame.AlphaBetaPrugna.TTEntry.Flag;
import mnkgame.MNKCell;
import mnkgame.MNKCellState;
import mnkgame.MNKGameState;
import mnkgame.MNKPlayer;

final public class Player implements MNKPlayer {
  /** Local board. */
  private Board board;
  /** Timeout converted from seconds to milliseconds. */
  private long timeoutInMillis;
  /** We are first player or not. */
  private boolean first;
  /** Game State of our and opponent player. */
  private MNKGameState myWinState, opponentWinState;
  /** Cell State of our and opponent player. */
  private MNKCellState myCellState, opponentCellState;
  /** Instant of the start of our round. */
  private long startTime;
  /** Max time for searchin the best move. */
  private long maxSearchingTime;
  /** Transposition table to mantain visited configurations. */
  private Map<Long, TTEntry> transpositionTable;
  /** Thread where execute cleanup. */
  private Thread transpositionTableCleaner;

  /** A safety limit to exit from Alpha-beta before the end of the round. */
  private static final int SAFETY_THRESHOLD = 95;
  /** A default value to return in Alpha-beta if time is finishing. */
  private static final int SAFETY_HALT = Integer.MAX_VALUE / 2;
  /** Upper bound value of Alpha-beta. */
  private static final int INFINITY_POSITIVE = Integer.MAX_VALUE;
  /** Lower bound value of Alpha-beta. */
  private static final int INFINITY_NEGATIVE = Integer.MIN_VALUE;
  /** Score associated to win configuration. */
  private static final int WINNING_SCORE = 100_000_000;
  /** Score associated to losing configuration. */
  private static final int LOSING_SCORE = -WINNING_SCORE;
  /** Score associated to draw configuration. */
  private static final int DRAWING_SCORE = 0;

  /** Default empty constructor */
  public Player() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void initPlayer(
          int M, int N, int K, boolean first, int timeoutInSecs) {
    this.first = first;
    this.board = new Board(M, N, K);
    this.timeoutInMillis = timeoutInSecs * 1000;
    this.myWinState = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
    this.opponentWinState = first ? MNKGameState.WINP2 : MNKGameState.WINP1;
    this.myCellState = first ? MNKCellState.P1 : MNKCellState.P2;
    this.opponentCellState = first ? MNKCellState.P2 : MNKCellState.P1;
    this.maxSearchingTime = (this.timeoutInMillis * SAFETY_THRESHOLD) / 100;
    this.transpositionTable = new HashMap<>();

    // for (var boardScore : this.board.scores.entrySet()) {
    //   MNKCell cell = boardScore.getKey();
    //   int score = boardScore.getValue();
    //   System.out.format("Cell: %s - Score: %d\n", cell, score);
    // }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MNKCell selectCell(MNKCell[] FC, MNKCell[] MC) {
    this.startTime = System.currentTimeMillis();

    // Stop the thread that (if it) was in background
    if (transpositionTableCleaner != null
            && transpositionTableCleaner.isAlive()) {
      transpositionTableCleaner.interrupt();
      transpositionTableCleaner = null;
    }

    // Last available move
    if (FC.length == 1) return FC[0];

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

      if (MC.length == 1 && board.M == board.N) {
        if (lastMarkedCell.i != board.M / 2
                && lastMarkedCell.j != board.N / 2) {
          MNKCell cell = new MNKCell(board.M / 2, board.N / 2);
          board.markCell(cell);
          return cell;
        }
      }
    }

    MNKCell bestCell = iterativeDeepening(board);
    if (bestCell == null) bestCell = board.pickRandomCell();
    board.markCell(bestCell);

    // for (var entry : transpositionTable.entrySet()) {
    //   System.out.format("Board hash: %d ---- %s\n",
    //                      entry.getKey(),
    //                      entry.getValue().toString());

    // Start thread in background for cleaning the
    // transposition table
    transpositionTableCleaner = new Thread(new TTCleaner(), "TT Cleaner");
    transpositionTableCleaner.start();

    return bestCell;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String playerName() {
    return "AlphaBetaPrugna";
  }

  /**
   * Class that implements a Runnable object for the
   * transposition cleanup thread.
   */
  private class TTCleaner implements Runnable {
    @Override
    public void run() {
      long hash = 0;

      // Remove from transposition table useless configurations
      for (MNKCell cell : board.getMarkedCells()) {
        hash = board.zobrist.updateZobrist(hash, cell);

        TTEntry ttEntry = transpositionTable.get(hash);
        if (ttEntry != null) transpositionTable.remove(hash);
      }
    }
  }

  /**
   * Calls alpha beta by increasing the depth until the time runs out.
   *
   * @param board board to evaluate
   * @return best cell move
   */
  private MNKCell iterativeDeepening(Board board) {
    MNKCell bestCell = null;

    for (int depth = 1; depth <= board.getFreeCells().length; depth++) {
      List<Object> values = alphaBetaAtRoot(board, depth);
      MNKCell cell = (MNKCell) values.get(0);
      int score = (int) values.get(1);

      if (isTimeFinishing() || score == SAFETY_HALT) break;

      bestCell = cell;
    }

    return bestCell;
  }

  /**
   * Alpha-beta execution on the root of the game tree.
   *
   * @param board board to evaluate
   * @param depth max reachable depth
   * @return list containing bestCell with its bestScore
   */
  private List<Object> alphaBetaAtRoot(Board board, int depth) {
    MNKCell bestCell = null;
    int bestScore = INFINITY_NEGATIVE;

    MNKCell[] sortedMoves = sortMoves(board);
    for (MNKCell cell : sortedMoves) {
      board.markCell(cell);
      int score = alphaBetaWithMemory(
              board, depth, INFINITY_NEGATIVE, INFINITY_POSITIVE, false);
      board.unmarkCell();

      if (isTimeFinishing() || score == SAFETY_HALT) break;

      if (score > bestScore) {
        bestScore = score;
        bestCell = cell;
      }
    }

    return Arrays.asList(bestCell, bestScore);
  }

  /**
   * Implements AlphaBeta algorithm with transposition table.
   *
   * @param board board to evaluate
   * @param depth max reachable depth
   * @param alpha lower bound value
   * @param beta upper bound value
   * @param isMaximizing true if is a maximizing node, false otherwise
   * @return best evaluated score
   */
  private int alphaBetaWithMemory(
          Board board, int depth, int alpha, int beta, boolean isMaximizing) {
    int alphaOrig = alpha;

    /**
     * Transposition table lookup.
     */
    TTEntry entry = transpositionTable.getOrDefault(board.hash, null);
    if (entry != null && entry.depth >= depth) {
      if (entry.flag == Flag.EXACT)
        return entry.score;
      else if (entry.flag == Flag.ALPHA)
        alpha = Math.max(alpha, entry.score);
      else if (entry.flag == Flag.BETA)
        beta = Math.min(beta, entry.score);
      if (alpha >= beta) return entry.score;
    }

    if (isTimeFinishing()) return SAFETY_HALT;
    if (depth == 0 || !board.isGameOpen()) return eval(board, depth);

    MNKCell[] sortedCells = sortMoves(board);
    int eval = isMaximizing ? INFINITY_NEGATIVE : INFINITY_POSITIVE;
    for (MNKCell cell : sortedCells) {
      board.markCell(cell);

      int score =
              alphaBetaWithMemory(board, depth - 1, alpha, beta, !isMaximizing);
      if (isMaximizing)
        alpha = Math.max(alpha, eval = Math.max(eval, score));
      else
        beta = Math.min(beta, eval = Math.min(eval, score));

      board.unmarkCell();
      if (beta <= alpha) break;
    }

    /*
     * Traditional transposition table storing of bounds.
     */
    TTEntry newEntry = new TTEntry();
    newEntry.score = eval;
    newEntry.depth = depth;
    if (eval <= alphaOrig)
      newEntry.flag = Flag.BETA;
    else if (eval >= beta)
      newEntry.flag = Flag.ALPHA;
    else
      newEntry.flag = Flag.EXACT;
    transpositionTable.putIfAbsent(board.hash, newEntry);

    return eval;
  }

  /**
   * It gives a score to the given board given.
   *
   * @param board board to be evaluate
   * @param depth depth reached
   * @return the board score
   */
  private int eval(Board board, int depth) {
    MNKGameState state = board.gameState();

    if (first) {
      if (state.equals(myWinState))
        return WINNING_SCORE + depth;
      else if (state.equals(opponentWinState))
        return LOSING_SCORE - depth;
    } else {
      if (state.equals(opponentWinState))
        return LOSING_SCORE - depth;
      else if (state.equals(myWinState))
        return WINNING_SCORE + depth;
    }

    if (state.equals(MNKGameState.DRAW)) return DRAWING_SCORE + depth;

    MNKCell lastMarked = board.getLastMarkedCell();
    int evalLastMarked = new Eval(board, lastMarked).eval();

    if (first)
      return lastMarked.state == myCellState ? evalLastMarked + depth
                                             : -evalLastMarked - depth;
    else
      return lastMarked.state == myCellState ? -evalLastMarked - depth
                                             : +evalLastMarked + depth;
  }

  /**
   * Sort the cells in a circular way (starting from up-left direction).
   *
   * @param lastCell last marked cell
   * @param FCSize free cells size
   * @return list of sorted moves
   */
  private List<MNKCell> sortCellsInCircularWay(
          Board board, MNKCell lastCell, int FCSize) {
    Map<MNKCell, Integer> sortedFreeCells = new LinkedHashMap<>(FCSize);
    List<Point> copyDirections = new ArrayList<>(allDirections.values());
    List<Point> invalidDirections = new ArrayList<>();

    for (int i = 1; i <= board.K; i++) {
      for (var direction : copyDirections) {
        int x = lastCell.i + direction.x * i;
        int y = lastCell.j + direction.y * i;

        // If for the current value of "i" the cell is out of bounds of
        // the board it will surely be for the following ones too,
        // we can therefore add this "direction" in the list of invalid
        // directions.
        if (!board.isCellInBounds(x, y)) {
          invalidDirections.add(direction);
          continue;
        }

        // We check the status of the cell on the board,
        // if this is Free then we insert it in the "sortedFreeCells"
        if (board.cellState(x, y) == MNKCellState.FREE) {
          MNKCell cell = new MNKCell(x, y);
          sortedFreeCells.putIfAbsent(cell, board.getCellScore(cell));
        }
      }

      // We remove invalid directions from copyDirections
      for (var notValidDirection : invalidDirections)
        copyDirections.remove(notValidDirection);

      // If we have no more directions in which to move,
      // it makes no sense to continue and therefore we
      // can exit the for.
      if (copyDirections.size() == 0) break;

      // Restore to initial state invalidDirections
      invalidDirections.removeAll(invalidDirections);
    }

    // Sort the moves based on the board scores
    Map<MNKCell, Integer> sorted =
            sortedFreeCells.entrySet()
                    .stream()
                    .sorted(Entry.<MNKCell, Integer>comparingByValue()
                                    .reversed())
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                            (e1, e2) -> e1, LinkedHashMap::new));

    return Arrays.asList(sorted.keySet().toArray(new MNKCell[0]));
  }

  /**
   * Sort the moves according to the state of the board.
   *
   * @param board current board
   * @return the array of sorted cells
   */
  private MNKCell[] sortMoves(Board board) {
    HashSet<MNKCell> FC = new HashSet<MNKCell>(board.getFCSet());
    int size = FC.size();
    List<MNKCell> sortedFreeCells = new ArrayList<>();
    MNKCell myLastMarkedCell = board.getLastMarkedCell(myCellState);
    MNKCell opponentLastMarkedCell = board.getLastMarkedCell(opponentCellState);

    if (myLastMarkedCell != null) {
      List<MNKCell> mySortedCells =
              sortCellsInCircularWay(board, myLastMarkedCell, size);
      sortedFreeCells.addAll(mySortedCells);
      FC.removeAll(mySortedCells);
    }

    if (opponentLastMarkedCell != null) {
      List<MNKCell> opponentSortedCells =
              sortCellsInCircularWay(board, opponentLastMarkedCell, size);

      for (MNKCell cell : opponentSortedCells) {
        if (!sortedFreeCells.contains(cell)) {
          sortedFreeCells.add(cell);
          FC.remove(cell);
        }
      }
    }

    sortedFreeCells.addAll(FC);
    return sortedFreeCells.toArray(new MNKCell[0]);
  }

  /**
   * Check if the time is running out.
   *
   * @return true if the maxSearchingTime has been exceeded, false otherwise
   */
  private boolean isTimeFinishing() {
    long elapsedTime = System.currentTimeMillis() - this.startTime;
    return elapsedTime > this.maxSearchingTime;
  }
}
