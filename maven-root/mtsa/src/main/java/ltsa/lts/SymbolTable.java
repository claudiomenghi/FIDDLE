package ltsa.lts;

import java.util.Hashtable;

public class SymbolTable {

	private static Hashtable<String, Integer> keyword;

	//public static void init() {
	static { //static initialiser since the language doesn't change at runtime!
		keyword = new Hashtable<String, Integer>();
		keyword.put("const", new Integer(Symbol.CONSTANT));
		keyword.put("property", new Integer(Symbol.PROPERTY));
		keyword.put("range", new Integer(Symbol.RANGE));
		keyword.put("if", new Integer(Symbol.IF));
		keyword.put("then", new Integer(Symbol.THEN));
		keyword.put("else", new Integer(Symbol.ELSE));
		keyword.put("forall", new Integer(Symbol.FORALL));
		keyword.put("when", new Integer(Symbol.WHEN));
		keyword.put("set", new Integer(Symbol.SET));
		keyword.put("progress", new Integer(Symbol.PROGRESS));
		keyword.put("menu", new Integer(Symbol.MENU));
		keyword.put("animation", new Integer(Symbol.ANIMATION));
		keyword.put("actions", new Integer(Symbol.ACTIONS));
		keyword.put("controls", new Integer(Symbol.CONTROLS));
		keyword.put("deterministic", new Integer(Symbol.DETERMINISTIC));
		keyword.put("minimal", new Integer(Symbol.MINIMAL));
		keyword.put("compose", new Integer(Symbol.COMPOSE));
		keyword.put("target", new Integer(Symbol.TARGET));
		keyword.put("import", new Integer(Symbol.IMPORT));
		keyword.put("assert", new Integer(Symbol.ASSERT));
		keyword.put("fluent", new Integer(Symbol.PREDICATE));
		keyword.put("exists", new Integer(Symbol.EXISTS));
		keyword.put("rigid", new Integer(Symbol.RIGID));
		keyword.put("fluent", new Integer(Symbol.PREDICATE));
		keyword.put("constraint", new Integer(Symbol.CONSTRAINT));
		keyword.put("ltl_property", new Integer(Symbol.LTLPROPERTY));
		keyword.put("initially", new Integer(Symbol.INIT));
		keyword.put("optimistic", new Integer(Symbol.OPTIMISTIC));
		keyword.put("pessimistic", new Integer(Symbol.PESSIMISTIC));
		keyword.put("clousure", new Integer(Symbol.CLOUSURE));
		keyword.put("abstract", new Integer(Symbol.ABSTRACT));
		keyword.put("restricts", new Integer(Symbol.RESTRICTS));
		keyword.put("instances", new Integer(Symbol.INSTANCES));
		keyword.put("prechart", new Integer(Symbol.PRECHART));
		keyword.put("mainchart", new Integer(Symbol.MAINCHART));
		keyword.put("eTS", new Integer(Symbol.E_TRIGGERED_SCENARIO));
		keyword.put("uTS", new Integer(Symbol.U_TRIGGERED_SCENARIO));

		keyword.put("distribution", new Integer(Symbol.DISTRIBUTION));
		keyword.put("systemModel", new Integer(Symbol.SYSTEM_MODEL));
		keyword.put("outputFileName", new Integer(Symbol.OUTPUT_FILE_NAME));
		keyword.put("distributedAlphabets", new Integer(Symbol.DISTRIBUTED_ALPHABETS));
		keyword.put("def", new Integer(Symbol.DEF));
		keyword.put("foreach", new Integer(Symbol.FOREACH));

		keyword.put ("exploration", new Integer(Symbol.EXPLORATION));
	    keyword.put ("environment", new Integer(Symbol.EXPLORATION_ENVIRONMENT));
	    keyword.put ("model", new Integer(Symbol.EXPLORATION_MODEL));
	    keyword.put ("goal", new Integer(Symbol.EXPLORATION_GOAL));
	    keyword.put ("environment_actions", new Integer(Symbol.EXPLORATION_ENVIRONMENT_ACTIONS));
		
		keyword.put("component", new Integer(Symbol.COMPONENT));
		keyword.put("condition", new Integer(Symbol.CONDITION));
		keyword.put("controller", new Integer(Symbol.CONTROLLER));
		keyword.put("gr", new Integer(Symbol.CONTROLLER));
		keyword.put("syncGR", new Integer(Symbol.SYNC_CONTROLLER));
		keyword.put("starenv", new Integer(Symbol.STARENV));
		keyword.put("plant", new Integer(Symbol.PLANT));
		keyword.put("controllerSpec", new Integer(Symbol.GOAL));
		keyword.put("safety", new Integer(Symbol.SAFETY));
		keyword.put("assumption", new Integer(Symbol.ASSUME));
		keyword.put("failure", new Integer(Symbol.FAULT));
		keyword.put("liveness", new Integer(Symbol.GUARANTEE));
		keyword.put("controllable", new Integer(Symbol.CONTROLLABLE));
		keyword.put("checkCompatibility", new Integer(Symbol.CHECK_COMPATIBILITY));
		keyword.put("permissive", new Integer(Symbol.PERMISSIVE));
		keyword.put("controlled_det", new Integer(Symbol.CONTROLLED_DET));
		keyword.put("nonblocking", new Integer(Symbol.CONTROLLER_NB));
		keyword.put("lazyness", new Integer(Symbol.CONTROLLER_LAZYNESS));
		keyword.put("non_transient", new Integer(Symbol.NON_TRANSIENT));
        keyword.put ("reachability", new Integer(Symbol.REACHABILITY));
		keyword.put("activityFluents", new Integer(Symbol.ACTIVITY_FLUENTS));
        keyword.put ("test_latency", new Integer(Symbol.TEST_LATENCY));
		keyword.put("exceptionHandling", new Integer(Symbol.EXCEPTION_HANDLING));
		keyword.put("concurrencyFluents", new Integer(Symbol.CONCURRENCY_FLUENTS));
		keyword.put("controlstack", new Integer(Symbol.CONTROL_STACK));
		keyword.put("tier", new Integer(Symbol.CONTROL_TIER));
		keyword.put("mdp", new Integer(Symbol.MDP));
		keyword.put("probabilistic", new Integer(Symbol.PROBABILISTIC));

		//Updating controller problem
		keyword.put("updatingController", new Integer(Symbol.UPDATING_CONTROLLER));
		keyword.put("oldController", new Integer(Symbol.OLD_CONTROLLER));
		keyword.put("oldEnvironment", new Integer(Symbol.OLD_ENVIRONMENT));
		//keyword.put("hatEnvironment", new Integer(Symbol.HAT_ENVIRONMENT));
		keyword.put("newEnvironment", new Integer(Symbol.NEW_ENVIRONMENT));
		keyword.put("oldGoal", new Integer(Symbol.OLD_GOAL));
		keyword.put("newGoal", new Integer(Symbol.NEW_GOAL));
		keyword.put("transition", new Integer(Symbol.TRANSITION));
		//keyword.put("updateFluents", new Integer(Symbol.UPDATE_FLUENTS));
		keyword.put("oldPropositions", new Integer(Symbol.OLD_PROPOSITIONS));
		keyword.put("newPropositions", new Integer(Symbol.NEW_PROPOSITIONS));
		keyword.put("debug", new Integer(Symbol.UPDATE_DEBUG));
		keyword.put("checkTrace", new Integer(Symbol.UPDATE_CHECK_TRACE));
		keyword.put("graphUpdate", Symbol.GRAPH_UPDATE);
		keyword.put("initialState", Symbol.GRAPH_INITIAL_STATE);
		keyword.put("transitions", Symbol.GRAPH_TRANSITIONS);
	}

	public static Integer get(String s) {
		return keyword.get(s);
	}
}