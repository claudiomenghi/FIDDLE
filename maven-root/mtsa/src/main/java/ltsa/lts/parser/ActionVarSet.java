package ltsa.lts.parser;

import ltsa.lts.Diagnostics;

/**
 * -- evaluate [i:low..high] labels
 */
public class ActionVarSet extends ActionSet {

	protected Symbol var;

	public ActionVarSet(Symbol var, LabelSet set) {
		super(set);
		this.var = var;
	}

	@Override
	protected String computeName() {
		String s = (String) actions.elementAt(current);
		if (locals != null)
			locals.put(var.toString(), new Value(s));
		return s;
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
		return new ActionVarSet(var, set);
	}

}