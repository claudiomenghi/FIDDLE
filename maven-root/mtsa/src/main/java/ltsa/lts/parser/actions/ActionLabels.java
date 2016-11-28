package ltsa.lts.parser.actions;

import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.parser.Value;

public abstract class ActionLabels {

	public ActionLabels follower; // next part of compound label

	protected Hashtable<String, Value> locals;
	protected Hashtable<String, Value> globals;

	public void addFollower(ActionLabels f) {
		follower = f;
	}

	public ActionLabels getFollower() {
		return follower;
	}

	/**
	 * initializes context for label generation
	 */
	public void initContext(Hashtable<String, Value> locals, Hashtable<String, Value> globals) {
		this.locals = locals;
		this.globals = globals;
		initialise();
		checkDuplicateVarDefn();
		if (follower != null) {
			follower.initContext(locals, globals);
		}
	}

	public void clearContext() {
		removeVarDefn();
		if (follower != null)
			follower.clearContext();
	}

	/**
	 * - returns string for this label and moves counter
	 */
	public String nextName() {
		String s = computeName();
		if (follower != null) {
			s = s + "." + follower.nextName();
			if (!follower.hasMoreNames()) {
				follower.initialise();
				next();
			}
		} else {
			next();
		}
		return s;
	}

	/**
	 * - returns false if no more names
	 */
	public abstract boolean hasMoreNames();

	/**
	 * default implementations for ActionLabels with no variables
	 */
	public Vector<String> getActions(Hashtable<String, Value> locals, Hashtable<String, Value> constants) {
		Vector<String> v = new Vector<>();
		initContext(locals, constants);
		while (hasMoreNames()) {
			String s = nextName();
			v.addElement(s);
		}
		clearContext();
		return v;
	}

	public boolean hasMultipleValues() {
		if (this instanceof ActionRange || this instanceof ActionSet
				|| this instanceof ActionVarRange
				|| this instanceof ActionVarSet)
			return true;
		else if (follower != null)
			return follower.hasMultipleValues();
		return false;
	}

	/**
	 * default implementations for ActionLabels with no variables
	 */

	protected void checkDuplicateVarDefn() {
	}

	protected void removeVarDefn() {
	}

	protected abstract String computeName();

	protected abstract void next();

	protected abstract void initialise();

	/*
	 * clone operation
	 */

	public ActionLabels myclone() {
		ActionLabels an = make();
		if (follower != null)
			an.follower = follower.myclone();
		return an;
	}

	protected abstract ActionLabels make();

}
