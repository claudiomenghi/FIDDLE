package ltsa.lts;

import java.util.Vector;

/**
 * -- evaluate {a,b,c,d,e} labels
 */
public class ActionSet extends ActionLabels {

	public LabelSet set;
	protected Vector<String> actions;

	public ActionSet(LabelSet set) {
		this.set = set;
	}

	protected String computeName() {
		return (String) actions.elementAt(current);
	}

	protected int current, high, low;

	protected void initialise() {
		actions = set.getActions(locals, globals);
		current = low = 0;
		high = actions.size() - 1;
	}

	protected void next() {
		++current;
	}

	public boolean hasMoreNames() {
		return (current <= high);
	}

	protected ActionLabels make() {
		return new ActionSet(set);
	}
	
}
