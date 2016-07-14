package ltsa.lts.csp;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.google.common.base.Preconditions;

import ltsa.lts.Diagnostics;
import ltsa.lts.lts.StateMachine;
import ltsa.lts.parser.LabelSet;
import ltsa.lts.parser.Symbol;

/**
 * contains the declaration of a process
 * @author Claudio Menghi
 *
 */
public class ProcessSpec extends Declaration {
	
	private Symbol name;
	
	public Hashtable constants;
	public Hashtable init_constants = new Hashtable();
	public Vector parameters = new Vector();
	public Vector<StateDefn> stateDefns = new Vector<>();
	public LabelSet alphaAdditions;
	public LabelSet alphaHidden;
	public Vector alphaRelabel;
	public boolean isProperty = false;
	public boolean isMinimal = false;
	public boolean isDeterministic = false;
	public boolean isOptimistic = false;
	public boolean isPessimistic = false;
	public boolean isClousure = false;
	public boolean isAbstract = false;
	public boolean exposeNotHide = false;
	public boolean isController = false;

	public boolean isProbabilistic = false;
	public boolean isMDP = false;
	public boolean isStarEnv = false;
	public Symbol goal;

	public File importFile = null; // used if the process is imported from a
									// .aut file

	public ProcessSpec(){
		
	}
	public ProcessSpec(Symbol name){
		Preconditions.checkNotNull(name, "The name of the process cannot be null");
		this.name=name;
	}
	
	public Symbol getSymbol(){
		return name;
	}
	
	public boolean imported() {
		return importFile != null;
	}

	public String getname() {
		constants = (Hashtable) init_constants.clone();
		StateDefn s = stateDefns.firstElement();
		name = s.name;
		if (s.range != null)
			Diagnostics.fatal("process name cannot be indexed", name);
		return s.name.toString();
	}
	
	@Override
	public void explicitStates(StateMachine m) {
		Enumeration<StateDefn> e = stateDefns.elements();
		while (e.hasMoreElements()) {
			Declaration d = e.nextElement();
			d.explicitStates(m);
		}
	}

	public void addAlphabet(StateMachine m) {
		if (alphaAdditions != null) {
			Vector a = alphaAdditions.getActions(constants);
			Enumeration e = a.elements();
			while (e.hasMoreElements()) {
				String s = (String) e.nextElement();
				if (!m.getAlphabet().contains(s))
					m.addEvent(s);
			}
		}
	}

	public void hideAlphabet(StateMachine m) {
		if (alphaHidden == null) {
			return;
		}
		m.setHidden(alphaHidden.getActions(constants));
	}

	public void relabelAlphabet(StateMachine m) {
		if (alphaRelabel == null){
			return;
		}
		m.setRelabels(new Relation());
		Enumeration e = alphaRelabel.elements();
		while (e.hasMoreElements()) {
			RelabelDefn r = (RelabelDefn) e.nextElement();
			r.makeRelabels(constants, m.getRelabels());
		}
	}

	@Override
	public void crunch(StateMachine m) {
		Enumeration<StateDefn> e = stateDefns.elements();
		while (e.hasMoreElements()) {
			Declaration d = e.nextElement();
			d.crunch(m);
		}
	}

	@Override
	public void transition(StateMachine m) {
		Enumeration<StateDefn> e = stateDefns.elements();
		while (e.hasMoreElements()) {
			Declaration d = e.nextElement();
			d.transition(m);
		}
	}

	public void doParams(Vector actuals) {
		Enumeration a = actuals.elements();
		Enumeration f = parameters.elements();
		while (a.hasMoreElements() && f.hasMoreElements())
			constants.put(f.nextElement(), a.nextElement());
	}

	public ProcessSpec myclone() {
		ProcessSpec p = new ProcessSpec(name);
		p.constants = (Hashtable) constants.clone();
		p.init_constants = init_constants;
		p.parameters = parameters;
		Enumeration<StateDefn> e = stateDefns.elements();
		while (e.hasMoreElements())
			p.stateDefns.addElement(e.nextElement().myclone());
		p.alphaAdditions = alphaAdditions;
		p.alphaHidden = alphaHidden;
		p.alphaRelabel = alphaRelabel;
		p.isProperty = isProperty;
		p.isMinimal = isMinimal;
		p.isDeterministic = isDeterministic;
		p.exposeNotHide = exposeNotHide;
		p.importFile = importFile;
		p.isOptimistic = isOptimistic;
		return p;
	}

}
