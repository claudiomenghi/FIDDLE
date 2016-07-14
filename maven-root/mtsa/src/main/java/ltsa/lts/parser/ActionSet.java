package ltsa.lts.parser;

import java.util.Vector;

/**
 * -- evaluate {a,b,c,d,e} labels
 */
public class ActionSet extends ActionLabels {

	public LabelSet set;
	protected Vector<String> actions;

	protected int current, high, low;

	
	public ActionSet(LabelSet set) {
		this.set = set;
	}

	@Override
	protected String computeName() {
		return (String) actions.elementAt(current);
	}

	@Override
	protected void initialise() {
		actions = set.getActions(locals, globals);
		current = low = 0;
		high = actions.size() - 1;
	}

	@Override
	protected void next() {
		++current;
	}

	@Override
	public boolean hasMoreNames() {
		return (current <= high);
	}

	@Override
	protected ActionLabels make() {
		return new ActionSet(set);
	}
	
}
