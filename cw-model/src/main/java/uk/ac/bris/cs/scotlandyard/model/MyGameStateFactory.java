package uk.ac.bris.cs.scotlandyard.model;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nonnull;
import java.util.*;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.atlassian.fugue.Iterables.size;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);

	}
	private class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableList<LogEntry> log;
		private List<Player> detectives;
		private ImmutableSet<Piece> remaining;
		private Player mrX;
		private Player currPlayer;
		private final ImmutableList<Player> everyone;
		private int currRound;
		private final int maxRounds;
		private ImmutableSet<Piece> winner;
		private ImmutableSet<Move> moves;

		public MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.moves = getAvailableMoves();
			this.currRound = log.size();
			this.maxRounds = setup.moves.size();
			List<Player> allPlayers = new ArrayList<>(detectives);
			allPlayers.add(mrX);
			this.everyone = ImmutableList.copyOf(allPlayers);

			for (Player p : everyone)
				if (p.piece() == remaining.iterator().next()) {
					currPlayer = p;
				}
			this.winner = getWinner();

			if (detectives == null) throw new NullPointerException("detective are null");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("graph is empty");
			if (mrX == null) throw new NullPointerException("MrX is null");
			if (!mrX.isMrX()) throw new IllegalArgumentException("No MrX");
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves are empty");
			for (Player detective : detectives) {
				if(!detective.isDetective()) throw new IllegalArgumentException("No detectice");
				if (detective.has(Ticket.SECRET)) throw new IllegalArgumentException("Doesn't have");
				if (detective.has(Ticket.DOUBLE)) throw new IllegalArgumentException("Doesn't have");
			}
			for (var detect1 : detectives) {
				for (Player detect2 : detectives) {
					if (detect1.equals(detect2) && (detect1 != detect2)) {
						throw new IllegalArgumentException("Duplicate pieces");
					}
				}
			}
			for (Player detect : detectives) {
				for (Player detective2 : detectives) {
					if (detect.location() == detective2.location() && detect != detective2) {
						throw new IllegalArgumentException("Detectives are in the same Location");
					}
				}
			}


		}



		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player gamer : detectives) {
				if (gamer.piece() == detective) {
					return Optional.of(gamer.location());
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.isMrX()) {
				TicketBoard ticketBoard = new TicketBoard() {
					@Override
					public int getCount(@Nonnull Ticket ticket) {
						return mrX.tickets().getOrDefault(ticket, 0);
					}
				};
				return Optional.of(ticketBoard);
			} else {
				for (var gamer : detectives) {
					if (piece == gamer.piece()) {
						TicketBoard ticketBoard = new TicketBoard() {
							@Override
							public int getCount(@Nonnull Ticket ticket) {
								return gamer.tickets().getOrDefault(ticket, 0);
							}

						};
						return Optional.of(ticketBoard);
					}

				}
				return Optional.empty();
			}
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			ArrayList<Piece> winners = new ArrayList<>();
			boolean detectivesWin = false;
			for (Player detective : detectives) {
				if (detective.location() == mrX.location()) {
					detectivesWin = true;
					break;
				}
			}

			if (detectivesWin || ((moves != null) && moves.equals(ImmutableSet.of()) && remaining.contains(mrX.piece()))) {
				for (Player detective : detectives) {
					winners.add(detective.piece());
				}
			} else {
				boolean mrXWins = (currRound == maxRounds && remaining.contains(mrX.piece()));
				if (!mrXWins) { // Check if detectives are stuck
					for (Player detective : detectives) {
						if (detective.has(Ticket.TAXI) || detective.has(Ticket.UNDERGROUND) || detective.has(Ticket.BUS)) {
							mrXWins = false;
							break;
						}
						mrXWins = true;
					}
				}

				if (mrXWins) {
					winners.add(mrX.piece());
				}
			}

			return ImmutableSet.copyOf(winners);
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			if (!winner.isEmpty()) {
				return ImmutableSet.of();
			}
			Set<Move> moves = new HashSet<>();

			if (remaining.contains(Piece.MrX.MRX)) {

				moves.addAll(movesForMrX(setup, detectives, mrX));
			} else {
				moves.addAll(movesForDet(setup, detectives, remaining));
			}
			return ImmutableSet.copyOf(moves);

		}

		private Set<Move> movesForMrX(GameSetup setup, List<Player> detectives, Player mrX) {
			Set<Move> moves = new HashSet<>();
			int location = mrX.location();
			moves.addAll(makeSingleMoves(setup, detectives, mrX, location));
			moves.addAll(makeDoubleMoves(setup, detectives, mrX, location));
			return moves;
		}

		private Set<Move> movesForDet(GameSetup setup, List<Player> detectives, Set<Piece> remaining) {
			Set<Move> moves = new HashSet<>();
			for (Player detective : detectives) {
				if (remaining.contains(detective.piece())) {
					moves.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
				}
			}
			return moves;
		}


		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> pieces = new HashSet<>();
			pieces.add(mrX.piece());
			for (Player detective : detectives) {
				pieces.add(detective.piece());
			}
			return ImmutableSet.copyOf(pieces);
		}
		@Nonnull @Override public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			Piece p = move.commencedBy();
			for(Player player : everyone){
				if(player.piece() == p){
					currPlayer = player;
				}
			}

			int destination1 = move.accept(new Visitor<>() {
				@Override
				public Integer visit(SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(DoubleMove move) {
					return move.destination2;
				}
			});
			int destination2 = move.accept(new Visitor<>() {
				@Override
				public Integer visit(SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(DoubleMove move) {
					return move.destination2;
				}
			});


			currPlayer = currPlayer.at(destination2);

			List<Piece> Remaining = new ArrayList<>(remaining);


			if (p.isMrX()) {
				for (Player detective : detectives) {
					Remaining.add(detective.piece());
				}
			}

			for (Ticket ticket : move.tickets()) {
				currPlayer = currPlayer.use(ticket);
				if(currPlayer.isDetective()){
					mrX = mrX.give(ticket);
				}
			}


			Remaining.remove(p);

			boolean noMovesLeft = true;
			for (Move m : moves) {
				if (m.commencedBy() != p) {
					noMovesLeft = false;
					break;
				}
			}
			if (Remaining.isEmpty() || (!p.isMrX() && noMovesLeft)) {
				Remaining.clear();
				Remaining.add(MrX.MRX);
			}


			if(p.isMrX()){

				ArrayList<LogEntry> newLog = LogUpdt(move, destination1, destination2);
				return new MyGameState(setup, ImmutableSet.copyOf(Remaining), ImmutableList.copyOf(newLog), currPlayer, detectives);
			}

			else{
				List<Player> updatedPlayers = new ArrayList<>();
				for (Player detective : detectives) {
					if (detective.piece() == p) {
						updatedPlayers.add(currPlayer);
					} else {
						updatedPlayers.add(detective);
					}
				}

				return new MyGameState(setup, ImmutableSet.copyOf(Remaining), log, mrX, ImmutableList.copyOf(updatedPlayers));
			}
		}

		private ArrayList<LogEntry> LogUpdt(Move move, int destination1, int destination2) {
			ArrayList<LogEntry> newLog= new ArrayList<>(log);

			if(size(move.tickets()) > 1){
				if(setup.moves.get(currRound))
					newLog.add(LogEntry.reveal(move.tickets().iterator().next(), destination1));
				else newLog.add(LogEntry.hidden(move.tickets().iterator().next()));
				currRound++;
			}
			if (setup.moves.get(currRound))
				newLog.add(LogEntry.reveal(move.tickets().iterator().next(), destination2));
			else newLog.add(LogEntry.hidden(move.tickets().iterator().next()));

			return newLog;
		}
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

			source = player.location();
			HashSet<SingleMove> makeSingleMoves = new HashSet<>();
			for (int destination : setup.graph.adjacentNodes(source)) {
				boolean match = false;
				for (var detectivelocation : detectives) {
					if (destination == detectivelocation.location()) {
						match = true;
					}
				}
				if (match) {
					continue;

				}

				for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) {
						makeSingleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
					if (player.has(Ticket.SECRET)) {
						makeSingleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}
			return ImmutableSet.copyOf(makeSingleMoves);
		}

		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int location) {
			int source = mrX.location();
			Set<DoubleMove> doubleMoves = new HashSet<>();


			if (!mrX.has(Ticket.DOUBLE) || setup.moves.size() <= 1) {
				return ImmutableSet.of();
			}

			Set<Integer> occupiedLocations = new HashSet<>();
			for (Player detective : detectives) {
				occupiedLocations.add(detective.location());
			}


			for (int firstDestination : setup.graph.adjacentNodes(source)) {
				if (occupiedLocations.contains(firstDestination)) continue;

				for (Transport transport1 : setup.graph.edgeValueOrDefault(source, firstDestination, ImmutableSet.of())) {
					Ticket ticket1 = transport1.requiredTicket();
					if (!mrX.has(ticket1)) continue;

					for (int secondDestination : setup.graph.adjacentNodes(firstDestination)) {
						if (occupiedLocations.contains(secondDestination)) continue;

						for (Transport transport2 : setup.graph.edgeValueOrDefault(firstDestination, secondDestination, ImmutableSet.of())) {
							Ticket ticket2 = transport2.requiredTicket();
							if (!mrX.has(ticket2)) continue;
							validDoubleMoves(doubleMoves, mrX, source, ticket1, firstDestination, ticket2, secondDestination);
						}
					}
				}
			}

			return ImmutableSet.copyOf(doubleMoves);
		}

		private static void validDoubleMoves(Set<DoubleMove> moves, Player mrX, int source, Ticket ticket1, int firstdestination, Ticket ticket2, int seconddestination) {
			Piece playerPiece = mrX.piece();

			if (ticket1 != ticket2) {
				moves.add(new DoubleMove(playerPiece, source, ticket1, firstdestination, ticket2, seconddestination));
			}

			if (ticket1 == ticket2 && mrX.hasAtLeast(ticket1, 2)) {
				moves.add(new DoubleMove(playerPiece, source, ticket1, firstdestination, ticket2, seconddestination));
			}

			if (mrX.has(Ticket.SECRET)) {
				moves.add(new DoubleMove(playerPiece, source, Ticket.SECRET, firstdestination, ticket2, seconddestination));
				moves.add(new DoubleMove(playerPiece, source, ticket1, firstdestination, Ticket.SECRET, seconddestination));
			}

			if (mrX.hasAtLeast(Ticket.SECRET, 2)) {
				moves.add(new DoubleMove(playerPiece, source, Ticket.SECRET, firstdestination, Ticket.SECRET, seconddestination));
			}
		}
	}
}



