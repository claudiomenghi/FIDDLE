package ltsa.lts.ltl;

import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.parser.Symbol;
import ltsa.lts.parser.Value;
import ltsa.lts.parser.actions.LabelSet;

public class PostconditionDefinition extends FormulaDefinition {

	public PostconditionDefinition(Symbol n, FormulaSyntax f, LabelSet ls,
			Hashtable<String, Value> ip, Vector<String> p, String box) {
		super(n, f, ls, ip, p);
		this.getFac().setFormula(
				this.getLTLFormula().expand(this.getFac(), new Hashtable<>(),
						this.getInitialParams()));
	}	
}