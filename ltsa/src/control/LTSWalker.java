package control;

import java.util.ArrayList;
import java.util.List;

import ac.ic.doc.commons.relations.BinaryRelation;
import ac.ic.doc.commons.relations.Pair;
import ac.ic.doc.mtstools.model.MTS;

public class LTSWalker
{
  public static List<Long> walk(MTS<Long,String> env, List<String> trace)
  {
    List<Long> frontStates = new ArrayList<Long>();
    frontStates.add(env.getInitialState());
    for (String action : trace)
    {
      List<Long> newFront = new ArrayList<Long>();
      for (Long state : frontStates)
      {
        BinaryRelation<String,Long> transitions = env.getTransitions(state, MTS.TransitionType.REQUIRED);
        for (Pair<String,Long> transition : transitions)
          if (transition.getFirst().equals(action))
            newFront.add(transition.getSecond());
        //if the state cannot continue with the trace it will not appear in the new front
      }
      frontStates = newFront;
    }
    return frontStates;
  }
}
