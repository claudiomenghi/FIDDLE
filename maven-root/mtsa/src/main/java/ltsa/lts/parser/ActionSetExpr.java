package ltsa.lts.parser;

import java.util.Enumeration;
import java.util.Vector;

/**
 * -- evaluates {a,b,c,d,e}\{d,e} labels
 */

public class ActionSetExpr extends ActionLabels {

	public LabelSet left;
	public LabelSet right;
	public Vector<String> actions;

	public ActionSetExpr(LabelSet left, LabelSet right) {
		this.left = left;
		this.right = right;
	}

	protected String computeName() {
		return (String) actions.elementAt(current);
	}

	protected int current, high, low;

	protected void initialise() {
		Vector left_actions = left.getActions(locals, globals);
		Vector right_actions = right.getActions(locals, globals);
		actions = new Vector();
		Enumeration e = left_actions.elements();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			if (!right_actions.contains(s))
				actions.addElement(s);
		}
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
		return new ActionSetExpr(left, right);
	}
	
}