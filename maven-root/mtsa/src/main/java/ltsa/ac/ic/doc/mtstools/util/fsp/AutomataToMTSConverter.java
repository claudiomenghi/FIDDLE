package ltsa.ac.ic.doc.mtstools.util.fsp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.util.MTSUtils;
import ltsa.lts.util.collections.MyList;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.impl.MTSImpl;



public class AutomataToMTSConverter {
	
	private static AutomataToMTSConverter instance;
	 
	private String[] indexToAction;
	private MTS<Long,String> mts;
	private TransitionType[] indexToTransitionType;
	private ModelConverterUtils modelConverterUtils;

	private AutomataToMTSConverter() {
		modelConverterUtils = new ModelConverterUtils();
	}

	public static AutomataToMTSConverter getInstance() {
		if (instance == null) {
			instance = new AutomataToMTSConverter();
		}
		return instance;
	}

	public MTS<Long, String> convert(LabelledTransitionSystem automata) {
		// TODO this isn't converting anything about the probabilistic transitions yet. 
		this.mts = new MTSImpl<>(modelConverterUtils.rank(automata.start()));
		
		indexToAction = new String[automata.getAlphabet().length];
		indexToTransitionType = new TransitionType[automata.getAlphabet().length];
		
		this.addActions(automata);
		this.addTransitions(automata);
		
		
		return mts;
	}
	
	/**
	 * 
	 * @param automata
	 */
	private void addActions(LabelledTransitionSystem automata) {
		String[] alphabet = automata.getAlphabet();
		Map<String,Integer> reverseMap = new HashMap<>();

		for(int i = 0; i<alphabet.length; i++) {
			String action = MTSUtils.getAction(alphabet[i]);
		
			if (reverseMap.containsKey(action)) {
				indexToAction[i] = indexToAction[reverseMap.get(action)];
			} else {
				mts.addAction(action);
				reverseMap.put(action,i);
				indexToAction[i] = action;
			}
			if (MTSUtils.isMaybe(alphabet[i])) {
				indexToTransitionType[i] = MTS.TransitionType.MAYBE;
			} else {
				indexToTransitionType[i] = MTS.TransitionType.REQUIRED;
			}
		}
	}


	private void addTransitions(LabelledTransitionSystem automata) {
		Queue<Long> toProcess = new LinkedList<Long>();
		
		toProcess.offer(mts.getInitialState());
		while(!toProcess.isEmpty()) {
			Long actualState = toProcess.poll();
			MyList transitions = automata.getTransitions(modelConverterUtils.unrank(actualState));
			while(!transitions.empty()) {
				Long rank = modelConverterUtils.rank(transitions.getTo());
				if (!mts.getStates().contains(rank)) {
					mts.addState(rank);
					toProcess.offer(rank);
				}
				mts.addTransition(
						actualState,
						indexToAction[transitions.getAction()], //aca podria guardar de donde viene cada action
						rank,
						indexToTransitionType[transitions.getAction()]);
				
				transitions.next();
			}
		}
	}
}
