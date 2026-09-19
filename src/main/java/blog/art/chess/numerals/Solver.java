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

import blog.art.chess.numerals.Moves.Move;
import blog.art.chess.numerals.Moves.NullMove;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class Solver {

  static void goPerft(Position position, int nPlies) {
    long begin = System.currentTimeMillis();
    List<Move> pseudoLegalMoves = new ArrayList<>();
    if (Engine.isLegal(position, pseudoLegalMoves)) {
      long nNodes = count(position, nPlies, pseudoLegalMoves, true);
      long end = System.currentTimeMillis();
      System.out.printf("Nodes searched: %d%n", nNodes);
      System.out.printf("info time %d%n", end - begin);
    } else {
      System.out.println("info string Illegal position");
    }
  }

  private static long count(Position position, int nPlies, List<Move> pseudoLegalMoves,
      boolean verbose) {
    if (nPlies == 0) {
      return 1;
    }
    long nNodes = 0;
    for (Move move : pseudoLegalMoves) {
      List<Move> pseudoLegalMovesNext = new ArrayList<>();
      Optional<Position> positionNext = Engine.makeMove(position, move, pseudoLegalMovesNext);
      if (positionNext.isPresent()) {
        long nChildNodes = count(positionNext.get(), nPlies - 1, pseudoLegalMovesNext, false);
        nNodes += nChildNodes;
        if (verbose) {
          System.out.printf("%s: %d%n", Engine.toUciCode(move), nChildNodes);
        }
      }
    }
    return nNodes;
  }

  private record Variation(int value, List<Move> moves) {

  }

  static void goMate(Position position, int nMoves) {
    long begin = System.currentTimeMillis();
    List<Move> pseudoLegalMoves = new ArrayList<>();
    if (Engine.isLegal(position, pseudoLegalMoves)) {
      List<Variation> variations = new ArrayList<>();
      for (Move move : pseudoLegalMoves) {
        List<Move> pseudoLegalMovesMin = new ArrayList<>();
        Optional<Position> positionMin = Engine.makeMove(position, move, pseudoLegalMovesMin);
        if (positionMin.isPresent()) {
          Variation variationMin = searchMin(positionMin.get(), nMoves, pseudoLegalMovesMin);
          int distance =
              variationMin.value > 0 ? nMoves - variationMin.value + 1 : Integer.MAX_VALUE;
          List<Move> moves = new ArrayList<>(variationMin.moves);
          moves.addFirst(move);
          variations.add(new Variation(distance, moves));
          if (distance <= nMoves) {
            System.out.printf("info string %s: mate in %d%n", Engine.toUciCode(move), distance);
          } else {
            System.out.printf("info string %s: no mate in %d%n", Engine.toUciCode(move), nMoves);
          }
        }
      }
      long end = System.currentTimeMillis();
      if (!variations.isEmpty()) {
        variations.sort(Comparator.comparingInt(Variation::value));
        Variation principalVariation = variations.getFirst();
        if (principalVariation.value <= nMoves) {
          List<String> tokens = new ArrayList<>();
          for (Move move : principalVariation.moves) {
            tokens.add(Engine.toUciCode(move));
          }
          System.out.printf("info time %d score mate %d pv %s%n", end - begin,
              principalVariation.value, String.join(" ", tokens));
        } else {
          System.out.printf("info time %d%n", end - begin);
        }
        System.out.printf("bestmove %s%n", Engine.toUciCode(principalVariation.moves.getFirst()));
      } else {
        System.out.printf("info time %d%n", end - begin);
        System.out.printf("bestmove %s%n", Engine.toUciCode(new NullMove()));
      }
    } else {
      System.out.println("info string Illegal position");
    }
  }

  private static Variation searchMax(Position positionMax, int nMoves,
      List<Move> pseudoLegalMovesMax) {
    int valueMax = -1;
    List<Move> movesMax = new ArrayList<>();
    for (Move moveMax : pseudoLegalMovesMax) {
      List<Move> pseudoLegalMovesMin = new ArrayList<>();
      Optional<Position> positionMin = Engine.makeMove(positionMax, moveMax, pseudoLegalMovesMin);
      if (positionMin.isPresent()) {
        Variation variationMin = searchMin(positionMin.get(), nMoves, pseudoLegalMovesMin);
        if (variationMin.value > valueMax) {
          valueMax = variationMin.value;
          movesMax = new ArrayList<>(variationMin.moves);
          movesMax.addFirst(moveMax);
          if (valueMax == nMoves) {
            break;
          }
        }
      }
    }
    return new Variation(valueMax, movesMax);
  }

  private static Variation searchMin(Position positionMin, int nMoves,
      List<Move> pseudoLegalMovesMin) {
    int valueMin = 0;
    List<Move> movesMin = new ArrayList<>();
    if (nMoves == 1) {
      for (Move moveMin : pseudoLegalMovesMin) {
        if (Engine.makeMove(positionMin, moveMin, null).isPresent()) {
          valueMin = -1;
          break;
        }
      }
    } else {
      for (Move moveMin : pseudoLegalMovesMin) {
        List<Move> pseudoLegalMovesMax = new ArrayList<>();
        Optional<Position> positionMax = Engine.makeMove(positionMin, moveMin, pseudoLegalMovesMax);
        if (positionMax.isPresent()) {
          Variation variationMax = searchMax(positionMax.get(), nMoves - 1, pseudoLegalMovesMax);
          if (valueMin == 0 || variationMax.value < valueMin) {
            valueMin = variationMax.value;
            movesMin = new ArrayList<>(variationMax.moves);
            movesMin.addFirst(moveMin);
            if (valueMin == -1) {
              break;
            }
          }
        }
      }
    }
    if (valueMin == 0) {
      valueMin = Engine.makeMove(positionMin, new NullMove(), null).isPresent() ? -1 : nMoves;
    }
    return new Variation(valueMin, movesMin);
  }
}
