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

import mnkgame.MNKCell;
import mnkgame.MNKCellState;
import mnkgame.MNKGameState;
import mnkgame.MNKPlayer;
import mnkgame.AlphaBetaPrugna.TTEntry.Flag;

final public class Player implements MNKPlayer {
  private Board board;
  private double timeoutInMillis;
  private MNKGameState myWinState, opponentWinState;
  private MNKCellState myCellState, opponentCellState;
  private double startTime, maxSearchingTime;
  private Map<Long, TTEntry> transpositionTable;

  private static final double SAFETY_THRESHOLD = 0.99;
  private static final int SAFETY_HALT = Integer.MAX_VALUE / 2;
  private static final int INFINITY_POSITIVE = Integer.MAX_VALUE;
  private static final int INFINITY_NEGATIVE = Integer.MIN_VALUE;
  private static final int WINNING_SCORE = 100_000_000;
  private static final int LOSING_SCORE = -WINNING_SCORE;
  private static final int DRAWING_SCORE = 0;

  /** Default empty constructor */
  public Player() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initPlayer(int M, int N, int K, boolean first, int timeoutInSecs) {
    this.board = new Board(M, N, K);
    this.timeoutInMillis = (double) timeoutInSecs * 1000;

    this.myWinState = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
    this.opponentWinState = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

    this.myCellState = first ? MNKCellState.P1 : MNKCellState.P2;
    this.opponentCellState = first ? MNKCellState.P2 : MNKCellState.P1;

    this.maxSearchingTime = this.timeoutInMillis * SAFETY_THRESHOLD;

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
    if (bestCell == null) {
      System.out.println("Player: random move in select cell");
      //for (var entry : transpositionTable.entrySet()) {
      //  System.out.format("Board hash: %d ---- %s\n",
      //                     entry.getKey(),
      //                     entry.getValue().toString());
      //}
      bestCell = board.pickRandomCell();
    }
    board.markCell(bestCell);

    return bestCell;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String playerName() {
    return "GEmirator";
  }

  /**
   *
   *
   * @param board Board to evaluate
   * @return The best cell move
   */
  public MNKCell iterativeDeepening(Board board) {
    MNKCell bestCell = null;
    int bestScore = 0;

    for (int depth = 1; depth <= board.getFreeCells().length; depth++) {
      List<Object> values = alphaBetaAtRoot(board, depth);
      MNKCell cell = (MNKCell) values.get(0);
      int score = (int) values.get(1);

      if (isTimeFinishing() || score == SAFETY_HALT) break;

//      if (score >= bestScore) {
//        System.out.format("old: bestScore: %d - bestCell: %s\n"
//            + "new: bestScore: %d - bestCell: %s\n"
//            + "depth: %d\n",
//            bestScore, bestCell, score, cell, depth);
//
//        bestScore = score;
//        bestCell = cell;
//      }
      bestScore = score;
      bestCell = cell;
    }

    return bestCell;
  }

  /**
   *
   * @param board
   * @param depth
   * @return
   */
  public List<Object> alphaBetaAtRoot(Board board, int depth) {
    MNKCell bestCell = null;
    int bestScore = INFINITY_NEGATIVE;

    MNKCell[] sortedMoves = sortMoves(board);

    for (MNKCell cell : sortedMoves) {
      board.markCell(cell);
      int score = alphaBetaWithMemory(board, depth, INFINITY_NEGATIVE, INFINITY_POSITIVE, false);
      board.unmarkCell();

      if (isTimeFinishing() || score == SAFETY_HALT) break;

      if (score > bestScore) {
        bestScore = score;
        bestCell = cell;
      }
    }

    return Arrays.asList(bestCell, bestScore);
  }

  private int alphaBetaWithMemory(Board board, int depth, int alpha, int beta,
                                  boolean isMaximizing) {
    int alphaOrig = alpha;

    /**
     * Transposition table lookup
     */
    TTEntry entry = transpositionTable.getOrDefault(board.hash, null);
    if (entry != null && entry.depth >= depth) {
      if (entry.flag == Flag.EXACT) return entry.score;
      else if (entry.flag == Flag.ALPHA) alpha = Math.max(alpha, entry.score);
      else if (entry.flag == Flag.BETA) beta = Math.min(beta, entry.score);
      if (alpha >= beta) return entry.score;
    }

    if (isTimeFinishing()) return SAFETY_HALT;
    if (depth == 0 || !board.isGameOpen()) return eval(board, depth);

    MNKCell[] sortedCells = sortMoves(board);
    int eval = isMaximizing ? INFINITY_NEGATIVE : INFINITY_POSITIVE;
    for (MNKCell cell : sortedCells) {
      board.markCell(cell);

      int score = alphaBetaWithMemory(board, depth - 1, alpha, beta, !isMaximizing);
      if (isMaximizing) alpha = Math.max(alpha, eval = Math.max(eval, score));
      else beta = Math.min(beta, eval = Math.min(eval, score));

      board.unmarkCell();
      if (beta <= alpha) break;
    }

    /*
     * Traditional transposition table storing of bounds
     */
    TTEntry newEntry = new TTEntry();
    newEntry.score = eval;
    newEntry.depth = depth;
    if (eval <= alphaOrig) newEntry.flag = Flag.BETA;
    else if (eval >= beta) newEntry.flag = Flag.ALPHA;
    else newEntry.flag = Flag.EXACT;
    transpositionTable.putIfAbsent(board.hash, newEntry);
    /* Fail low result implies an upper bound */
//    if (eval <= alpha) newEntry.flag = Flag.ALPHA;
//
//    /* Found an accurate minimax value - will not occur if called with zero window */
//    else if (eval > alpha && eval < beta) newEntry.flag = Flag.BETA;
//
//    /* Found an accurate minimax value – will not occur if called with zero window */
//    if (eval >= alpha) newEntry.flag = Flag.EXACT;

    return eval;
  }

  /**
   *
   * @param f
   * @param d
   * @return
   */
//  private int mtdf(Board board, int bestScore, int depth) {
//    int g = bestScore;
//    int upperbound = INFINITY_POSITIVE;
//    int lowerbound = INFINITY_NEGATIVE;
//
//    while (lowerbound < upperbound) {
//      int beta = Math.max(g, lowerbound + 1);
//      g = alphaBetaWithMemory(board, depth, beta - 1, beta, true);
//      if (g < beta) upperbound = g;
//      else lowerbound = g;
//    }
//
//    return g;
//  }

  /**
   * It gives a score to the board passed as a parameter
   *
   * @param board Board to be evaluated
   * @param depth Depth reached
   * @return the score evaluated
   */
  private int eval(Board board, int depth) {
    MNKGameState state = board.gameState();

    if (state.equals(myWinState)) return WINNING_SCORE + depth;
    else if (state.equals(opponentWinState)) return LOSING_SCORE + depth;
    else if (state.equals(MNKGameState.DRAW)) return DRAWING_SCORE + depth;

    MNKCell lastMarked = board.getLastMarkedCell();
    int evalLastMarked = new Eval(board, lastMarked).eval();

    return lastMarked.state == myCellState ? evalLastMarked + depth
                                           : -evalLastMarked - depth;
  }

  /**
   * Sort the cells in a circular way (starting from up-left).
   *
   * @param lastCell Last marked cell
   * @param FCSize Free cells size
   * @return list of sorted moves
   */
  private List<MNKCell> sortCellsInCircularWay(Board board, MNKCell lastCell, int FCSize) {
    Map<MNKCell, Integer> sortedFreeCells = new LinkedHashMap<>(FCSize);
    List<Point> copyDirections = new ArrayList<>(allDirections.values());
    List<Point> invalidDirections = new ArrayList<>();

    for (int i = 1; i <= board.K; i++) {
      for (var direction : copyDirections) {
        int x = lastCell.i + direction.x * i;
        int y = lastCell.j + direction.y * i;

        // If for the current value of "i" the cell is out of bounds of
        // the board it will surely be for the following ones too,
        // we can therefore add this "direction" in the list of invalid directions.
        if (!board.isCellInBounds(x, y)) {
          invalidDirections.add(direction);
          continue;
        }

        // Controlliamo lo stato della cella nella board,
        // se questo è Free allora la inseriamo nelle "sortedFreeCells"
        if (board.cellState(x, y) == MNKCellState.FREE) {
          MNKCell cell = new MNKCell(x, y);
          sortedFreeCells.putIfAbsent(cell, board.getCellScore(cell));
        }
      }

      // Eliminiamo da copyDirections le direzioni non valide
      for (var notValidDirection : invalidDirections)
        copyDirections.remove(notValidDirection);

      // Se non abbiamo più direzioni in cui muoverci non ha
      // senso continuare e quindi possiamo uscire dal for.
      if (copyDirections.size() == 0) break;

      // Restore to initial state invalidDirections
      invalidDirections.removeAll(invalidDirections);
    }

    // for (var item : sortedFreeCells.entrySet()) {
    //   System.out.format("%s, %d\n", item.getKey(), item.getValue());
    // }
    // System.out.println("\n\n");

    Map<MNKCell, Integer> sorted =
      sortedFreeCells.entrySet().stream()
                     .sorted(Entry.<MNKCell, Integer>comparingByValue().reversed())
                     .collect(Collectors.toMap(Entry::getKey,
                                               Entry::getValue,
                                               (e1, e2) -> e1,
                                               LinkedHashMap::new));

    // Sort the moves based on the board scores
    // for (var item : sorted.entrySet()) {
    //   System.out.format("%s, %d\n", item.getKey(), item.getValue());
    // }
    // System.exit(1);

    return Arrays.asList(sorted.keySet().toArray(new MNKCell[0]));
  }

  /**
   * Sort the moves according to the state of the board
   *
   * @param board Current board
   * @return the array of sorted cells
   */
  private MNKCell[] sortMoves(Board board) {
    HashSet<MNKCell> FC = new HashSet<MNKCell>(board.getFCSet());
    int size = FC.size();
    List<MNKCell> sortedFreeCells = new ArrayList<>();
    MNKCell myLastMarkedCell = board.getLastMarkedCell(myCellState);
    MNKCell opponentLastMarkedCell = board.getLastMarkedCell(opponentCellState);

    if (myLastMarkedCell != null) {
      List<MNKCell> mySortedCells = sortCellsInCircularWay(board, myLastMarkedCell, size);
      sortedFreeCells.addAll(mySortedCells);
      FC.removeAll(mySortedCells);
    }

    if (opponentLastMarkedCell != null) {
      List<MNKCell> opponentSortedCells = sortCellsInCircularWay(board, opponentLastMarkedCell, size);

      // We put the remaining elements of opponentSortedCells
      // in sortedFreeCells
      for (MNKCell cell : opponentSortedCells) {
        if (!sortedFreeCells.contains(cell)) {
          sortedFreeCells.add(cell);
          FC.remove(cell);
        }
      }
    }

    // merge remaining free cells
    sortedFreeCells.addAll(FC);

    // for (var s : sortedFreeCells) {
    //   System.out.format("(%d, %d)\n", s.i, s.j);
    // }
    // System.exit(1);

    return sortedFreeCells.toArray(new MNKCell[0]);
  }

  /**
   * @return true if the maxSearchingTime has been exceeded, false otherwise}
   */
  private boolean isTimeFinishing() {
    double elapsedTime = System.currentTimeMillis() - this.startTime;
    return elapsedTime > this.maxSearchingTime;
  }
}
