package ltsa.lts.parser.actions;

import ltsa.lts.parser.Symbol;


/**
 * -- evaluate lowerident labels
 */
public class ActionName extends ActionLabels {

	public Symbol name;

	public ActionName(Symbol name) {
		this.name = name;
	}

	@Override
	protected String computeName() {
		return name.toString();
	}

	protected boolean consumed;

	@Override
	protected void initialise() {
		consumed = false;
	}

	@Override
	protected void next() {
		consumed = true;
	}

	@Override
	public boolean hasMoreNames() {
		return !consumed;
	}

	@Override
	protected ActionLabels make() {
		return new ActionName(name);
	}
	
}