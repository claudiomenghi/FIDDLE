package ltsa.control;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ltsa.ac.ic.doc.mtstools.util.fsp.AutomataToMTSConverter;
import ltsa.ac.ic.doc.mtstools.util.fsp.MTSToAutomataConverter;
import ltsa.control.util.GoalDefToControllerGoal;
import ltsa.dispatcher.TransitionSystemDispatcher;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.CompositeState;
import ltsa.lts.csp.CompositionExpression;
import ltsa.lts.operations.minimization.Minimiser;
import ltsa.lts.output.LTSOutput;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import MTSTools.ac.ic.doc.commons.relations.BinaryRelation;
import MTSTools.ac.ic.doc.commons.relations.Pair;
import MTSTools.ac.ic.doc.mtstools.model.MTS;
import MTSTools.ac.ic.doc.mtstools.model.MTS.TransitionType;
import MTSTools.ac.ic.doc.mtstools.model.impl.LTSSimulationSemantics;

public class ControlStackSynthesiser
{  
  private static final long SPECIAL_INIT_STATE = 9997;
  private static final long SPECIAL_EXCEP_STATE1 = 9998;
  private static final long SPECIAL_EXCEP_STATE2 = 9999;
  


  public static boolean checkStackSimulation(CompositeState cStackDef, LTSOutput output)
  {
    List<ControlTierDefinition> tiers = ControlStackDefinition.getDefinition(cStackDef.getName()).getTiers();
    for (int i = tiers.size()-1; i >= 1; i--)
    {
      ControlTierDefinition tierLower = tiers.get(i);
      ControlTierDefinition tierHigher = tiers.get(i-1);
      int tierNum = tiers.size()-i;

      LabelledTransitionSystem lowerEnv = getControlStackEnvironment(cStackDef, tierLower.getEnvModel().toString(), output);
      LabelledTransitionSystem higherEnv = getControlStackEnvironment(cStackDef, tierHigher.getEnvModel().toString(), output);
      
      boolean simulates = true;
      if (higherEnv != lowerEnv) //shortcut for identical environs
        simulates = TransitionSystemDispatcher.isLTSRefinement(higherEnv, lowerEnv, output);
      
      if (simulates)
        output.outln("'"+lowerEnv.getName()+"' simulates '"+higherEnv.getName()+"'.");
      else
      {
        output.outln("Control stack synthesis FAILED: Tier "+tierNum+" environment does not simulate tier "+(tierNum+1)+" environment.");
        return false;
      }
    }
    return true;
  }
  
  private static void setInitialStates(MTS<Long,String> env, String envName, List<Long> initialStates)
  {
    env.addState(SPECIAL_INIT_STATE);
    for (Long s : initialStates)
    {
      System.out.println("  adding initial tau to "+s);
      env.addAction("init_"+envName);
      env.addTransition(SPECIAL_INIT_STATE, "init_"+envName, s, TransitionType.REQUIRED);
    }
    env.setInitialState(SPECIAL_INIT_STATE);
  }
  
  private static LabelledTransitionSystem cleanUpAlphabet(LabelledTransitionSystem machine)
  {
    MTS<Long,String> m2 = AutomataToMTSConverter.getInstance().convert(machine);
    Vector<String> toRemove = new Vector<String>();
    toRemove.add("tau");
    toRemove.add("-1");
    for (String a : m2.getActions())
      if (a.startsWith("@")) //a.endsWith("?") || 
        toRemove.add(a);
    for (String a : toRemove)
      m2.removeAction(a);
    return MTSToAutomataConverter.getInstance().convert(m2, machine.getName(), false);
  }
  
  private static LabelledTransitionSystem solveSafety(LabelledTransitionSystem envModel, Collection<LabelledTransitionSystem> safetyMachines, LTSOutput output)
  {
    //System.out.println("ENV pre-SAFETY "+envModel.getAlphabetV());
    Vector<LabelledTransitionSystem> machines = new Vector<LabelledTransitionSystem>();
    machines.add(envModel);
    machines.addAll(safetyMachines);
    CompositeState parallel = new CompositeState("SAFE_ENVIRON", machines);
    TransitionSystemDispatcher.applyComposition(parallel, output);
    //System.out.println("ENV post-SAFETY "+parallel.composition.getAlphabetV());
    return cleanUpAlphabet(parallel.getComposition());
  }
  
  public static LabelledTransitionSystem addControllerExceptions(int tier, LabelledTransitionSystem controller, List<String> controlledActions)
  {
    MTS<Long,String> controller2 = AutomataToMTSConverter.getInstance().convert(controller);
    controller2.removeAction("tau"); //what?
    controller2.removeAction("-1"); //what?
    
    controller2 = addControllerExceptions(tier, controller2, controlledActions); //was called with controller not controller2 -> inf loop, why never happened before?
    //had to change type of method below to MTS
    
    return MTSToAutomataConverter.getInstance().convert(controller2, controller.getName(), false);
  }
  
