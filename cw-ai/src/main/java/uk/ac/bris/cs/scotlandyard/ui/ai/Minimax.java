package uk.ac.bris.cs.scotlandyard.ui.ai;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
public class Minimax implements Ai {


    @Nonnull
    @Override
    public String name() {
        return "MiniMaxMrX";
    }

    private Board makeMove(Board board, Move move) {
        return ((Board.GameState) board).advance(move);
    }

    private boolean isMrXRemaining(Board board) {
        for (Move move : board.getAvailableMoves()) {
            if (move.commencedBy().isMrX()) {
                return true;
            }
        }
        return false;
    }

    private List<Board> detectiveMoves(Board board) {
        if (isMrXRemaining(board)) {
            return List.of(board);
        }
        List<Board> boards = new ArrayList<>();
        for (Move move : board.getAvailableMoves()) {
            boards.addAll(detectiveMoves(makeMove(board, move)));
        }
        return boards;
    }

    private int minimax(Move moves, Board board, int depth, boolean isMrX, int mrXLocation, int alpha, int beta) {
        if (depth == 0 || board.getWinner().isEmpty()) {
            return score(moves, board);
        }

        if (isMrX) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : board.getAvailableMoves()) {
                Board newState = makeMove(board, move);
                int eval = minimax(moves, newState, depth - 1, false, mrXLocation, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Board subBoard : detectiveMoves(board)) {
                int eval = minimax(moves, subBoard, depth - 1, true, mrXLocation, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
    }


    private int score(Move moves, Board board) {
        int mrXLocation = moves.accept(new Move.Visitor<Integer>() {
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }

            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.destination2;
            }
        });

        int distanceFromDetectives = Distance(board, moves);
        int connectivityScore = freedom(board, mrXLocation);
        int ticketPenalty = TicketScore(moves, distanceFromDetectives);

        return connectivityScore + distanceFromDetectives - ticketPenalty;
    }

    private int freedom(Board board, int location) {
        return board.getSetup().graph.adjacentNodes(location).size();
    }


    private int TicketScore(Move move, int distanceFromDetectives) {
        int score = 0;
        for (ScotlandYard.Ticket ticket : move.tickets()) {
            switch (ticket) {
                case TAXI:
                    score += 1;
                    break;
                case BUS:
                    score += 2;
                    break;
                case UNDERGROUND:
                    score += 3;
                    break;
                case SECRET:
                    score += (distanceFromDetectives < 3) ? 10 : 5; // Higher penalty if close to detectives
                    break;
                case DOUBLE:
                    score += 10;
                    break;
            }
        }
        return score;
    }

    private int Distance(Board board, Move move) {
        return board.getPlayers().stream()
                .filter(Piece::isDetective)
                .map(player -> board.getDetectiveLocation((Piece.Detective) player))
                .filter(Optional::isPresent)
                .mapToInt(detLoc -> move.accept(new Move.Visitor<Integer>() {
                    @Override
                    public Integer visit(Move.SingleMove m) {
                        return Penalty(board, m.destination, detLoc.get());
                    }

                    @Override
                    public Integer visit(Move.DoubleMove m) {
                        int penalty = Penalty(board, m.destination2, detLoc.get());
                        return m.destination2 == m.source() ? penalty - 10 : penalty;
                    }
                })).sum();
    }

    private int Penalty(Board board, int destination, int detectiveLocation) {
        int distance = Dijkstra.findDistance(board, destination, detectiveLocation);
        if (distance == 1) {
            return -200;
        }
        return distance;
    }


    private Move.Visitor<Integer> visitor = new Move.Visitor<>() {
        @Override
        public Integer visit(Move.SingleMove move) {
            return move.destination;
        }

        @Override
        public Integer visit(Move.DoubleMove move) {
            return move.destination2;
        }
    };

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        int depth = 2;
        int maxEval = Integer.MIN_VALUE;
        Move bestMove = null;
        for (Move move : board.getAvailableMoves()) {
            Board newState = makeMove(board, move);
            int eval = minimax(move, newState, depth - 1, false, move.accept(visitor), Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (eval > maxEval) {
                maxEval = eval;
                bestMove = move;
            }
        }
        return bestMove; // Depth is set to 2, adjust as needed.
    }
}