package ltsa.lts.animator;

import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.checkers.Analyser;
import ltsa.lts.gui.EventManager;
import ltsa.lts.ltl.FluentTrace;
import ltsa.lts.operations.composition.parallel.StackCheck;
import ltsa.lts.output.LTSOutput;
import ltsa.lts.util.collections.MyList;

public class AnimatorDecorator implements Animator {
	private Analyser analyser;
	@Override
	public int actionChosen() {
		return analyser.actionChosen();
	}
	@Override
	public String actionNameChosen() {
		return analyser.actionNameChosen();
	}
	public boolean analyse(boolean checkDeadlocks) {
		return analyser.analyse(checkDeadlocks);
	}
	public boolean analyse(FluentTrace tracer) {
		return analyser.analyse(tracer);
	}
	public LabelledTransitionSystem compose() {
		return analyser.compose();
	}
	public LabelledTransitionSystem composeNoHide() {
		return analyser.composeNoHide();
	}
	public void disablePartialOrder() {
		analyser.disablePartialOrder();
	}
	public void enablePartialOrder() {
		analyser.enablePartialOrder();
	}
	public boolean END(byte[] state) {
		return analyser.end(state);
	}
	@Override
	public boolean equals(Object obj) {
		return analyser.equals(obj);
	}
	@Override
	public String[] getAllNames() {
		return alphabetWithNoMaybes();
	}
	private String[] alphabetWithNoMaybes() {
		String[] allNames = analyser.getAllNames();
		String[] result = new String[allNames.length/2];
		for (int i = 0; i < result.length; i++) {
			result[i] = allNames[i];
		}
		return result;
	}
	public String[] getAlphabet() {
		return alphabetWithNoMaybes();
	}
	public List getErrorTrace() {
		return analyser.getErrorTrace();
	}
	@Override
	public String[] getMenuNames() {
		return analyser.getMenuNames();
	}
	@Override
	public boolean getPriority() {
		return analyser.getPriority();
	}
	@Override
	public BitSet getPriorityActions() {
		return analyser.getPriorityActions();
	}
	public Vector getTraceToState(byte[] from, byte[] to) {
		return analyser.getTraceToState(from, to);
	}
	public MyList getTransitions(byte[] state) {
		return analyser.getTransitions(state);
	}
	public String getViolatedProperty() {
		return analyser.getViolatedProperty();
	}
	@Override
	public boolean hasErrorTrace() {
		return analyser.hasErrorTrace();
	}
	@Override
	public int hashCode() {
		return analyser.hashCode();
	}
	@Override
	public BitSet initialise(Vector menu) {
		return analyser.initialise(menu);
	}
	public boolean isAccepting(byte[] state) {
		return analyser.isAccepting(state);
	}
	@Override
	public boolean isEnd() {
		return analyser.isEnd();
	}
	@Override
	public boolean isError() {
		return analyser.isError();
	}
	public boolean isPartialOrder() {
		return analyser.isPartialOrder();
	}
	@Override
	public BitSet menuStep(int choice) {
		return analyser.menuStep(choice);
	}
	@Override
	public void message(String msg) {
		analyser.message(msg);
	}
	@Override
	public boolean nonMenuChoice() {
		return analyser.nonMenuChoice();
	}
	public void setStackChecker(StackCheck s) {
		analyser.setStackChecker(s);
	}
	@Override
	public BitSet singleStep() {
		return analyser.singleStep();
	}
	
	public byte[] START() {
		return analyser.start();
	}
	@Override
	public String toString() {
		return analyser.toString();
	}
	@Override
	public boolean traceChoice() {
		return analyser.traceChoice();
	}
	@Override
	public BitSet traceStep() {
		return analyser.traceStep();
	}
	public AnimatorDecorator(Analyser analyser) {
		this.analyser = analyser;
	}
	
}
