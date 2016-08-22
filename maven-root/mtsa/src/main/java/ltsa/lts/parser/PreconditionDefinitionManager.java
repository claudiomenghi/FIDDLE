package ltsa.lts.parser;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.Diagnostics;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.ltl.AssertDefinition;
import ltsa.lts.ltl.FormulaSyntax;
import ltsa.lts.ltl.PreconditionDefinition;
import ltsa.lts.ltl.ltlftoba.LTLf2LTS;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.actions.LabelSet;

import com.google.common.base.Preconditions;

public class PreconditionDefinitionManager {

	private Map<String, PreconditionDefinition> preconditions;

	/**
	 * maps each process and corresponding box to the associated precondition
	 */
	private Map<Entry<String, String>, String> mapProcessBoxToPrecondition;

	public static boolean addAsterisk = true;

	public PreconditionDefinitionManager() {
		this.preconditions = new HashMap<>();
		this.mapProcessBoxToPrecondition = new HashMap<>();
	}

	public boolean containsPrecondition(String process, String box) {
		return this.mapProcessBoxToPrecondition
				.containsKey(new AbstractMap.SimpleEntry<>(process, box));
	}

	/**
	 * given the process and the corresponding box returns the pre-condition
	 * 
	 * @param process
	 *            the process to be considered
	 * @param box
	 *            the box to be analyzed
	 * @return the corresponding precondition
	 * @throws NullPointerException
	 *             if one of the parameter is null
	 * @throws IllegalArgumentException
	 *             if the couple <process, box> is not associated with a
	 *             precondition
	 */
	public PreconditionDefinition getPrecondition(String process, String box) {
		Preconditions.checkNotNull(process, "The process cannot be null");
		Preconditions.checkNotNull(box, "The box cannot be null");
		Preconditions.checkArgument(this.mapProcessBoxToPrecondition
				.containsKey(new AbstractMap.SimpleEntry<>(process, box)),
				"No precondition associated with the process: " + process
						+ " and the box: " + box);

		String preconditionName = this.mapProcessBoxToPrecondition
				.get(new AbstractMap.SimpleEntry<>(process, box));

		return this.preconditions.get(preconditionName);
	}

	/**
	 * return the name of the pre-condition
	 * 
	 * @param process
	 *            the process to be considered
	 * @param box
	 *            the box to be analyzed
	 * @return the corresponding precondition
	 * @throws NullPointerException
	 *             if one of the parameter is null
	 * @throws IllegalArgumentException
	 *             if the couple <process, box> is not associated with a
	 *             precondition
	 */
	public String getPreconditionName(String process, String box) {

		Preconditions.checkNotNull(process, "The process cannot be null");
		Preconditions.checkNotNull(box, "The box cannot be null");
		Preconditions.checkArgument(this.mapProcessBoxToPrecondition
				.containsKey(new AbstractMap.SimpleEntry<>(process, box)));

		return this.mapProcessBoxToPrecondition
				.get(new AbstractMap.SimpleEntry<>(process, box));
	}

	public void reset() {
		this.preconditions = new HashMap<>();
	}

	public Map<String, PreconditionDefinition> getPreconditions() {
		return this.preconditions;
	}

	public void put(Symbol n, FormulaSyntax f, LabelSet ls,
			Hashtable<String, Value> ip, Vector<String> p, String process,
			String box) {
		if (preconditions == null) {
			preconditions = new HashMap<>();
		}
		if (preconditions.put(n.toString(), new PreconditionDefinition(n, f,
				ls, ip, p)) != null) {
			Diagnostics.fatal("duplicate preconditions definition: " + n, n);
		}
		this.mapProcessBoxToPrecondition.put(new AbstractMap.SimpleEntry<>(
				process, box), n.toString());

	}

	/**
	 * returns a state machine describing the violating behaviors
	 * 
	 * @param output
	 *            the output used to print messages
	 * @param asserted
	 *            the string representing the precondition to be considered
	 * @param name
	 *            the name of the pre-condition
	 * @return a state machine describing the violating behaviors
	 * @throws IllegalArgumentException
	 *             if the string representing the precondition is not a valid
	 *             string
	 */
	public CompositeState compile(LTSOutput output,
			List<String> alphabetCharacters, String name) {
		Preconditions
				.checkArgument(
						preconditions.containsKey(name),
						"The precondition "
								+ name
								+ " is not contained into the set of the preconditions");
		PreconditionDefinition precondition = preconditions.get(name);

		output.outln("FORMULA: " + precondition.getFac().getFormula()
				+ " considered");

		return new LTLf2LTS().toCompositeState(precondition.getFac()
				.getFormula(), output, alphabetCharacters, name);
	}

	public CompositeState toProperty(LTSOutput output,
			List<String> alphabetCharacters, String name) {
		Preconditions
				.checkArgument(
						preconditions.containsKey(name),
						"The precondition "
								+ name
								+ " is not contained into the set of the preconditions");
		PreconditionDefinition precondition = preconditions.get(name);

		output.outln("FORMULA: " + precondition.getFac().getFormula()
				+ " considered");

		return new LTLf2LTS().toProperty(precondition.getFac().getFormula(),
				output, alphabetCharacters, name);
	}

	/**
	 * returns the name of the preconditions
	 * 
	 * @return the name of the preconditions
	 */
	public Set<String> names() {
		Set<String> defs = new HashSet<>();

		for (String elem : preconditions.keySet()) {
			if (!elem.startsWith(AssertDefinition.NOT_DEF)) {
				defs.add(elem);
			}
		}

		return defs;
	}
}
