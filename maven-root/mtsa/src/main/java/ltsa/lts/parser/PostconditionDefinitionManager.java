package ltsa.lts.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.FormulaSyntax;
import ltsa.lts.ltl.PostconditionDefinition;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.actions.LabelSet;

import com.google.common.base.Preconditions;

public class PostconditionDefinitionManager {

	/**
	 * maps the name of the postcondition to the corresponding postcondition
	 */
	private Map<String, PostconditionDefinition> postconditions;

	/**
	 * maps each couple <process,<box, post>> to the corresponding postcondition
	 */
	private Map<String, Map<String, String>> mapBoxPostconditions;

	/**
	 * maps each process to the corresponding box
	 */
	private Map<String, Set<String>> mapProcessBoxes;

	/**
	 * maps each postcondition to the corresponding process
	 */
	private Map<String, String> mapPostConditionMachine;

	public static boolean addAsterisk = true;

	public PostconditionDefinitionManager() {
		reset();
	}

	public void reset() {
		this.postconditions = new HashMap<>();
		this.mapBoxPostconditions = new HashMap<>();
		this.mapProcessBoxes = new HashMap<>();
		this.mapPostConditionMachine = new HashMap<>();
	}

	public Map<String, String> getMapPostConditionMachine() {
		return mapPostConditionMachine;
	}

	public Map<String, PostconditionDefinition> getpostConditions() {
		return this.postconditions;
	}

	public Map<String, Set<String>> getMapProcessBox() {
		return this.mapProcessBoxes;
	}

	public void put(Symbol postConditionName, FormulaSyntax f, LabelSet ls,
			Hashtable<String, Value> ip, Vector<String> p, String box,
			String process) {
		if (postconditions == null) {
			postconditions = new HashMap<>();
		}
		if (!mapProcessBoxes.containsKey(process)) {
			mapProcessBoxes.put(process, new HashSet<>());
		}
		mapProcessBoxes.get(process).add(box);

		if (postconditions.put(postConditionName.toString(),
				new PostconditionDefinition(postConditionName.getValue(), postConditionName, f, ls, ip, p,
						box)) != null) {
			Diagnostics.fatal("duplicate preconditions definition: "
					+ postConditionName, postConditionName);
		}

		if (this.mapBoxPostconditions.containsKey(process)
				&& this.mapBoxPostconditions.get(process).containsKey(box)) {
			Diagnostics
					.fatal("A post-condition is already present for the box: "
							+ box + " of the process: " + process);

		}
		this.mapPostConditionMachine.put(postConditionName.toString(), process);
		if (this.mapBoxPostconditions.containsKey(process)) {
			this.mapBoxPostconditions.get(process).put(box,
					postConditionName.toString());
		} else {
			Map<String, String> boxPre = new HashMap<>();
			boxPre.put(box, postConditionName.toString());
			this.mapBoxPostconditions.put(process, boxPre);
		}
	}

	public Map<String, Map<String, String>> getMapBoxPostcondition() {
		return mapBoxPostconditions;
	}

	/**
	 * returns a state machine describing the violating behaviors
	 * 
	 * @param output
	 *            the output used to print messages
	 * @param asserted
	 *            the string representing the precondition to be considered
	 * @return a state machine describing the violating behaviors
	 * @throws IllegalArgumentException
	 *             if the string representing the precondition is not a valid
	 *             string
	 */
	public LabelledTransitionSystem compile(LTSOutput output, String asserted,
			List<String> alphabetCharacters, String name) {
		Preconditions
				.checkArgument(
						this.postconditions.containsKey(asserted),
						"The postcondition "
								+ asserted
								+ " is not contained into the set of the preconditions");
		PostconditionDefinition post = this.postconditions.get(asserted);

		output.outln("FORMULA: " + post.getFac().getFormula() + " considered");

		return new LTLf2LTS().toLTS(post.getFac().getFormula(), output,
				alphabetCharacters, name);
	}

	public CompositeState compile(LTSOutput output,
			List<String> alphabetCharacters, String name) {
		Preconditions
				.checkArgument(
						postconditions.containsKey(name),
						"The precondition "
								+ name
								+ " is not contained into the set of the preconditions");
		PostconditionDefinition post = postconditions.get(name);

		output.outln("FORMULA: " + post.getFac().getFormula() + " considered");

		return new LTLf2LTS().toCompositeState(post.getFac().getFormula(),
				output, alphabetCharacters, name);
	}


}