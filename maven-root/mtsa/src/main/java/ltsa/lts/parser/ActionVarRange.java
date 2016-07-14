package ltsa.lts.parser;

import java.util.Stack;

import ltsa.lts.Diagnostics;
import ltsa.lts.csp.Range;

/**
 * -- evaluate [i:low..high] labels
 */
public class ActionVarRange extends ActionRange {

	protected Symbol var;

	public ActionVarRange(Symbol var, Stack low, Stack high) {
		super(low, high);
		this.var = var;
	}

	public ActionVarRange(Symbol var, Range r) {
		super(r);
		this.var = var;
	}

	@Override
	protected String computeName() {
		if (locals != null)
			locals.put(var.toString(), new Value(current));
		return String.valueOf(current);
	}

	@Override
	protected void checkDuplicateVarDefn() {
		if (locals == null)
			return;
		if (locals.get(var.toString()) != null)
			Diagnostics.fatal("Duplicate variable definition: " + var, var);
	}

	@Override
	protected void removeVarDefn() {
		if (locals != null)
			locals.remove(var.toString());
	}

	@Override
	protected ActionLabels make() {
		return new ActionVarRange(var, rlow, rhigh);
	}

}
