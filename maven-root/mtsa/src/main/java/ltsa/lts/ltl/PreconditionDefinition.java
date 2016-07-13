package ltsa.lts.ltl;

import java.util.Hashtable;
import java.util.Vector;

import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;

public class PreconditionDefinition extends FormulaDefinition {

	public PreconditionDefinition(Symbol n, FormulaSyntax f, LabelSet ls,
			Hashtable ip, Vector p) {
		super(n, f, ls, ip, p);
		this.getFac().setFormula(
				this.getLTLFormula().expand(this.getFac(), new Hashtable(),
						this.getInitialParams()));
	}

}
