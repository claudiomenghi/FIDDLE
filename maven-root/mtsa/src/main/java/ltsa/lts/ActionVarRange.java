package ltsa.lts;

import java.util.Stack;

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

	protected String computeName() {
		if (locals != null)
			locals.put(var.toString(), new Value(current));
		return String.valueOf(current);
	}

	protected void checkDuplicateVarDefn() {
		if (locals == null)
			return;
		if (locals.get(var.toString()) != null)
			Diagnostics.fatal("Duplicate variable definition: " + var, var);
	}

	protected void removeVarDefn() {
		if (locals != null)
			locals.remove(var.toString());
	}

	protected ActionLabels make() {
		return new ActionVarRange(var, rlow, rhigh);
	}

}
