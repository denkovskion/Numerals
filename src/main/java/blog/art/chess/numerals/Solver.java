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

import blog.art.chess.numerals.Game.Node;
import blog.art.chess.numerals.Game.Position;
import blog.art.chess.numerals.Moves.Move;
import blog.art.chess.numerals.Moves.NullMove;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class Solver {

  static void goPerft(Position position, int nPlies) {
    long begin = System.currentTimeMillis();
    Optional<List<Move>> pseudoLegalMoves = Engine.isPositionLegal(position);
    if (pseudoLegalMoves.isPresent()) {
      long nNodes = count(new Node(position, pseudoLegalMoves.get()), nPlies, true);
      long end = System.currentTimeMillis();
      System.out.printf("Nodes searched: %d%n", nNodes);
      System.out.printf("info time %d%n", end - begin);
    } else {
      System.out.println("info string Illegal position");
    }
  }

  private static long count(Node node, int nPlies, boolean verbose) {
    if (nPlies == 0) {
      return 1;
    }
    long nNodes = 0;
    for (Move move : node.searchList()) {
      Optional<Node> nodeNext = Engine.makeMove(node.position(), move);
      if (nodeNext.isPresent()) {
        long nChildNodes = count(nodeNext.get(), nPlies - 1, false);
        nNodes += nChildNodes;
        if (verbose) {
          System.out.printf("%s: %d%n", Engine.toUciCode(move), nChildNodes);
        }
      }
    }
    return nNodes;
  }

  private record Variation(int value, List<Move> gameList) {

  }

  static void goMate(Position position, int nMoves) {
    long begin = System.currentTimeMillis();
    Optional<List<Move>> pseudoLegalMoves = Engine.isPositionLegal(position);
    if (pseudoLegalMoves.isPresent()) {
      List<Variation> variations = new ArrayList<>();
      for (Move move : pseudoLegalMoves.get()) {
        Optional<Node> nodeMin = Engine.makeMove(position, move);
        if (nodeMin.isPresent()) {
          Variation variationMin = searchMin(nodeMin.get(), nMoves);
          int distance =
              variationMin.value > 0 ? nMoves - variationMin.value + 1 : Integer.MAX_VALUE;
          List<Move> gameList = new ArrayList<>(variationMin.gameList);
          gameList.addFirst(move);
          variations.add(new Variation(distance, gameList));
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
          for (Move move : principalVariation.gameList) {
            tokens.add(Engine.toUciCode(move));
          }
          System.out.printf("info time %d score mate %d pv %s%n", end - begin,
              principalVariation.value, String.join(" ", tokens));
        } else {
          System.out.printf("info time %d%n", end - begin);
        }
        System.out.printf("bestmove %s%n",
            Engine.toUciCode(principalVariation.gameList.getFirst()));
      } else {
        System.out.printf("info time %d%n", end - begin);
        System.out.printf("bestmove %s%n", Engine.toUciCode(new NullMove()));
      }
    } else {
      System.out.println("info string Illegal position");
    }
  }

  private static Variation searchMax(Node nodeMax, int nMoves) {
    int valueMax = -1;
    List<Move> gameListMax = new ArrayList<>();
    for (Move moveMax : nodeMax.searchList()) {
      Optional<Node> nodeMin = Engine.makeMove(nodeMax.position(), moveMax);
      if (nodeMin.isPresent()) {
        Variation variationMin = searchMin(nodeMin.get(), nMoves);
        if (variationMin.value > valueMax) {
          valueMax = variationMin.value;
          gameListMax = new ArrayList<>(variationMin.gameList);
          gameListMax.addFirst(moveMax);
          if (valueMax == nMoves) {
            break;
          }
        }
      }
    }
    return new Variation(valueMax, gameListMax);
  }

  private static Variation searchMin(Node nodeMin, int nMoves) {
    int valueMin = 0;
    List<Move> gameListMin = new ArrayList<>();
    if (nMoves == 1) {
      for (Move moveMin : nodeMin.searchList()) {
        if (Engine.makeMove(nodeMin.position(), moveMin).isPresent()) {
          valueMin = -1;
          break;
        }
      }
    } else {
      for (Move moveMin : nodeMin.searchList()) {
        Optional<Node> nodeMax = Engine.makeMove(nodeMin.position(), moveMin);
        if (nodeMax.isPresent()) {
          Variation variationMax = searchMax(nodeMax.get(), nMoves - 1);
          if (valueMin == 0 || variationMax.value < valueMin) {
            valueMin = variationMax.value;
            gameListMin = new ArrayList<>(variationMax.gameList);
            gameListMin.addFirst(moveMin);
            if (valueMin == -1) {
              break;
            }
          }
        }
      }
    }
    if (valueMin == 0) {
      valueMin = Engine.makeMove(nodeMin.position(), new NullMove()).isPresent() ? -1 : nMoves;
    }
    return new Variation(valueMin, gameListMin);
  }
}
