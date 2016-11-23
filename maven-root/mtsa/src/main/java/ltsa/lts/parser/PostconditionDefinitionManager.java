package ltsa.lts.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.annotation.Nonnull;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.ltl.FormulaSyntax;
import ltsa.lts.ltl.PostconditionDefinition;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.actions.LabelSet;

import com.google.common.base.Preconditions;

/**
 * Manages the post-conditions. Maps the name to the corresponding
 * postcondition, given a process and a box keeps track of the corresponding
 * post-conditions
 *
 */
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

	public PostconditionDefinitionManager() {
		reset();
	}

	/**
	 * returns true if the box of the given process has a post-conditions
	 * associated
	 * 
	 * @param process
	 *            the process to be considered
	 * @param box
	 *            the box to be considered
	 * @return true if there is a post condition associated with the box of the
	 *         current process
	 * @throws NullPointerException
	 *             if one of the parameters is null
	 */
	public boolean hasPostCondition(@Nonnull String process, @Nonnull String box) {
		Preconditions.checkNotNull(process,
				"The process to be considered cannot be null");
		Preconditions.checkNotNull(box,
				"The box to be considered cannot be null");
		if (!this.mapBoxPostconditions.containsKey(process)) {
			return false;
		} else {
			return this.mapBoxPostconditions.get(process).containsKey(box);
		}
	}

	/**
	 * returns the postcondition of the box of the given process
	 * 
	 * @param process
	 *            the process to be considered
	 * @param box
	 *            the box to be considered
	 * @return true if there is a post condition associated with the box of the
	 *         current process
	 * @throws NullPointerException
	 *             if one of the parameters is null
	 * @throws IllegalArgumentException
	 *             if the box is not associated with a post-condition
	 */
	public String getPostCondition(@Nonnull String process, @Nonnull String box) {
		Preconditions.checkArgument(this.hasPostCondition(process, box),
				"No post-condition is associated with the box: " + box
						+ " of the process: " + process);
		return this.mapBoxPostconditions.get(process).get(box);
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
				new PostconditionDefinition(postConditionName.getValue(),
						postConditionName, f, ls, ip, p, box)) != null) {
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

	public CompositeState compile(LTSOutput output,
			Set<String> alphabetCharacters, String name) {
		Preconditions.checkNotNull(name,
				"The name of the post-condition cannot be null");
		Preconditions
				.checkArgument(
						postconditions.containsKey(name),
						"The post-condition "
								+ name
								+ " is not contained into the set of the preconditions");
		PostconditionDefinition post = postconditions.get(name);

		output.outln("FORMULA: " + post.getFac().getFormula() + " considered");

		return new LTLf2LTS().toCompositeState(post.getFac().getFormula(),
				output, alphabetCharacters, name);
	}

	public LabelledTransitionSystem toFiniteLTS(LTSOutput output,
			Set<String> alphabetCharacters, String name) {
		return this.compile(output, alphabetCharacters, name).getComposition();
	}
}