package MTSSynthesis.ar.dc.uba.model.condition;

import java.util.Set;

import MTSSynthesis.ar.dc.uba.model.language.Symbol;

/**
 * Fluent definition
 * @author gsibay
 *
 */
public interface Fluent {

	/**
	 * Returns the Fluent's name
	 * @return
	 */
	public String getName();
	
	/**
	 * Returns the Fluent's initial value
	 * @return
	 */
	boolean isInitialValue();
	
	/**
	 * Returns the Fluent's initiating actions
	 * @return
	 */
	public Set<Symbol> getInitiatingActions();
	
	/**
	 * Returns the Fluent's terminating actions
	 * @return
	 */
	public Set<Symbol> getTerminatingActions();
}
