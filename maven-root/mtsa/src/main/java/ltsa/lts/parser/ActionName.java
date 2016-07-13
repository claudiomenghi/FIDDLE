package ltsa.lts.parser;


/**
 * -- evaluate lowerident labels
 */
public class ActionName extends ActionLabels {

	public Symbol name;

	public ActionName(Symbol name) {
		this.name = name;
	}

	protected String computeName() {
		return name.toString();
	}

	protected boolean consumed;

	protected void initialise() {
		consumed = false;
	}

	protected void next() {
		consumed = true;
	}

	public boolean hasMoreNames() {
		return !consumed;
	}

	protected ActionLabels make() {
		return new ActionName(name);
	}
	
}