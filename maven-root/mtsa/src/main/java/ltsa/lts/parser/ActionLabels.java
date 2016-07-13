package ltsa.lts.parser;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public abstract class ActionLabels {

	public ActionLabels follower; // next part of compound label

	public void addFollower(ActionLabels f) {
		follower = f;
	}

	public ActionLabels getFollower() {
		return follower;
	}

	protected Hashtable locals;
	protected Hashtable globals;

	/**
	 * - initialises context for label generation
	 */
	public void initContext(Hashtable locals, Hashtable globals) {
		this.locals = locals;
		this.globals = globals;
		initialise();
		checkDuplicateVarDefn();
		if (follower != null)
			follower.initContext(locals, globals);
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

	public Vector<String> getActions(Hashtable locals, Hashtable constants) {
		Vector<String> v = new Vector<String>();
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












