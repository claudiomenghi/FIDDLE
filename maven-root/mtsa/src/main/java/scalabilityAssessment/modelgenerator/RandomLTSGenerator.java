package scalabilityAssessment.modelgenerator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;

/**
 * creates a Random LTS. The LTS is connected, i.e., all the states are
 * reachable from the initial state.
 * 
 * @author Claudio
 *
 */
public class RandomLTSGenerator {

	private final int numberOfStates;
	private final int numberOfEvents;
	private final int transitionPerstates;

	public RandomLTSGenerator(int numberOfStates, int numberOfEvents,
			int transitionsPerStates) {
		this.numberOfStates = numberOfStates;
		this.numberOfEvents = numberOfEvents;
		this.transitionPerstates = transitionsPerStates;
		System.out.println("Transition per state "+transitionsPerStates);
	}

	public LabelledTransitionSystem getRandomLTS(String name) {

		List<String> events = this.createEvents();
		LabelledTransitionSystem lts = new LabelledTransitionSystem(name);

		String[] alphabet = new String[events.size()];
		for (int i = 0; i < events.size(); i++) {
			alphabet[i] = Integer.toString(i);
		}
		// sets the alphabet of the LTS
		lts.setAlphabet(alphabet);

		// creates the state of the LTS
		lts.setStates(new LTSTransitionList[this.numberOfStates]);

		boolean[] toBeVisitedArray = new boolean[this.numberOfStates];
		boolean[] visited = new boolean[this.numberOfStates];
		// creates the transitions of the LTS
		Deque<Integer> toBeVisited = new ArrayDeque<>(this.numberOfStates);

		toBeVisited.add(0);
		toBeVisitedArray[0] = true;
		Random random = new Random();
		int toBevisitedSize = 1;
		while (toBevisitedSize > 0) {

			// for (int currentState = 0; currentState <
			// this.size.getNumberOfStates(); currentState++) {
			int currentState = toBeVisited.peek();
			for (int i = 0; i < this.transitionPerstates; i++) {
				// picks a random destination for the transition
				int nextState = random.nextInt(this.numberOfStates);
				int event = random.nextInt(this.numberOfEvents);

				lts.addTransition(currentState, event, nextState);
				if (!visited[nextState] && !toBeVisitedArray[nextState]) {
					toBeVisited.push(nextState);
					toBevisitedSize++;
					toBeVisitedArray[nextState]=true;
				}
			}
			toBeVisited.remove(currentState);
			visited[currentState] = true;
			toBeVisitedArray[currentState]=false;
			toBevisitedSize--;
			// toBeVisitedArray[currentState] = false;
		}
		// }
		return lts;
	}

	private List<String> createEvents() {
		List<String> events = new ArrayList<>();
		for (int i = 0; i < this.numberOfEvents; i++) {
			events.add(Integer.toString(i));
		}
		return events;
	}
}