  public static MTS<Long,String> addControllerExceptions(int tier, MTS<Long,String> controller, List<String> controlledActions)
  {
    //prepare sets of actions
    Set<String> alphabet = controller.getActions();
    List<String> uncontrolledActions = new Vector<String>();
    uncontrolledActions.addAll(alphabet);
    uncontrolledActions.removeAll(controlledActions);

    //build exception states
    // (s) -- exception --> (-1) -- tier_disabled --> (-2) -- alphabet --> (-2)  
    controller.addState(SPECIAL_EXCEP_STATE1);
    controller.addState(SPECIAL_EXCEP_STATE2);
    for (String action : alphabet)
      controller.addTransition(SPECIAL_EXCEP_STATE2, action, SPECIAL_EXCEP_STATE2, TransitionType.REQUIRED);
    String disabledAction = "tier_disabled"+tier+"";
    controller.addAction(disabledAction);
    controller.addTransition(SPECIAL_EXCEP_STATE1, disabledAction, SPECIAL_EXCEP_STATE2, TransitionType.REQUIRED);
    
    //complete states with missing actions to exception state
    for (long state : controller.getStates())
    {
      if (state != SPECIAL_EXCEP_STATE1 && state != SPECIAL_EXCEP_STATE2)
      {
        List<String> missingUncontrolled = new Vector<String>();
        missingUncontrolled.addAll(uncontrolledActions);
        for (Pair<String,Long> transition : controller.getTransitions(state, TransitionType.REQUIRED))
          missingUncontrolled.remove(transition.getFirst());
        for (String uncon : missingUncontrolled)
          controller.addTransition(state, uncon, SPECIAL_EXCEP_STATE1, TransitionType.REQUIRED);
      }
    }
    return controller;
  }

  private static LabelledTransitionSystem getControlStackEnvironment(CompositeState cStackDef, String name, LTSOutput output)
  {
    Object compactOrComposite = cStackDef.controlStackEnvironments.get(name);
    if (compactOrComposite instanceof CompositeState)
    {
      TransitionSystemDispatcher.applyComposition((CompositeState) compactOrComposite, output);
      return ((CompositeState) compactOrComposite).getComposition();
    }
    else
      return (LabelledTransitionSystem) compactOrComposite;
  }
  
  public static void generatePrismMDP(LabelledTransitionSystem lts, List<String> controlledActions)
  {
    LabelledTransitionSystem completed = addControllerExceptions(1, lts, controlledActions);
    
    MTS<Long,String> lts2 = AutomataToMTSConverter.getInstance().convert(completed);
    //prepare sets of actions
    Set<String> alphabet = lts2.getActions();
    List<String> uncontrolledActions = new Vector<String>();
    uncontrolledActions.addAll(alphabet);
    uncontrolledActions.removeAll(controlledActions);
    
    try
    {
      PrintWriter writer = new PrintWriter(new FileWriter("h:/contsynth/controllers/"+lts.getName()+".nm"));
      
      final String stateVar = "state_"+lts.getName().substring(0, 2); //try to make them unique when combined with other LTSs
      final String responseVar = "response_"+lts.getName().substring(0, 2);
      
      writer.println("mdp //generated by MTSA");
      writer.println("module "+lts.getName());
      writer.println(stateVar+": [0.."+lts2.getStates().size()+"] init 0;");
      writer.println(responseVar+": [0.."+uncontrolledActions.size()+"] init 0;");
      
      writer.println("\n//environment actions");
      for (int r = 0; r < uncontrolledActions.size(); r++)
        writer.println("["+prismifyLabel(uncontrolledActions.get(r))+"] "+responseVar+"="+(r+1)+" -> 1.0:("+responseVar+"'=0);");
      
      writer.println("\n//controlled actions");
      long exceptionState = lts2.getStates().size()-2;
      for (long state : lts2.getStates())
      {
        List<Pair<String,Long>> envTrans = new Vector<Pair<String,Long>>();
        for (Pair<String,Long> transition : lts2.getTransitions(state, TransitionType.REQUIRED))
        {
          if (uncontrolledActions.contains(transition.getFirst()))
          {
            envTrans.add(transition);
            //if (transition.getFirst().equalsIgnoreCase("tier_disabled1"))
            //  exceptionState = state;
          }
          else
            writer.println("["+prismifyLabel(transition.getFirst())+"] "+stateVar+"="+state+" & "+responseVar+"=0 -> 1.0:("+stateVar+"'="+transition.getSecond()+");");
        }
        if (envTrans.size() > 0) //split env actions to unlabelled choice
        {
          String envChoice = "[] "+stateVar+"="+state+" & "+responseVar+"=0 -> ";
          int excCount = 0;
          for (Pair<String,Long> transition : envTrans)
            if (transition.getSecond() == exceptionState)
              excCount++;
          double excProb = 0.1/excCount;
          if (excCount == envTrans.size()) //edge case
            excProb = 1.0/excCount;
          double normalProb = 0.9/(envTrans.size()-excCount);
          if (excCount == 0) //edge case
            normalProb = 1.0/envTrans.size();
          
          for (int i = 0; i < envTrans.size(); i++)
          {//split prob between exp and non-exc
            Pair<String,Long> transition = envTrans.get(i);
            envChoice += (transition.getSecond() == exceptionState ? excProb : normalProb)+":("+responseVar+"'="+(uncontrolledActions.indexOf(transition.getFirst())+1)+")&("+stateVar+"'="+transition.getSecond()+")";
            envChoice += (i < envTrans.size()-1 ? " + " : ";");
          }
          writer.println(envChoice);
        }
      }

      writer.println("endmodule");
      
      if (exceptionState != -1)
        writer.println("\nlabel \"exception\" = "+stateVar+"="+exceptionState+";");
      
      writer.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private static String prismifyLabel(String label)
  {
    return label.replaceAll("\\.", "_");
  }
}
