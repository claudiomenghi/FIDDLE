package ltsa.lts.ltl;

import java.util.HashMap;
import java.util.Map;

import ltsa.lts.ltl.formula.Formula;
import ltsa.lts.parser.Symbol;

public class LTLAdditionalSymbolTable {

	private static Map<Formula, Symbol> table = new HashMap<>();
	private static String preSymbol = "pre";
	private static String postSymbol = "post";

	public static Symbol getPreSymbol(Formula formula, String black) {
		if (!table.containsKey(formula)) {
			table.put(formula, new Symbol(preSymbol + black, Symbol.UPPERIDENT));
		}
		return table.get(formula);

	}
	
	public static Symbol getPostSymbol(Formula formula, String black) {
		if (!table.containsKey(formula)) {
			table.put(formula, new Symbol(postSymbol + black, Symbol.UPPERIDENT));
		}
		return table.get(formula);

	}
}
