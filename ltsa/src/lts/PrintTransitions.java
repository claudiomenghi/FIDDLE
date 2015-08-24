package lts;



public class PrintTransitions {
  
  CompactState sm;
  
  public PrintTransitions (CompactState sm) {
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
    output.outln("\t"+sm.name);
    // print number of states
    output.outln("States:");
    output.outln("\t"+sm.maxStates);
    output.outln("Transitions:");
    output.outln("\t"+sm.name+ " = Q0,");
    for (int i = 0; i<sm.maxStates; i++ ){
      output.out("\tQ"+i+"\t= ");
      EventState current = EventStateUtils.transpose(sm.states[i]);
      if (current == null) {
        if (i==sm.endseq)
          output.out("END");
        else
          output.out("STOP");
        if (i<sm.maxStates-1) 
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
          String[] events = EventState.eventsToNext(current,sm.alphabet);
          Alphabet a = new Alphabet(events);
          
          if (current instanceof ProbabilisticEventState) {
        	  int evCnt= 0;
        	  for (String evt : events) {
        		  ProbabilisticEventState probCurr= (ProbabilisticEventState) current;
        		  while (!sm.alphabet[probCurr.event].equals(evt) && probCurr != null) {
        			  probCurr= (ProbabilisticEventState) probCurr.nondet;
        		  }
        		  
        		  if (probCurr == null)
        			  Diagnostics.fatal("Event " + evt + " was not found for state Q" + i);

        		  output.out(evt + " -> ");
        		  output.out("{" + probCurr.getBundle() + ":" + probCurr.getProbability() + "} ");
                  if (current.next<0) 
                      output.out("ERROR"); 
                    else 
                      output.out("Q"+current.next);

                  evCnt++;
        		  if (evCnt != events.length) {
        	            output.out("\n\t\t  |");
        		  }
        	  }
          } else {
              output.out(a.toString() + " -> ");
              if (current.next<0) 
                  output.out("ERROR"); 
                else 
                  output.out("Q"+current.next);
          }
          current = current.list;

          if (current==null) {
            if (i<sm.maxStates-1) 
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
  
  