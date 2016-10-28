package ltsa.ac.ic.doc.mtstools.util.fsp;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.impl.MDP;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSImpl;
import MTSTools.ac.ic.doc.mtstools.model.impl.ProbabilisticTransition;

/**
 * Very closely based on AutomataToMTSConverter Daniel Sykes 2014
 **/

public class AutomataToMDPConverter {
	private static AutomataToMDPConverter instance;

	private String[] indexToAction;
	private MDP mdp;
	private ModelConverterUtils modelConverterUtils;

	private AutomataToMDPConverter() {
		modelConverterUtils = new ModelConverterUtils();
	}

	public static AutomataToMDPConverter getInstance() {
		if (instance == null) {
			instance = new AutomataToMDPConverter();
		}
		return instance;
	}

	public MTS<Long, String> convert(MDP mdp) {
		MTS<Long, String> res = new MTSImpl<Long, String>(mdp.getInitialState());
		for (String a : mdp.getActions())
			res.addAction(a);
		for (long s : mdp.getStates())
			res.addState(s);
		for (long s : mdp.getStates()) {
			for (ProbabilisticTransition t : mdp.getTransitionsFrom(s))
				res.addTransition(s, t.getAction(), t.getTo(),
						TransitionType.REQUIRED);
		}
		return res;
	}

	public MDP convert(LabelledTransitionSystem automata) {
		this.mdp = new MDP(modelConverterUtils.rank(automata.start()));

		indexToAction = new String[automata.getAlphabet().length];

		this.addActions(automata);
		this.addTransitions(automata);

		// printEverything(automata);

		return mdp;
	}

	private void addActions(LabelledTransitionSystem automata) {
		String[] alphabet = automata.getAlphabet();
		Map<String, Integer> reverseMap = new HashMap<String, Integer>();

		for (int i = 0; i < alphabet.length; i++) {
			String action = alphabet[i];

			if (reverseMap.containsKey(action)) {
				indexToAction[i] = indexToAction[reverseMap.get(action)];
			} else {
				mdp.addAction(action);
				reverseMap.put(action, i);
				indexToAction[i] = action;
			}
		}
	}

	private void addTransitions(LabelledTransitionSystem automaton) {
		Queue<Long> stateQueue = new LinkedList<Long>();

		stateQueue.add(mdp.getInitialState());
		while (!stateQueue.isEmpty()) {
			long actualState = stateQueue.remove();
			System.out.println("Processing " + actualState);
			if (automaton.isAccepting((int) actualState))
				mdp.addStateLabel(actualState, "accepting");
			if (automaton.getStates()[(int) actualState] != null)
				for (Object o : Collections
						.list(automaton.getStates()[(int) actualState]
								.elements())) {
					LTSTransitionList ev = (LTSTransitionList) o;
					long next = ev.getNext();
					String action = indexToAction[ev.getEvent()];
					if (!mdp.getStates().contains(next)) {
						mdp.addState(next);
						stateQueue.add(next);
					}
					System.out.println("adding " + actualState + " --" + action
							+ "--> " + next);
					mdp.addTransition(actualState, action, next);

					if (LTSTransitionList.hasNonDet(ev)) {
						System.out.println(indexToAction[ev.getEvent()] + "-->"
								+ ev.getNext() + " has nondet (mdpconverter)");
						for (Object o2 : Collections.list(ev.elements())) {
							if (o2 != o) {
								LTSTransitionList ev2 = (LTSTransitionList) o2;
								long next2 = ev2.getNext();
								if (!mdp.getStates().contains(next2)) {
									mdp.addState(next2);
									stateQueue.add(next2);
								}
								System.out.println("adding " + actualState
										+ " --" + indexToAction[ev2.getEvent()]
										+ "--> " + next2);
								mdp.addTransition(actualState,
										indexToAction[ev2.getEvent()], next2); // but
																				// if
																				// it's
																				// nondet+prob?
							}
						}
					} else
						System.out.println(indexToAction[ev.getEvent()] + "-->"
								+ ev.getNext()
								+ " has NO nondet (mdpconverter)");
				}
		}
	}
}
