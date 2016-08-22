package ltsa.exploration.strategy;

import MTSSynthesis.ar.dc.uba.model.condition.Fluent;
import MTSSynthesis.ar.dc.uba.model.condition.FluentImpl;
import MTSSynthesis.ar.dc.uba.model.condition.FluentPropositionalVariable;
import MTSSynthesis.ar.dc.uba.model.language.SingleSymbol;
import MTSSynthesis.ar.dc.uba.model.language.Symbol;
import MTSSynthesis.controller.model.gr.GRControllerGoal;
import ltsa.exploration.Synthesis;
import ltsa.exploration.knowledge.Knowledge;
import ltsa.lts.automata.lts.state.LabelledTransitionSystem;
import ltsa.lts.automata.lts.state.LTSTransitionList;

import java.util.*;

public class StrategyNewAction extends Strategy
{
    private Knowledge knowledge;
    private GRControllerGoal<String> goal;

    //region Constructor
    public StrategyNewAction(Knowledge knowledge, GRControllerGoal<String> goal)
    {
        this.knowledge = knowledge;
        this.goal = goal;
    }
    //endregion

    //region Overrides
    @Override
    public String chooseNextAction(HashSet<String> availableActions)
    {
        // The new action is in the current state
        String[] alphabet = this.knowledge.getCmponents()[0].getAlphabet();
        for (int anEvent : this.knowledge.getCmponents()[0].getStates()[this.knowledge.getCurrentStates()[0]].getEvents())
            for (String anAvailableAction : availableActions)
                if (alphabet[anEvent].equals(anAvailableAction + '?'))
                    return anAvailableAction;

        // The new action is in another state
        return getControllerNextAction(availableActions);
    }
    //endregion

    //region Private methods
    private Boolean fullyExplored(LTSTransitionList aState, Boolean onlyControllable)
    {
        for (int anEvent : aState.getEvents())
            if (isPossibleAction(anEvent, onlyControllable))
                return false;

        return true;
    }
    private Boolean isPossibleAction(int i, Boolean onlyControllable)
    {
        String action = this.knowledge.getCmponents()[0].getAlphabet()[i];

        if (onlyControllable)
            for (int componentNumber = 1; componentNumber < this.knowledge.getCmponents().length; componentNumber++)
                for (String anAction : this.knowledge.getCmponents()[componentNumber].getAlphabet())
                    if (Objects.equals(action, anAction))
                        return false;

        return !action.contains("tau") && action.contains("?");
    }
    private String getControllerNextAction(HashSet<String> availableActions)
    {
        LabelledTransitionSystem[] machines = this.knowledge.cloneForSynthesisFromCurrentState();
        int stateZero = this.knowledge.getCurrentStates()[0];

        // To controllable action
        if (!this.fullyExplored(machines[0].getStates()[stateZero], true))
        {
            String controllerAction = getActionFromController(machines[0], stateZero, availableActions);
            if (!controllerAction.equals("WAIT"))
                return controllerAction;
        }
        for (int i = 1; i < machines[0].getStates().length; i++)
        {
            if (!this.fullyExplored(machines[0].getStates()[i], true))
            {
                String controllerAction = getActionFromController(machines[0], i, availableActions);
                 if (!controllerAction.equals("WAIT"))
                    return controllerAction;
            }
        }

        // To shared action
        if (!this.fullyExplored(machines[0].getStates()[stateZero], false))
        {
            String controllerAction = getActionFromController(machines[0], stateZero, availableActions);
            if (!controllerAction.equals("WAIT"))
                return controllerAction;
        }
        for (int i = 1; i < machines[0].getStates().length; i++)
        {
            if (!this.fullyExplored(machines[0].getStates()[i], false))
            {
                String controllerAction = getActionFromController(machines[0], i, availableActions);
                if (!controllerAction.equals("WAIT"))
                    return controllerAction;
            }
        }

        return "WAIT";
    }
    private LabelledTransitionSystem getControllerToState(LabelledTransitionSystem machine, int stateNumber)
    {
        // Controlable actions
        ArrayList<String> controllableActions = new ArrayList<>();
        for (String anAction : goal.getControllableActions())
            controllableActions.add(anAction);

        Set<String> goalControllableActions = new HashSet<>();
        goalControllableActions.add("fluent_on");
        for (String anAction : controllableActions)
            goalControllableActions.add(anAction);

        // Fluent
        Set<Symbol> on = new HashSet<>();
        on.add(new SingleSymbol("fluent_on"));

        Set<Symbol> off = new HashSet<>();
        for (String anAction : goalControllableActions)
            if (!anAction.equals("fluent_on"))
                off.add(new SingleSymbol(anAction));

        Fluent fluent = new FluentImpl("F_end", on, off, false);
        Set<Fluent> fluents = new HashSet<>();
        fluents.add(fluent);

        // Formula
        FluentPropositionalVariable formula = new FluentPropositionalVariable(fluent);

        // Goal
        GRControllerGoal<String> goal = new GRControllerGoal<>();
        goal.addAllFluents(fluents);
        goal.addGuarantee(formula);
        goal.addAllControllableActions(goalControllableActions);

        // Machine
        LabelledTransitionSystem compositionMachine = machine.myclone();
        compositionMachine.setAlphabet(Arrays.copyOf(compositionMachine.getAlphabet(), compositionMachine.getAlphabet().length+ 2));
        compositionMachine.getAlphabet()[compositionMachine.getAlphabet().length- 2] = "fluent_on?";
        compositionMachine.getAlphabet()[compositionMachine.getAlphabet().length - 1] ="fluent_on";
        compositionMachine.getStates()[stateNumber] = LTSTransitionList.addTransition(compositionMachine.getStates()[stateNumber], compositionMachine.getAlphabet().length - 1, stateNumber);

        // Transform MTS to LTS
        for (int j = 0; j < compositionMachine.getStates().length; j++)
            for (int k = 0; k < machine.getAlphabet().length; k++)
                if (machine.getAlphabet()[k].contains("tau") || machine.getAlphabet()[k].contains("?"))
                    compositionMachine.getStates()[j] = LTSTransitionList.removeEvent(compositionMachine.getStates()[j], k);

        // Sinthesis
        LabelledTransitionSystem[] compositionMachineArray = new LabelledTransitionSystem[1];
        compositionMachineArray[0] = compositionMachine;
        Synthesis synthesis = new Synthesis(compositionMachineArray, goal);
        return synthesis.getComposition().getComposition();
    }
    private String getActionFromController(LabelledTransitionSystem machine, int stateNumber, HashSet<String> availableActions)
    {
        LabelledTransitionSystem controllerToNextUnexploredState = getControllerToState(machine, stateNumber);
        if (controllerToNextUnexploredState.getMtsControlProblemAnswer().equals("ALL"))
            for (int anEvent : controllerToNextUnexploredState.getStates()[0].getEvents())
                if (availableActions.contains(controllerToNextUnexploredState.getAlphabet()[anEvent]))
                    return controllerToNextUnexploredState.getAlphabet()[anEvent];
        return "WAIT";
    }
    //endregion
}