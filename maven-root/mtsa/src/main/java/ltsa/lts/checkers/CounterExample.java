package ltsa.lts.checkers;
import java.util.Enumeration;
import java.util.Vector;

import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.csp.Declaration;
import ltsa.lts.output.LTSOutput;

public class CounterExample {

    protected CompositeState mach;
    protected Vector<String> errorTrace = null;

    public CounterExample(CompositeState m) {
        mach = m;
    }

    public void print(LTSOutput output , boolean checkDeadlocks ) {
        LTSTransitionList trace = new LTSTransitionList(0,0);
        int findState = Declaration.ERROR;
        if (checkDeadlocks){
        	findState = Integer.MIN_VALUE;
        }
        int result = LTSTransitionList.search(
                         trace,
                         mach.getComposition().getStates(),
                         	0,
                         findState,
                         mach.getComposition().getEndOfSequenceIndex(),
                         checkDeadlocks
                     );
        errorTrace = null;
        switch(result) {
        case Declaration.SUCCESS:
            output.outln("No deadlocks/errors");
            break;
        case Declaration.STOP:
           output.outln("Trace to DEADLOCK:");
           errorTrace = LTSTransitionList.getPath(trace.getPath(),mach.getComposition().getAlphabet());
           printPath(output,errorTrace);
           break;
        case Declaration.ERROR:
           errorTrace = LTSTransitionList.getPath(trace.getPath(),mach.getComposition().getAlphabet());
           String name = findComponent(errorTrace);
           output.outln("Trace to property violation in "+name+":");
           printPath(output,errorTrace);
           break;
        }
    }

    private void printPath(LTSOutput output, Vector<String> v) {
        Enumeration<String> e = v.elements();
        while (e.hasMoreElements())
            output.outln("\t"+e.nextElement());
    }

    private String findComponent(Vector<String> trace) {
        Enumeration<LabelledTransitionSystem> e = mach.getMachines().elements();
        while (e.hasMoreElements()) {
            LabelledTransitionSystem cs = e.nextElement();
            if (cs.isErrorTrace(trace)){
            	return cs.getName();
            }
        }
        return "?";
    }
    
    public Vector<String> getErrorTrace(){ return errorTrace;}
}

