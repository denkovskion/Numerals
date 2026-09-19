/*
 * MIT License
 *
 * Copyright (c) 2026 Ivan Denkovski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package blog.art.chess.numerals;

import blog.art.chess.numerals.Moves.Capture;
import blog.art.chess.numerals.Moves.Castling;
import blog.art.chess.numerals.Moves.DoubleStep;
import blog.art.chess.numerals.Moves.EnPassant;
import blog.art.chess.numerals.Moves.Move;
import blog.art.chess.numerals.Moves.NullMove;
import blog.art.chess.numerals.Moves.Promotion;
import blog.art.chess.numerals.Moves.PromotionCapture;
import blog.art.chess.numerals.Moves.QuietMove;
import blog.art.chess.numerals.Pieces.Bishop;
import blog.art.chess.numerals.Pieces.Category;
import blog.art.chess.numerals.Pieces.King;
import blog.art.chess.numerals.Pieces.Knight;
import blog.art.chess.numerals.Pieces.Pawn;
import blog.art.chess.numerals.Pieces.Piece;
import blog.art.chess.numerals.Pieces.Queen;
import blog.art.chess.numerals.Pieces.Rook;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class Engine {

  static boolean isLegal(Position position, List<Move> pseudoLegalMoves) {
    return generateMoves(position.board(), position.blackToMove(), position.castlingOrigins(),
        position.enPassantTarget(), pseudoLegalMoves);
  }

  private static boolean generateMoves(List<Piece> board, boolean blackToMove,
      Set<Integer> castlingOrigins, Integer enPassantTarget, List<Move> moves) {
    for (int origin = 0; origin < 64; origin++) {
      Piece piece = board.get(origin);
      if (piece != null && piece.black() == blackToMove) {
        switch (piece) {
          case Category category -> {
            int[] directions = switch (category) {
              case King _, Queen _ -> new int[]{-9, -8, -7, -1, 1, 7, 8, 9};
              case Rook _ -> new int[]{-8, -1, 1, 8};
              case Bishop _ -> new int[]{-9, -7, 7, 9};
              case Knight _ -> new int[]{-17, -15, -10, -6, 6, 10, 15, 17};
            };
            int maxOffset = switch (category) {
              case King _, Queen _, Rook _, Bishop _ -> 1;
              case Knight _ -> 2;
            };
            for (int direction : directions) {
              for (int square = origin; ; ) {
                int target = square + direction;
                if (target >= 0 && target < 64 && Math.abs(target / 8 - square / 8) <= maxOffset
                    && Math.abs(target % 8 - square % 8) <= maxOffset) {
                  Piece other = board.get(target);
                  if (other != null) {
                    if (other.black() != category.black()) {
                      if (other instanceof King) {
                        return false;
                      }
                      if (moves != null) {
                        moves.add(new Capture(origin, target));
                      }
                    }
                    break;
                  } else {
                    if (moves != null) {
                      moves.add(new QuietMove(origin, target));
                    }
                    if (category instanceof King || category instanceof Knight) {
                      break;
                    }
                  }
                  square = target;
                } else {
                  break;
                }
              }
            }
            if (category instanceof King) {
              if (castlingOrigins.contains(origin)) {
                int[] castlingDirections = {-8, 8};
                for (int direction : castlingDirections) {
                  int target2 = origin + direction;
                  if (board.get(target2) == null) {
                    int target = target2 + direction;
                    if (board.get(target) == null) {
                      if (direction > 0) {
                        int origin2 = target + direction;
                        if (castlingOrigins.contains(origin2)) {
                          if (moves != null) {
                            moves.add(new Castling(origin, target, origin2, target2));
                          }
                        }
                      } else {
                        int stop = target + direction;
                        if (board.get(stop) == null) {
                          int origin2 = stop + direction;
                          if (castlingOrigins.contains(origin2)) {
                            if (moves != null) {
                              moves.add(new Castling(origin, target, origin2, target2));
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          case Pawn(boolean black) -> {
            int[] captureDirections = black ? new int[]{-9, 7} : new int[]{-7, 9};
            int maxOffset = 1;
            for (int direction : captureDirections) {
              int target = origin + direction;
              if (target >= 0 && target < 64 && Math.abs(target / 8 - origin / 8) <= maxOffset
                  && Math.abs(target % 8 - origin % 8) <= maxOffset) {
                Piece other = board.get(target);
                if (other != null) {
                  if (other.black() != black) {
                    if (other instanceof King) {
                      return false;
                    }
                    if (origin % 8 == (black ? 1 : 6)) {
                      Piece[] box = new Piece[]{new Queen(black), new Rook(black),
                          new Bishop(black), new Knight(black)};
                      for (Piece promoted : box) {
                        if (moves != null) {
                          moves.add(new PromotionCapture(origin, target, promoted));
                        }
                      }
                    } else {
                      if (moves != null) {
                        moves.add(new Capture(origin, target));
                      }
                    }
                  }
                } else {
                  if (enPassantTarget != null) {
                    if (target == enPassantTarget) {
                      int stop = (target / 8) * 8 + origin % 8;
                      if (moves != null) {
                        moves.add(new EnPassant(origin, target, stop));
                      }
                    }
                  }
                }
              }
            }
            int direction = black ? -1 : 1;
            int target = origin + direction;
            if (target >= 0 && target < 64 && Math.abs(target / 8 - origin / 8) <= 1
                && Math.abs(target % 8 - origin % 8) <= maxOffset) {
              if (board.get(target) == null) {
                if (origin % 8 == (black ? 1 : 6)) {
                  Piece[] box = new Piece[]{new Queen(black), new Rook(black), new Bishop(black),
                      new Knight(black)};
                  for (Piece promoted : box) {
                    if (moves != null) {
                      moves.add(new Promotion(origin, target, promoted));
                    }
                  }
                } else {
                  if (moves != null) {
                    moves.add(new QuietMove(origin, target));
                  }
                  if (origin % 8 == (black ? 6 : 1)) {
                    int target2 = target + direction;
                    if (board.get(target2) == null) {
                      if (moves != null) {
                        moves.add(new DoubleStep(origin, target2, target));
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  static Optional<Position> makeMove(Position position, Move move, List<Move> pseudoLegalMoves) {
    if (switch (move) {
      case NullMove(), QuietMove(_, _), Capture(_, _) -> true;
      case Castling(int origin, _, _, int target2) -> {
        if (makeMove(position, new NullMove(), null).isPresent()) {
          if (makeMove(position, new QuietMove(origin, target2), null).isPresent()) {
            yield true;
          }
        }
        yield false;
      }
      case DoubleStep(_, _, _), EnPassant(_, _, _), Promotion(_, _, _), PromotionCapture(_, _, _) ->
          true;
    }) {
      List<Piece> board = new ArrayList<>(position.board());
      boolean blackToMove = !position.blackToMove();
      Set<Integer> castlingOrigins = new HashSet<>(position.castlingOrigins());
      Integer enPassantTarget = null;
      switch (move) {
        case NullMove() -> {
        }
        case QuietMove(int origin, int target) -> {
          board.set(target, board.set(origin, null));
          castlingOrigins.remove(origin);
        }
        case Capture(int origin, int target) -> {
          board.set(target, board.set(origin, null));
          castlingOrigins.remove(origin);
          castlingOrigins.remove(target);
        }
        case Castling(int origin, int target, int origin2, int target2) -> {
          board.set(target, board.set(origin, null));
          board.set(target2, board.set(origin2, null));
          castlingOrigins.remove(origin);
          castlingOrigins.remove(origin2);
        }
        case DoubleStep(int origin, int target, int stop) -> {
          board.set(target, board.set(origin, null));
          enPassantTarget = stop;
        }
        case EnPassant(int origin, int target, int stop) -> {
          board.set(stop, null);
          board.set(target, board.set(origin, null));
        }
        case Promotion(int origin, int target, Piece promoted) -> {
          board.set(origin, null);
          board.set(target, promoted);
        }
        case PromotionCapture(int origin, int target, Piece promoted) -> {
          board.set(origin, null);
          board.set(target, promoted);
          castlingOrigins.remove(target);
        }
      }
      Position result = new Position(board, blackToMove, castlingOrigins, enPassantTarget);
      if (isLegal(result, pseudoLegalMoves)) {
        return Optional.of(result);
      }
    }
    return Optional.empty();
  }

  static String toUciCode(Move move) {
    return String.valueOf(switch (move) {
      case NullMove() -> "0000";
      case QuietMove(int origin, int target) -> toUciCode(origin) + toUciCode(target);
      case Capture(int origin, int target) -> toUciCode(origin) + toUciCode(target);
      case Castling(int origin, int target, _, _) -> toUciCode(origin) + toUciCode(target);
      case DoubleStep(int origin, int target, _) -> toUciCode(origin) + toUciCode(target);
      case EnPassant(int origin, int target, _) -> toUciCode(origin) + toUciCode(target);
      case Promotion(int origin, int target, Piece promoted) ->
          toUciCode(origin) + toUciCode(target) + toUciCode(promoted);
      case PromotionCapture(int origin, int target, Piece promoted) ->
          toUciCode(origin) + toUciCode(target) + toUciCode(promoted);
    });
  }

  private static String toUciCode(int square) {
    return new String(new char[]{(char) ('a' + square / 8), (char) ('1' + square % 8)});
  }

  private static String toUciCode(Piece piece) {
    return switch (piece) {
      case King _ -> "k";
      case Queen _ -> "q";
      case Rook _ -> "r";
      case Bishop _ -> "b";
      case Knight _ -> "n";
      case Pawn _ -> "p";
    };
  }

  static Position newPosition(List<Piece> board, boolean blackToMove, Set<Integer> castlingOrigins,
      Integer enPassantTarget) {
    for (boolean value : new boolean[]{false, true}) {
      int frequency = 0;
      for (Piece piece : board) {
        if (piece instanceof King(boolean black) && black == value) {
          frequency++;
        }
      }
      if (!(frequency == 1)) {
        throw new IllegalArgumentException("Not accepted number of kings");
      }
    }
    for (int castlingOrigin : castlingOrigins) {
      int file = castlingOrigin / 8 + 1;
      int rank = castlingOrigin % 8 + 1;
      if (!(board.get(castlingOrigin) instanceof Piece piece && (file == 5 && piece instanceof King
          || (file == 1 || file == 8) && piece instanceof Rook) && (rank == 1 && !piece.black()
          || rank == 8 && piece.black()))) {
        throw new IllegalArgumentException("Not accepted castling rights");
      }
    }
    if (enPassantTarget != null) {
      if (!(enPassantTarget % 8 + 1 == (blackToMove ? 3 : 6)
          && board.get(enPassantTarget + (blackToMove ? -1 : 1)) == null
          && board.get(enPassantTarget) == null && board.get(
          enPassantTarget + (blackToMove ? 1 : -1)) instanceof Pawn(boolean black)
          && black != blackToMove)) {
        throw new IllegalArgumentException("Not accepted en passant square");
      }
    }
    return new Position(new ArrayList<>(board), blackToMove, new HashSet<>(castlingOrigins),
        enPassantTarget);
  }
}
