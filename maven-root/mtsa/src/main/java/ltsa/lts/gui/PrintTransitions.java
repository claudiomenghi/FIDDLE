package ltsa.lts.gui;

import ltsa.lts.Diagnostics;
import ltsa.lts.EventStateUtils;
import ltsa.lts.automata.lts.Alphabet;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;
import ltsa.lts.automata.probabilistic.ProbabilisticEventState;
import ltsa.lts.output.LTSOutput;



public class PrintTransitions {
  
  LabelledTransitionSystem sm;
  
  public PrintTransitions (LabelledTransitionSystem sm) {
      this.sm = sm;
  }
    public void print( LTSOutput output ) {
    	//ISSUE
        print( output , 9000 );
    }
  
  public void print(LTSOutput output, int MAXPRINT) {
    int linecount =0;
    // print name
    output.outln("Process:");
    output.outln("\t"+sm.getName());
    // print number of states
    output.outln("States:");
    output.outln("\t"+sm.getMaxStates());
    output.outln("Transitions:");
    output.outln("\t"+sm.getName()+ " = Q0,");
    for (int i = 0; i<sm.getMaxStates(); i++ ){
      output.out("\tQ"+i+"\t= ");
      LTSTransitionList current = EventStateUtils.transpose(sm.getStates()[i]);
      if (current == null) {
        if (i==sm.getEndOfSequenceIndex())
          output.out("END");
        else
          output.out("STOP");
        if (i<sm.getMaxStates()-1) 
           output.outln(","); 
        else 
           output.outln(".");  
      } else {
        output.out("(");
        while (current != null) {
          linecount++;
          if (linecount>MAXPRINT) {
            output.outln("EXCEEDED MAXPRINT SETTING");
            return;
          }
          String[] events = LTSTransitionList.eventsToNext(current,sm.getAlphabet());
          Alphabet a = new Alphabet(events);
          
          if (current instanceof ProbabilisticEventState) {
        	  int evCnt= 0;
        	  for (String evt : events) {
        		  ProbabilisticEventState probCurr= (ProbabilisticEventState) current;
        		  while (!sm.getAlphabet()[probCurr.getEvent()].equals(evt) && probCurr != null) {
        			  probCurr= (ProbabilisticEventState) probCurr.getNondet();
        		  }
        		  
        		  if (probCurr == null)
        			  Diagnostics.fatal("Event " + evt + " was not found for state Q" + i);

        		  output.out(evt + " -> ");
        		  output.out("{" + probCurr.getBundle() + ":" + probCurr.getProbability() + "} ");
                  if (current.getNext()<0) 
                      output.out("ERROR"); 
                    else 
                      output.out("Q"+current.getNext());

                  evCnt++;
        		  if (evCnt != events.length) {
        	            output.out("\n\t\t  |");
        		  }
        	  }
          } else {
              output.out(a.toString() + " -> ");
              if (current.getNext()<0) 
                  output.out("ERROR"); 
                else 
                  output.out("Q"+current.getNext());
          }
          current = current.getList();

          if (current==null) {
            if (i<sm.getMaxStates()-1) 
              output.outln("),"); 
            else 
              output.outln(").");
          } else {
            output.out("\n\t\t  |");
          }
        }
      }
    }
  }
  
}
  
  