package ltsa.lts.csp;

import java.util.Hashtable;
import java.util.Stack;

import ltsa.lts.parser.Symbol;

/* -----------------------------------------------------------------------*/

public class Range extends Declaration {
	public static Hashtable<String, Range> ranges;
	public Stack<Symbol> low;
	public Stack<Symbol> high;
}