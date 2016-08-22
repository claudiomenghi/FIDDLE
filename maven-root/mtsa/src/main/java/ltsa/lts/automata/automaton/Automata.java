package ltsa.lts.automata.automaton;

import java.util.List;
import java.util.Vector;

import ltsa.lts.operations.composition.parallel.StackCheck;
import ltsa.lts.util.collections.MyList;

/**
 * This interface presents the common operations between composed automata &
 * on-the-fly composition
 */
public interface Automata {

	/**
	 * Returns the alphabet of the automata
	 * 
	 * @return the alphabet of the automata
	 */
	public String[] getAlphabet();

	/**
	 * returns the transitions from a particular state
	 * 
	 * @param state
	 *            the state to be considered
	 * @return the transitions from the particular state
	 */
	public MyList getTransitions(byte[] state);

	// returns name of violated property if ERROR found in getTransitions
	public String getViolatedProperty();

	/**
	 *  returns shortest trace to state (vector of Strings)
	 * @param from
	 * @param to
	 * @return
	 */
	public Vector<String> getTraceToState(byte[] from, byte[] to);

	/**
	 * returns true if END state
	 * 
	 * @param state
	 * @return true if END state
	 */
	public boolean end(byte[] state);

	/**
	 * returns true if Accepting state
	 * 
	 * @param state
	 * @return
	 */
	public boolean isAccepting(byte[] state);

	/**
	 * returns the number of the START state
	 * 
	 * @return the number of the START state
	 */
	public byte[] start();

	/**
	 * set the Stack Checker for partial order reduction
	 * 
	 * @param s
	 */
	public void setStackChecker(StackCheck s);

	/**
	 * true if partial order reduction
	 * 
	 * @return true if partial order reduction
	 */
	public boolean isPartialOrder();

	/**
	 * diable partial order
	 */
	public void disablePartialOrder();

	/**
	 * enable partial order
	 */
	public void enablePartialOrder();
}