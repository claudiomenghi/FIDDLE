package ltsa.ui;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ltsa.lts.automata.lts.LTSError;
import ltsa.lts.automata.lts.LTSException;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.parser.LTSCompiler;
import ltsa.lts.parser.SymbolTable;
import ltsa.lts.parser.actions.LabelSet;
import ltsa.lts.parser.ltsinput.LTSInput;

public class MinimalManager implements LTSManager {
	
	LTSInput input;
	LTSOutput output;
	LTSError error;
	String currentDirectory;

	Hashtable<String,LabelSet> labelSetConstants = null;
	
	public MinimalManager(LTSInput input, LTSOutput output, LTSError error, 
			String currentDirectory) {
		this.input = input;
		this.output = output;
		this.error = error;
		this.currentDirectory = currentDirectory;
		//SymbolTable.init();
		parse();
	}

    public boolean parse() {
        input.resetMarker();
        Hashtable cs;        
        LTSCompiler comp = new LTSCompiler(input,output,currentDirectory);
        try {
            comp.parse(cs = new Hashtable(),new Hashtable(),new Hashtable());
            
        } catch (LTSException x) {
            error.displayError(x);
            cs = null;
        }
        
        labelSetConstants = LabelSet.getConstants();
        return cs != null;
    }

	public CompositeState compile(String name) {	
        input.resetMarker();
        CompositeState cs=null;
        LTSCompiler comp = new LTSCompiler(input,output,currentDirectory);
        try {
        	comp.compile();
            cs = comp.continueCompilation(name, new EmptyLTSOuput());
        } catch (LTSException x) {
            error.displayError(x);
        }
        return cs;
	}

	public Set<String> getLabelSet(String name) {
		if (labelSetConstants == null)
			return null;
		
		Set<String> s = new HashSet<String>();
		LabelSet ls = labelSetConstants.get(name);
		
		if (ls == null)
			return null;
		
		for ( String a : (Vector<String>) ls.getActions(null) )
			s.add(a);
		
		return s;
	}

	public String getTargetChoice() { return null; }
	public void newMachines(List<LabelledTransitionSystem> machines) { }
	public void performAction(Runnable r, boolean showOutputPane) { r.run(); }

}
