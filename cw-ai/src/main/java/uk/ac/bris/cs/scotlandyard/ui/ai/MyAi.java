package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	private double MRXscore(Move move, Board board) {
		Integer mrXLocation = move.accept(new Move.Visitor<Integer>() {
			@Override
			public Integer visit(Move.SingleMove move) {
				return move.destination;
			}

			@Override
			public Integer visit(Move.DoubleMove move) {
				return move.destination2;
			}
		});

		double distanceScore = Distance(board, move);
		double rate = MoveRate(distanceScore);

		double TotalScore = freedom(board, mrXLocation) + rate - TicketScore(move);
		return TotalScore;
	}

	private double MoveRate(double distanceScore) {
		if (distanceScore < 0) {
			return 2 * distanceScore;
		}
		return Math.pow(1.05, distanceScore);
	}

	private double freedom(Board board, int location) {
		return 1.5 * board.getSetup().graph.adjacentNodes(location).stream()
				.filter(adjacent -> board.getPlayers().stream()
						.filter(Piece::isDetective)
						.map(player -> board.getDetectiveLocation((Piece.Detective) player))
						.filter(Optional::isPresent)
						.noneMatch(detLoc -> Dijkstra.findDistance(board, adjacent, detLoc.get()) <= 1))
				.count();
	}


	@Nonnull
	@Override
	public String name() {
		return "Sritha";
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

	private Integer TicketScore(Move move) {
		int ticketScore = 0;

		for (ScotlandYard.Ticket ticket : move.tickets()) {
			switch (ticket) {
				case TAXI:
					ticketScore += 1;
					break;
				case BUS:
					ticketScore += 2;
					break;
				case UNDERGROUND:
					ticketScore += 3;
					break;
				case SECRET:
					ticketScore += 4;
					break;
				case DOUBLE:
					ticketScore += 10;
					break;
			}
		}
		return ticketScore;
	}


	@Nonnull
	@Override
	public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		return board.getAvailableMoves().asList().stream()
				.max(Comparator.comparingDouble(move -> MRXscore(move, board)))
				.orElseThrow(() -> new IllegalStateException("No available moves found"));
	}
}
