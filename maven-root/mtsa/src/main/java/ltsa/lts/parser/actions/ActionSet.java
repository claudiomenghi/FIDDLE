package ltsa.lts.parser.actions;

import java.util.Vector;

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Preconditions;

/**
 * -- evaluate {a,b,c,d,e} labels
 */
public class ActionSet extends ActionLabels {
	protected final Log logger = LogFactory.getLog(getClass());
	
	private LabelSet set;
	protected Vector<String> actions;

	protected int current, high, low;

	public ActionSet(@Nonnull LabelSet set) {
		Preconditions.checkNotNull(set, "The set of labels cannot be null");
		this.set = set;
	}

	@Override
	protected String computeName() {
		return actions.elementAt(current);
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		if (actions != null) {
			actions.forEach(action -> builder.append(action + "\t"));
		}
		builder.append("}");
		builder.append(" label set: {");
		if (set != null && set.labels != null) {
			set.labels.forEach(action -> builder.append(action + "\t"));
		}
		builder.append("}");
		return builder.toString();
	}

	public LabelSet getLabelSet() {
		return this.set;
	}
}
