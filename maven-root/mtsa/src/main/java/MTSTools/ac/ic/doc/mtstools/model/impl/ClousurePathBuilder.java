package MTSTools.ac.ic.doc.mtstools.model.impl;

import java.util.*;

import MTSTools.ac.ic.doc.commons.relations.BinaryRelation;
import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;


public class ClousurePathBuilder {

	private static ClousurePathBuilder singleton = new ClousurePathBuilder();

	static public ClousurePathBuilder getInstance() {
		return singleton;
	}
	
	private ClousurePathBuilder(){};
	
	public <S, A> Iterator<List<Pair<A, S>>> getPathsIterator(final MTS<S, A> mts, final S state, final A label,
			final TransitionType type, final Set<A> silentActions) {

		return new ClousurePathIterator<S, A>(mts, state, label, type, silentActions);
	}

	private static class ClousurePathIterator<S, A> implements Iterator<List<Pair<A, S>>> {

	
		private final Pair<A,S> firstPair;

		private final A label;
		private final MTS<S, A> mts;
		private final Set<A> silentActions;
		private final TransitionType type;
		private Map<QueueType, Queue<S>> unprocessed;
		private Map<QueueType, Set<S>> visited;
		
		public ClousurePathIterator(MTS<S, A> mts, S state, A label, TransitionType type, Set<A> silentActions) {

			this.mts = mts;
			this.label = label;
			this.type = type;
			this.silentActions = silentActions;
			this.visited = new EnumMap<QueueType, Set<S>>(QueueType.class);
			this.unprocessed = new EnumMap<QueueType, Queue<S>>(QueueType.class);
			this.firstPair = Pair.create(null,state);
			
			for (QueueType t : QueueType.values()) {
				this.visited.put(t, new HashSet<S>());
				this.unprocessed.put(t, new LinkedList<S>());
			}
			this.enqueueToVisit(state,QueueType.PREACTION);

			this.processPreVisibleTransition();
		}

		public boolean hasNext() {
			return !this.unprocessed.get(QueueType.POSTACTION).isEmpty();

		}

		public List<Pair<A, S>> next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException();
			}
			S finalState = this.unprocessed.get(QueueType.POSTACTION).poll();
			
			this.clousureSilentActions(finalState, this.type, QueueType.POSTACTION);

			this.processPreVisibleTransition();
			
			Pair<A,S> finalPair = Pair.create(label, finalState);
			List<Pair<A, S>> result = new LinkedList<Pair<A,S>>();
			result.add(this.firstPair);
			result.add(finalPair);
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void clousureSilentActions(S state, MTS.TransitionType transitionType, QueueType queueType) {
			BinaryRelation<A, S> transitions = mts.getTransitions(state, transitionType);
			for (A silentAction : silentActions) {
				this.enqueueToVisit(transitions.getImage(silentAction), queueType);
			}
		}
		
		private void enqueueToVisit(Collection<S> states, QueueType queueType) {
			for (S toState : states) {
				if (!this.visited.get(queueType).contains(toState)) {
					this.visited.get(queueType).add(toState);
					this.unprocessed.get(queueType).add(toState);
				}
			}
		}
		
		private void enqueueToVisit(S state, QueueType queueType) {
			this.enqueueToVisit(Collections.singleton(state),queueType);
		}

		private void processPreVisibleTransition() {

			while (		this.unprocessed.get(QueueType.POSTACTION).isEmpty()
					&& !this.unprocessed.get(QueueType.PREACTION).isEmpty()	) {
				
				S state = this.unprocessed.get(QueueType.PREACTION).poll();

				if (type!=TransitionType.MAYBE && silentActions.contains(label)) {
					this.enqueueToVisit(state,QueueType.POSTACTION);				
				}
				
				this.clousureSilentActions(state, this.type, QueueType.PREACTION);
				this.enqueueToVisit(mts.getTransitions(state,this.type).getImage(this.label), QueueType.POSTACTION);				
			}
		}

		private enum QueueType {
			POSTACTION, PREACTION
		}

	}
}
