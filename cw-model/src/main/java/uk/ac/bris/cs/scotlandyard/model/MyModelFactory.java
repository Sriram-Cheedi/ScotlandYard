package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		// TODO
		Board.GameState gameState = new MyGameStateFactory().build(setup, mrX, detectives);
		ImmutableSet<Model.Observer> observers = ImmutableSet.of();
		return new MyModel(gameState, observers);
	}
	private class MyModel implements Model {
		private Board.GameState gameState;

		private ImmutableSet<Observer> observers;

		public MyModel(Board.GameState gameState, ImmutableSet<Observer> observers) {
			this.gameState = gameState;
			this.observers = observers;
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observers.contains(observer)){
				throw new IllegalArgumentException("observer is already there");
			}
			HashSet<Observer> tempSet = new HashSet<>(observers);
			tempSet.add(observer);
			observers = ImmutableSet.copyOf(tempSet);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer.equals(null)) {
				throw new NullPointerException();
			}
			if (!observers.contains(observer)) {
				throw new IllegalArgumentException("observer is not registered");
			}
			HashSet<Observer> tempSet = new HashSet<>(observers);
			tempSet.remove(observer);
			observers = ImmutableSet.copyOf(tempSet);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}


		@Override
		public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			Observer.Event event;
			if (gameState.getWinner().isEmpty()) {
				event = Observer.Event.MOVE_MADE;
			} else {
				event = Observer.Event.GAME_OVER;
			}

			for (Observer observer : observers) {
				observer.onModelChanged(gameState, event);
			}
		}
	}

}
