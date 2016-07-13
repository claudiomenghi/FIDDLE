package ltsa.lts.csp;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import ltsa.lts.ltscomposition.CompactState;
import ltsa.lts.ltscomposition.CompositeState;
import ltsa.lts.parser.ActionLabels;
import ltsa.lts.parser.Expression;
import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;

/* -----------------------------------------------------------------------*/
public class CompositeBody {
	// single process reference P
	public ProcessRef singleton;
	// list of CompositeBodies ( P || Q)
	public Vector<CompositeBody> procRefs;
	// conditional if Bexp then P else Q
	public Stack<Symbol> boolexpr;
	public CompositeBody thenpart; // overloaded as body of replicator
	public CompositeBody elsepart;
	// forall[i:R][j:S].
	public ActionLabels range; // used to store forall range/ranges
	// the following are only applied to singletons & procRefs (...)
	private ActionLabels prefix; // a:
	public ActionLabels accessSet; // S::
	public Vector<RelabelDefn> relabelDefns; // list of relabelling defns

	// private Vector accessors = null; //never used?
	// private Relation relabels = null;

	/**
	 * This method fills the <i>machines</i> with the compiled version of the
	 * processes referenced in the composition expression <i>c</i>. In addition
	 * to handling conditionals and applying accessors it also applies
	 * relabelling if necessary.
	 */
	void compose(CompositionExpression c, Vector machines,
			Hashtable<String, Value> locals) {
		Vector<String> accessors = accessSet == null ? null : accessSet
				.getActions(locals, c.constants);
		Relation relabels = RelabelDefn.getRelabels(relabelDefns, c.constants,
				locals);
		// conditional compostion
		if (boolexpr != null) {
			if (Expression.evaluate(boolexpr, locals, c.constants).intValue() != 0)
				thenpart.compose(c, machines, locals);
			else if (elsepart != null)
				elsepart.compose(c, machines, locals);
		} else if (range != null) {
			// replicated composition
			range.initContext(locals, c.constants);
			while (range.hasMoreNames()) {
				range.nextName();
				thenpart.compose(c, machines, locals);
			}
			range.clearContext();
		} else {
			// singleton or list
			Vector tempMachines = getPrefixedMachines(c, locals);
			// apply accessors
			if (accessors != null)
				for (Object o : tempMachines) {
					if (o instanceof CompactState) {
						CompactState mach = (CompactState) o;
						mach.addAccess(accessors);
					} else {
						CompositeState cs = (CompositeState) o;
						cs.addAccess(accessors);
					}
				}
			// apply relabels
			if (relabels != null)
				for (int i = 0; i < tempMachines.size(); ++i) {
					Object o = tempMachines.elementAt(i);
					if (o instanceof CompactState) {
						CompactState mach = (CompactState) o;
						mach.relabel(relabels);
					} else {
						CompositeState cs = (CompositeState) o;
						CompactState mm = cs.relabel(relabels, c.output);
						if (mm != null)
							tempMachines.setElementAt(mm, i);
					}
				}
			// add tempMachines to machines
			machines.addAll(tempMachines);
		}
	}

	/**
	 * Relabels the set of referenced processes in <i>c<\i>.
	 */
	private Vector getPrefixedMachines(CompositionExpression c,
			Hashtable<String, Value> locals) {
		if (prefix == null) {
			return getMachines(c, locals);
		} else {
			Vector pvm = new Vector();
			prefix.initContext(locals, c.constants);
			while (prefix.hasMoreNames()) {
				String px = prefix.nextName();
				Vector vm = getMachines(c, locals);
				for (Object o : vm) {
					if (o instanceof CompactState) {
						CompactState m = (CompactState) o;
						m.prefixLabels(px);
						pvm.addElement(m);
					} else {
						CompositeState cs = (CompositeState) o;
						cs.prefixLabels(px);
						pvm.addElement(cs);
					}
				}
			}
			prefix.clearContext();
			return pvm;
		}
	}

	/**
	 * Computes the set of referenced processes in <i>c<\i>.
	 */
	private Vector getMachines(CompositionExpression c,
			Hashtable<String, Value> locals) {
		Vector vm = new Vector();
		if (singleton != null) {
			singleton.instantiate(c, vm, c.output, locals);
		} else if (procRefs != null) {
			for (CompositeBody cb : procRefs) {
				cb.compose(c, vm, locals);
			}
		}
		return vm;
	}

	public ActionLabels getPrefix() {
		return prefix;
	}

	public void setPrefix(ActionLabels prefix) {
		this.prefix = prefix;
	}

}