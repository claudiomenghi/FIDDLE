package exploration.strategy;

import controller.model.gr.GRControllerGoal;
import exploration.Synthesis;
import exploration.knowledge.Knowledge;

import java.util.HashSet;

public class StrategySynthesis extends Strategy
{
    private Knowledge knowledge;
    private GRControllerGoal<String> goal;


    //region Constructor
    public StrategySynthesis(Knowledge knowledge, GRControllerGoal<String> goal)
    {
        this.knowledge = knowledge;
        this.goal = goal;
    }
    //endregion

    //region Overrides
    @Override
    public String chooseNextAction(HashSet<String> availableActions)
    {
        HashSet<String> controllerAvailableActions = getControllerAvailableActions();
        for (String anAction : availableActions)
            for (String anotherAction : controllerAvailableActions)
                if (anAction.equals(anotherAction))
                    return anAction;
        return "";
    }
    //endregion

    //region Public methods
    public HashSet<String> getControllerAvailableActions()
    {
        Synthesis synthesis = new Synthesis(this.knowledge.cloneForSynthesisFromCurrentState(), this.goal.copy());
        return synthesis.getControllerAvailableActions();
    }
    //endregion
}